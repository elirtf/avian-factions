package club.avian.factions.economy;

import club.avian.factions.api.economy.Currency;
import club.avian.factions.api.economy.TransactionResult;
import club.avian.factions.testing.MariaDbExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The atomicity claims, against a real MariaDB. These are the tests that matter most in the whole
 * suite: an economy that can be raced is an economy that can be duplicated.
 */
@ExtendWith(MariaDbExtension.class)
class JdbcEconomyRepositoryIT {

    static final Instant T0 = Instant.parse("2026-09-22T18:00:00.500Z");

    final JdbcEconomyRepository repo =
            new JdbcEconomyRepository(MariaDbExtension.database(), Clock.fixed(T0, ZoneOffset.UTC));

    private TransactionResult deposit(UUID player, long amount) {
        return repo.apply(player, Currency.MONEY, amount, "DEPOSIT", "test", null, 1).join();
    }

    private TransactionResult withdraw(UUID player, long amount) {
        return repo.apply(player, Currency.MONEY, -amount, "WITHDRAW", "test", null, 1).join();
    }

    private long balance(UUID player, Currency currency) {
        return MariaDbExtension.database().query(c -> {
            try (var ps = c.prepareStatement(
                    "SELECT amount FROM balances WHERE player = ? AND season_id = 1 AND currency = ?")) {
                ps.setString(1, player.toString());
                ps.setString(2, currency.name());
                try (var rs = ps.executeQuery()) {
                    return rs.next() ? rs.getLong(1) : 0L;
                }
            }
        }).join();
    }

    private int auditRows(UUID player) {
        return MariaDbExtension.database().query(c -> {
            try (var ps = c.prepareStatement("SELECT COUNT(*) FROM economy_transactions WHERE player = ?")) {
                ps.setString(1, player.toString());
                try (var rs = ps.executeQuery()) {
                    rs.next();
                    return rs.getInt(1);
                }
            }
        }).join();
    }

    @Test
    void depositCreatesTheRowAndAnAuditTrail() {
        var player = UUID.randomUUID();
        var result = deposit(player, 10_000);
        assertTrue(result.ok());
        assertNotNull(result.transaction(), "an OK transaction carries its audit id");
        assertEquals(10_000, result.balanceAfter());
        assertEquals(10_000, balance(player, Currency.MONEY));
        assertEquals(1, auditRows(player));
    }

    @Test
    void theAuditRowRecordsEverySpecifiedField() {
        var player = UUID.randomUUID();
        deposit(player, 5_000);
        withdraw(player, 2_000);

        var row = MariaDbExtension.database().query(c -> {
            try (var ps = c.prepareStatement("""
                    SELECT action, amount, balance_before, balance_after, reason, created_at
                    FROM economy_transactions WHERE player = ? AND action = 'WITHDRAW'
                    """)) {
                ps.setString(1, player.toString());
                try (var rs = ps.executeQuery()) {
                    rs.next();
                    return List.of(rs.getString("action"), rs.getLong("amount"),
                            rs.getLong("balance_before"), rs.getLong("balance_after"),
                            rs.getString("reason"), rs.getTimestamp("created_at").toInstant());
                }
            }
        }).join();
        assertEquals(List.of("WITHDRAW", -2_000L, 5_000L, 3_000L, "test", T0), row);
    }

    @Test
    void overdraftIsRefusedAndLeavesNoAuditRow() {
        var player = UUID.randomUUID();
        deposit(player, 1_000);
        var result = withdraw(player, 5_000);
        assertEquals(TransactionResult.Status.INSUFFICIENT_FUNDS, result.status());
        assertEquals(1_000, balance(player, Currency.MONEY), "the balance is untouched");
        assertEquals(1, auditRows(player), "only the deposit was audited; the refusal rolled back");
    }

