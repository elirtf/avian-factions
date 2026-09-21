package club.avian.factions.core.player;

import club.avian.factions.api.database.Database;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class JdbcPlayerRepository implements PlayerRepository {

    private static final String SELECT = "SELECT uuid, name, first_join_at, last_seen_at FROM players WHERE uuid = ?";
    // Dialect note (ADR-0002): INSERT … ON DUPLICATE KEY UPDATE is MySQL/MariaDB-specific
    // (PostgreSQL would use ON CONFLICT). Single-row upsert per join, as the prior-art survey advises.
    private static final String UPSERT = """
            INSERT INTO players (uuid, name, first_join_at, last_seen_at, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE name = VALUES(name), last_seen_at = VALUES(last_seen_at), updated_at = VALUES(updated_at)
            """;
    private static final String TOUCH = "UPDATE players SET last_seen_at = ?, updated_at = ? WHERE uuid = ?";

    private final Database db;

    public JdbcPlayerRepository(Database db) {
        this.db = db;
    }

    @Override
    public CompletableFuture<Optional<PlayerProfile>> find(UUID uuid) {
        return db.query(c -> select(c, uuid));
    }

    @Override
    public CompletableFuture<PlayerProfile> findOrCreate(UUID uuid, String name, Instant now) {
        return db.query(c -> {
            try (var ps = c.prepareStatement(UPSERT)) {
                var ts = Timestamp.from(now);
                ps.setString(1, uuid.toString());
                ps.setString(2, name);
                ps.setTimestamp(3, ts);
                ps.setTimestamp(4, ts);
                ps.setTimestamp(5, ts);
                ps.setTimestamp(6, ts);
                ps.executeUpdate();
            }
            return select(c, uuid).orElseThrow(() -> new SQLException("players row vanished after upsert: " + uuid));
        });
    }

    @Override
    public CompletableFuture<Void> touchLastSeen(UUID uuid, Instant at) {
        return db.execute(c -> {
            try (var ps = c.prepareStatement(TOUCH)) {
                ps.setTimestamp(1, Timestamp.from(at));
                ps.setTimestamp(2, Timestamp.from(at));
                ps.setString(3, uuid.toString());
                ps.executeUpdate();
            }
        });
    }

    private static Optional<PlayerProfile> select(Connection c, UUID uuid) throws SQLException {
        try (var ps = c.prepareStatement(SELECT)) {
            ps.setString(1, uuid.toString());
            try (var rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(read(rs)) : Optional.empty();
            }
        }
    }

    private static PlayerProfile read(ResultSet rs) throws SQLException {
        return new PlayerProfile(
                UUID.fromString(rs.getString("uuid")),
                rs.getString("name"),
                rs.getTimestamp("first_join_at").toInstant(),
                rs.getTimestamp("last_seen_at").toInstant());
    }
}
