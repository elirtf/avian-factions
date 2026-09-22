package club.avian.factions.economy;

import club.avian.factions.api.config.ConfigHandle;
import club.avian.factions.api.economy.Currency;
import club.avian.factions.api.economy.TransactionResult;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EconomyServiceTest {

    static final UUID ALICE = UUID.randomUUID();
    static final UUID BOB = UUID.randomUUID();

    /** Mirrors the real repository's contract, including refusing an overdraft. */
    static final class MemoryRepository implements EconomyRepository {
        final Map<UUID, Map<Currency, Long>> balances = new HashMap<>();
        final List<String> audit = new ArrayList<>();
        Map<UUID, Map<Currency, Long>> season = Map.of();

        @Override public Map<UUID, Map<Currency, Long>> loadSeason(int seasonId) {
            return season;
        }

        @Override public CompletableFuture<TransactionResult> apply(UUID player, Currency currency, long delta,
                                                                    String action, String reason, UUID ref, int season) {
            long before = get(player, currency);
            if (before + delta < 0) {
                return CompletableFuture.completedFuture(
                        TransactionResult.failure(TransactionResult.Status.INSUFFICIENT_FUNDS, before));
            }
            set(player, currency, before + delta);
            audit.add(action + " " + delta + " " + reason);
            return CompletableFuture.completedFuture(
                    new TransactionResult(TransactionResult.Status.OK, UUID.randomUUID(), before + delta));
        }

        @Override public CompletableFuture<TransactionResult> transfer(UUID from, UUID to, Currency currency,
                                                                       long amount, String reason, int season) {
            long fromBefore = get(from, currency);
            if (fromBefore < amount) {
                return CompletableFuture.completedFuture(
                        TransactionResult.failure(TransactionResult.Status.INSUFFICIENT_FUNDS, fromBefore));
            }
            set(from, currency, fromBefore - amount);
            set(to, currency, get(to, currency) + amount);
            audit.add("TRANSFER_OUT " + amount + " " + reason);
            audit.add("TRANSFER_IN " + amount + " " + reason);
            return CompletableFuture.completedFuture(
                    new TransactionResult(TransactionResult.Status.OK, UUID.randomUUID(), fromBefore - amount));
        }

        long get(UUID player, Currency currency) {
            return balances.getOrDefault(player, Map.of()).getOrDefault(currency, 0L);
        }

        private void set(UUID player, Currency currency, long value) {
            balances.computeIfAbsent(player, p -> new EnumMap<>(Currency.class)).put(currency, value);
        }
    }

    static final class Handle implements ConfigHandle<EconomyConfig> {
        final EconomyConfig config = new EconomyConfig();
        @Override public EconomyConfig get() { return config; }
        @Override public void onReload(Consumer<EconomyConfig> callback) { }
    }

    final MemoryRepository repo = new MemoryRepository();
    final Handle config = new Handle();
    final EconomyService economy = new EconomyService(repo, config, 1);

    @Test
    void anUnknownPlayerHasNothingAndNoAccount() {
        assertEquals(0, economy.balance(ALICE, Currency.MONEY));
        assertEquals(0, economy.balance(ALICE, Currency.TOKENS));
        assertFalse(economy.hasAccount(ALICE));
    }

    @Test
    void depositUpdatesTheCacheAndWritesOneAuditRow() {
        var result = economy.deposit(ALICE, Currency.MONEY, 10_000, "test").join();
        assertTrue(result.ok());
        assertEquals(10_000, result.balanceAfter());
        assertEquals(10_000, economy.balance(ALICE, Currency.MONEY), "the cache reflects the write");
        assertEquals(List.of("DEPOSIT 10000 test"), repo.audit);
        assertTrue(economy.hasAccount(ALICE));
    }

    @Test
    void withdrawBeyondTheBalanceIsRefusedAndChangesNothing() {
        economy.deposit(ALICE, Currency.MONEY, 500, "test").join();
        repo.audit.clear();

        var result = economy.withdraw(ALICE, Currency.MONEY, 900, "too much").join();
        assertEquals(TransactionResult.Status.INSUFFICIENT_FUNDS, result.status());
        assertEquals(500, economy.balance(ALICE, Currency.MONEY), "the balance is untouched");
        assertEquals(List.of(), repo.audit, "a refused withdrawal writes no audit row");
    }

    @Test
    void currenciesAreIndependent() {
        economy.deposit(ALICE, Currency.MONEY, 100, "m").join();
        economy.deposit(ALICE, Currency.TOKENS, 7, "t").join();
        assertEquals(100, economy.balance(ALICE, Currency.MONEY));
        assertEquals(7, economy.balance(ALICE, Currency.TOKENS));
        assertEquals(0, economy.balance(ALICE, Currency.GEMS));

        economy.withdraw(ALICE, Currency.TOKENS, 7, "spend").join();
        assertEquals(100, economy.balance(ALICE, Currency.MONEY), "spending tokens must not touch money");
    }

    @Test
    void nonPositiveAmountsAreRefusedBeforeTheDatabaseIsTouched() {
        for (long bad : new long[] {0, -1, Long.MIN_VALUE}) {
            assertEquals(TransactionResult.Status.INVALID_AMOUNT,
                    economy.deposit(ALICE, Currency.MONEY, bad, "bad").join().status());
            assertEquals(TransactionResult.Status.INVALID_AMOUNT,
                    economy.withdraw(ALICE, Currency.MONEY, bad, "bad").join().status());
        }
        assertEquals(List.of(), repo.audit, "nothing reached the repository");
    }

    @Test
    void transferMovesMoneyAndAuditsBothSides() {
        economy.deposit(ALICE, Currency.MONEY, 1_000, "seed").join();
        repo.audit.clear();

        var result = economy.transfer(ALICE, BOB, Currency.MONEY, 400, "pay").join();
        assertTrue(result.ok());
        assertEquals(600, economy.balance(ALICE, Currency.MONEY));
        assertEquals(400, economy.balance(BOB, Currency.MONEY));
        assertEquals(List.of("TRANSFER_OUT 400 pay", "TRANSFER_IN 400 pay"), repo.audit);
    }

    @Test
    void aFailedTransferLeavesBothSidesUntouched() {
        economy.deposit(ALICE, Currency.MONEY, 100, "seed").join();
        var result = economy.transfer(ALICE, BOB, Currency.MONEY, 500, "pay").join();
        assertEquals(TransactionResult.Status.INSUFFICIENT_FUNDS, result.status());
        assertEquals(100, economy.balance(ALICE, Currency.MONEY));
        assertEquals(0, economy.balance(BOB, Currency.MONEY));
    }

    @Test
    void transferringToYourselfIsRefused() {
        economy.deposit(ALICE, Currency.MONEY, 100, "seed").join();
        assertEquals(TransactionResult.Status.INVALID_AMOUNT,
                economy.transfer(ALICE, ALICE, Currency.MONEY, 50, "self").join().status());
        assertEquals(100, economy.balance(ALICE, Currency.MONEY), "and cannot duplicate money");
    }

    @Test
    void loadFillsTheCacheFromStorage() {
        repo.season = Map.of(ALICE, Map.of(Currency.MONEY, 250L, Currency.TOKENS, 3L));
        assertEquals(1, economy.load());
        assertEquals(250, economy.balance(ALICE, Currency.MONEY));
        assertEquals(3, economy.balance(ALICE, Currency.TOKENS));
    }

    @Test
    void moneyLeadsWithItsSymbolAndOtherCurrenciesTrailWithTheirName() {
        assertEquals("$1,640", economy.format(Currency.MONEY, 1_640));
        assertEquals("$5", economy.format(Currency.MONEY, 5));
        assertEquals("1,250 tokens", economy.format(Currency.TOKENS, 1_250));
        assertEquals("3 gems", economy.format(Currency.GEMS, 3));
    }

    @Test
    void startingBalancesAreGrantedOnceAndOnlyWhenConfigured() throws Exception {
        setStarting(100, 5, 0);
        economy.grantStartingBalances(ALICE).join();
        assertEquals(100, economy.balance(ALICE, Currency.MONEY));
        assertEquals(5, economy.balance(ALICE, Currency.TOKENS));
        assertEquals(0, economy.balance(ALICE, Currency.GEMS), "gems were zero, so nothing was written");
        assertEquals(2, repo.audit.size());
    }

    private void setStarting(long money, long tokens, long gems) throws Exception {
        set("startingMoney", money);
        set("startingTokens", tokens);
        set("startingGems", gems);
    }

    private void set(String field, Object value) throws Exception {
        var f = EconomyConfig.class.getDeclaredField(field);
        f.setAccessible(true);
        f.set(config.get(), value);
    }
}