    /**
     * The test this whole design exists for. Twenty threads each try to withdraw the full balance
     * at the same moment; exactly one may succeed, or the economy can be duplicated.
     */
    @Test
    void concurrentWithdrawalsCannotBothSucceed() throws Exception {
        var player = UUID.randomUUID();
        deposit(player, 1_000);

        int attempts = 20;
        var start = new CountDownLatch(1);
        var futures = new ArrayList<CompletableFuture<TransactionResult>>();
        var threads = new ArrayList<Thread>();
        for (int i = 0; i < attempts; i++) {
            var future = new CompletableFuture<TransactionResult>();
            futures.add(future);
            var thread = new Thread(() -> {
                try {
                    start.await();
                    future.complete(withdraw(player, 1_000));
                } catch (Exception e) {
                    future.completeExceptionally(e);
                }
            }, "withdraw-" + i);
            threads.add(thread);
            thread.start();
        }
        start.countDown();
        for (var thread : threads) {
            thread.join(TimeUnit.SECONDS.toMillis(30));
        }

        long succeeded = futures.stream().map(CompletableFuture::join).filter(TransactionResult::ok).count();
        assertEquals(1, succeeded, "exactly one withdrawal of the full balance may succeed");
        assertEquals(0, balance(player, Currency.MONEY));
        assertEquals(2, auditRows(player), "deposit + the one successful withdrawal");
    }

    @Test
    void concurrentDepositsAllLandAndSumCorrectly() throws Exception {
        var player = UUID.randomUUID();
        int attempts = 20;
        var start = new CountDownLatch(1);
        var threads = IntStream.range(0, attempts).mapToObj(i -> {
            var thread = new Thread(() -> {
                try {
                    start.await();
                    deposit(player, 100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
            thread.start();
            return thread;
        }).toList();
        start.countDown();
        for (var thread : threads) {
            thread.join(TimeUnit.SECONDS.toMillis(30));
        }
        assertEquals(attempts * 100, balance(player, Currency.MONEY), "no deposit may be lost to a race");
        assertEquals(attempts, auditRows(player));
    }

    @Test
    void transferMovesMoneyAndAuditsBothSidesWithOneReference() {
        var from = UUID.randomUUID();
        var to = UUID.randomUUID();
        deposit(from, 10_000);

        var result = repo.transfer(from, to, Currency.MONEY, 4_000, "pay", 1).join();
        assertTrue(result.ok());
        assertEquals(6_000, balance(from, Currency.MONEY));
        assertEquals(4_000, balance(to, Currency.MONEY));

        var references = MariaDbExtension.database().query(c -> {
            try (var ps = c.prepareStatement(
                    "SELECT DISTINCT reference_id FROM economy_transactions WHERE player IN (?, ?) AND reference_id IS NOT NULL")) {
                ps.setString(1, from.toString());
                ps.setString(2, to.toString());
                try (var rs = ps.executeQuery()) {
                    var ids = new ArrayList<String>();
                    while (rs.next()) {
                        ids.add(rs.getString(1));
                    }
                    return ids;
                }
            }
        }).join();
        assertEquals(1, references.size(), "both sides of a transfer share one reference id");
    }

    @Test
    void aRefusedTransferMovesNothing() {
        var from = UUID.randomUUID();
        var to = UUID.randomUUID();
        deposit(from, 100);
        var result = repo.transfer(from, to, Currency.MONEY, 5_000, "pay", 1).join();
        assertEquals(TransactionResult.Status.INSUFFICIENT_FUNDS, result.status());
        assertEquals(100, balance(from, Currency.MONEY));
        assertEquals(0, balance(to, Currency.MONEY));
    }

    @Test
    void currenciesAndSeasonsAreSeparateBalances() {
        var player = UUID.randomUUID();
        repo.apply(player, Currency.MONEY, 500, "DEPOSIT", "t", null, 1).join();
        repo.apply(player, Currency.TOKENS, 7, "DEPOSIT", "t", null, 1).join();
        repo.apply(player, Currency.MONEY, 900, "DEPOSIT", "t", null, 2).join();

        assertEquals(500, balance(player, Currency.MONEY));
        assertEquals(7, balance(player, Currency.TOKENS));
        assertEquals(500, repo.loadSeason(1).get(player).get(Currency.MONEY));
        assertEquals(900, repo.loadSeason(2).get(player).get(Currency.MONEY));
    }

    @Test
    void loadSeasonReturnsEveryCurrencyPerPlayer() {
        var player = UUID.randomUUID();
        repo.apply(player, Currency.MONEY, 250, "DEPOSIT", "t", null, 3).join();
        repo.apply(player, Currency.GEMS, 2, "DEPOSIT", "t", null, 3).join();
        var loaded = repo.loadSeason(3).get(player);
        assertEquals(250, loaded.get(Currency.MONEY));
        assertEquals(2, loaded.get(Currency.GEMS));
    }
}
