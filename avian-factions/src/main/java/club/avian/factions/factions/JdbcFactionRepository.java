package club.avian.factions.factions;

import club.avian.factions.api.database.Database;
import club.avian.factions.api.faction.FactionRank;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class JdbcFactionRepository implements FactionRepository {

    private static final String INSERT_FACTION = """
            INSERT INTO factions (id, season_id, name, name_key, leader, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """;
    private static final String INSERT_MEMBER = """
            INSERT INTO faction_members (faction_id, player, rank, joined_at) VALUES (?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE rank = VALUES(rank)
            """;
    private static final String DELETE_MEMBER = "DELETE FROM faction_members WHERE faction_id = ? AND player = ?";
    private static final String DELETE_FACTION = "DELETE FROM factions WHERE id = ?";
    private static final String SELECT_FACTIONS =
            "SELECT id, name, leader, created_at FROM factions WHERE season_id = ?";
    private static final String SELECT_MEMBERS = """
            SELECT m.faction_id, m.player, m.rank FROM faction_members m
            JOIN factions f ON f.id = m.faction_id WHERE f.season_id = ?
            """;

    private final Database db;

    public JdbcFactionRepository(Database db) {
        this.db = db;
    }

    @Override
    public List<FactionRecord> loadSeason(int seasonId) {
        return db.queryDuringBoot(c -> {
            Map<UUID, Map<UUID, FactionRank>> members = new HashMap<>();
            try (var ps = c.prepareStatement(SELECT_MEMBERS)) {
                ps.setInt(1, seasonId);
                try (var rs = ps.executeQuery()) {
                    while (rs.next()) {
                        members.computeIfAbsent(UUID.fromString(rs.getString("faction_id")), k -> new HashMap<>())
                                .put(UUID.fromString(rs.getString("player")), FactionRank.valueOf(rs.getString("rank")));
                    }
                }
            }
            List<FactionRecord> factions = new ArrayList<>();
            try (var ps = c.prepareStatement(SELECT_FACTIONS)) {
                ps.setInt(1, seasonId);
                try (var rs = ps.executeQuery()) {
                    while (rs.next()) {
                        var id = UUID.fromString(rs.getString("id"));
                        factions.add(new FactionRecord(id, rs.getString("name"),
                                UUID.fromString(rs.getString("leader")),
                                members.getOrDefault(id, Map.of()), seasonId,
                                rs.getTimestamp("created_at").toInstant()));
                    }
                }
            }
            return factions;
        });
    }

    @Override
    public CompletableFuture<Void> create(FactionRecord faction) {
        return db.execute(c -> inTransaction(c, () -> {
            try (var ps = c.prepareStatement(INSERT_FACTION)) {
                var now = Timestamp.from(faction.createdAt());
                ps.setString(1, faction.id().toString());
                ps.setInt(2, faction.seasonId());
                ps.setString(3, faction.name());
                ps.setString(4, FactionName.key(faction.name()));
                ps.setString(5, faction.leader().toString());
                ps.setTimestamp(6, now);
                ps.setTimestamp(7, now);
                ps.executeUpdate();
            }
            try (var ps = c.prepareStatement(INSERT_MEMBER)) {
                for (var member : faction.members().entrySet()) {
                    ps.setString(1, faction.id().toString());
                    ps.setString(2, member.getKey().toString());
                    ps.setString(3, member.getValue().name());
                    ps.setTimestamp(4, Timestamp.from(faction.createdAt()));
                    ps.addBatch();
                }
                ps.executeBatch();
            }
        }));
    }

    @Override
    public CompletableFuture<Void> addMember(UUID factionId, UUID player, FactionRank rank, Instant at) {
        return db.execute(c -> {
            try (var ps = c.prepareStatement(INSERT_MEMBER)) {
                ps.setString(1, factionId.toString());
                ps.setString(2, player.toString());
                ps.setString(3, rank.name());
                ps.setTimestamp(4, Timestamp.from(at));
                ps.executeUpdate();
            }
        });
    }

    @Override
    public CompletableFuture<Void> removeMember(UUID factionId, UUID player) {
        return db.execute(c -> {
            try (var ps = c.prepareStatement(DELETE_MEMBER)) {
                ps.setString(1, factionId.toString());
                ps.setString(2, player.toString());
                ps.executeUpdate();
            }
        });
    }

    @Override
    public CompletableFuture<Void> delete(UUID factionId) {
        return db.execute(c -> {
            try (var ps = c.prepareStatement(DELETE_FACTION)) {
                ps.setString(1, factionId.toString());
                ps.executeUpdate();
            }
        });
    }

    @FunctionalInterface
    private interface SqlBody {
        void run() throws SQLException;
    }

    /** Faction + leader row must land together or not at all (#4: money/state in one transaction). */
    private static void inTransaction(Connection c, SqlBody body) throws SQLException {
        var previous = c.getAutoCommit();
        c.setAutoCommit(false);
        try {
            body.run();
            c.commit();
        } catch (SQLException | RuntimeException e) {
            c.rollback();
            throw e;
        } finally {
            c.setAutoCommit(previous);
        }
    }
}
