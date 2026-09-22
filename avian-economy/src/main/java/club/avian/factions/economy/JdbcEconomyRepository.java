package club.avian.factions.economy;

import club.avian.factions.api.database.Database;
import club.avian.factions.api.economy.Currency;
import club.avian.factions.api.economy.TransactionResult;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Clock;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * The atomic part of the economy (spec §13 "transactions must be atomic", §74).
 *
 * <p>A withdrawal is a single conditional {@code UPDATE … WHERE amount >= ?}: the balance check
 * and the deduction are the same statement, so two withdrawals racing cannot both pass the check.
 * The audit row is written in the same database transaction as the balance change — either both
 * land or neither does.
 */
public final class JdbcEconomyRepository implements EconomyRepository {

    private static final String SELECT_ALL = "SELECT player, currency, amount FROM balances WHERE season_id = ?";
    private static final String ENSURE_ROW = """
            INSERT INTO balances (player, season_id, currency, amount, updated_at) VALUES (?, ?, ?, 0, ?)
            ON DUPLICATE KEY UPDATE player = player
            """;
    private static final String SELECT_FOR_UPDATE =
            "SELECT amount FROM balances WHERE player = ? AND season_id = ? AND currency = ? FOR UPDATE";
    /** The guard is in the statement, not in Java: this is what makes concurrent withdrawal safe. */
    private static final String APPLY_DELTA = """
            UPDATE balances SET amount = amount + ?, updated_at = ?
            WHERE player = ? AND season_id = ? AND currency = ? AND amount + ? >= 0
            """;
    private static final String INSERT_AUDIT = """
            INSERT INTO economy_transactions
                (id, season_id, player, currency, action, amount, balance_before, balance_after,
                 reason, reference_id, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

    private final Database db;
    private final Clock clock;

    public JdbcEconomyRepository(Database db, Clock clock) {
        this.db = db;
        this.clock = clock;
    }

    @Override
    public Map<UUID, Map<Currency, Long>> loadSeason(int seasonId) {
        return db.queryDuringBoot(c -> {
            Map<UUID, Map<Currency, Long>> balances = new HashMap<>();
            try (var ps = c.prepareStatement(SELECT_ALL)) {
                ps.setInt(1, seasonId);
                try (var rs = ps.executeQuery()) {
                    while (rs.next()) {
                        balances.computeIfAbsent(UUID.fromString(rs.getString("player")),
                                        k -> new EnumMap<>(Currency.class))
                                .put(Currency.valueOf(rs.getString("currency")), rs.getLong("amount"));
                    }
                }
            }
            return balances;
        });
    }

    @Override
    public CompletableFuture<TransactionResult> apply(UUID player, Currency currency, long delta,
                                                      String action, String reason, UUID referenceId,
                                                      int seasonId) {
        return db.query(c -> inTransaction(c, () -> {
            ensureRow(c, player, currency, seasonId);
            long before = lockedBalance(c, player, currency, seasonId);
            int changed = applyDelta(c, player, currency, seasonId, delta);
            if (changed == 0) {
                return TransactionResult.failure(TransactionResult.Status.INSUFFICIENT_FUNDS, before);
            }
            long after = before + delta;
            var id = writeAudit(c, player, currency, action, delta, before, after, reason, referenceId, seasonId);
            return new TransactionResult(TransactionResult.Status.OK, id, after);
        }));
    }

    @Override
    public CompletableFuture<TransactionResult> transfer(UUID from, UUID to, Currency currency,
                                                         long amount, String reason, int seasonId) {
        return db.query(c -> inTransaction(c, () -> {
            ensureRow(c, from, currency, seasonId);
            ensureRow(c, to, currency, seasonId);
            // Lock in a stable order so two opposite transfers cannot deadlock each other.
            var first = from.compareTo(to) <= 0 ? from : to;
            var second = first.equals(from) ? to : from;
            lockedBalance(c, first, currency, seasonId);
            lockedBalance(c, second, currency, seasonId);

            long fromBefore = lockedBalance(c, from, currency, seasonId);
            if (applyDelta(c, from, currency, seasonId, -amount) == 0) {
                return TransactionResult.failure(TransactionResult.Status.INSUFFICIENT_FUNDS, fromBefore);
            }
            long toBefore = lockedBalance(c, to, currency, seasonId);
            applyDelta(c, to, currency, seasonId, amount);

            var reference = UUID.randomUUID();
            writeAudit(c, from, currency, "TRANSFER_OUT", -amount, fromBefore, fromBefore - amount,
                    reason, reference, seasonId);
            writeAudit(c, to, currency, "TRANSFER_IN", amount, toBefore, toBefore + amount,
                    reason, reference, seasonId);
            return new TransactionResult(TransactionResult.Status.OK, reference, fromBefore - amount);
        }));
    }

    // --- statements ---------------------------------------------------------------------------

    private void ensureRow(Connection c, UUID player, Currency currency, int seasonId) throws SQLException {
        try (var ps = c.prepareStatement(ENSURE_ROW)) {
            ps.setString(1, player.toString());
            ps.setInt(2, seasonId);
            ps.setString(3, currency.name());
            ps.setTimestamp(4, Timestamp.from(clock.instant()));
            ps.executeUpdate();
        }
    }

    private long lockedBalance(Connection c, UUID player, Currency currency, int seasonId) throws SQLException {
        try (var ps = c.prepareStatement(SELECT_FOR_UPDATE)) {
            ps.setString(1, player.toString());
            ps.setInt(2, seasonId);
            ps.setString(3, currency.name());
            try (var rs = ps.executeQuery()) {
                return rs.next() ? rs.getLong(1) : 0L;
            }
        }
    }

    private int applyDelta(Connection c, UUID player, Currency currency, int seasonId, long delta)
            throws SQLException {
        try (var ps = c.prepareStatement(APPLY_DELTA)) {
            ps.setLong(1, delta);
            ps.setTimestamp(2, Timestamp.from(clock.instant()));
            ps.setString(3, player.toString());
            ps.setInt(4, seasonId);
            ps.setString(5, currency.name());
            ps.setLong(6, delta);
            return ps.executeUpdate();
        }
    }

    private UUID writeAudit(Connection c, UUID player, Currency currency, String action, long amount,
                            long before, long after, String reason, UUID referenceId, int seasonId)
            throws SQLException {
        var id = UUID.randomUUID();
        try (var ps = c.prepareStatement(INSERT_AUDIT)) {
            ps.setString(1, id.toString());
            ps.setInt(2, seasonId);
            ps.setString(3, player.toString());
            ps.setString(4, currency.name());
            ps.setString(5, action);
            ps.setLong(6, amount);
            ps.setLong(7, before);
            ps.setLong(8, after);
            ps.setString(9, reason);
            ps.setString(10, referenceId == null ? null : referenceId.toString());
            ps.setTimestamp(11, Timestamp.from(clock.instant()));
            ps.executeUpdate();
        }
        return id;
    }

    @FunctionalInterface
    private interface Body {
        TransactionResult run() throws SQLException;
    }

    private static TransactionResult inTransaction(Connection c, Body body) throws SQLException {
        var previous = c.getAutoCommit();
        c.setAutoCommit(false);
        try {
            var result = body.run();
            if (result.ok()) {
                c.commit();
            } else {
                c.rollback();   // a refused withdrawal must leave no audit row and no lock held
            }
            return result;
        } catch (SQLException | RuntimeException e) {
            c.rollback();
            throw e;
        } finally {
            c.setAutoCommit(previous);
        }
    }
}
