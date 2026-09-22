package club.avian.factions.factions.power;

import club.avian.factions.api.database.Database;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class JdbcPowerRepository implements PowerRepository {

    private static final String SELECT_SEASON =
            "SELECT player, power, power_boost, last_power_update_at, online_since FROM player_power WHERE season_id = ?";
    private static final String SELECT_ONE =
            "SELECT player, power, power_boost, last_power_update_at, online_since FROM player_power WHERE player = ? AND season_id = ?";
    private static final String UPSERT = """
            INSERT INTO player_power (player, season_id, power, power_boost, last_power_update_at, online_since)
            VALUES (?, ?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE power = VALUES(power), power_boost = VALUES(power_boost),
                last_power_update_at = VALUES(last_power_update_at), online_since = VALUES(online_since)
            """;

    private final Database db;

    public JdbcPowerRepository(Database db) {
        this.db = db;
    }

    @Override
    public List<PlayerPower> loadSeason(int seasonId) {
        return db.queryDuringBoot(c -> {
            List<PlayerPower> rows = new ArrayList<>();
            try (var ps = c.prepareStatement(SELECT_SEASON)) {
                ps.setInt(1, seasonId);
                try (var rs = ps.executeQuery()) {
                    while (rs.next()) {
                        rows.add(read(rs));
                    }
                }
            }
            return rows;
        });
    }

    @Override
    public CompletableFuture<Void> save(PlayerPower power, int seasonId) {
        return db.execute(c -> {
            try (var ps = c.prepareStatement(UPSERT)) {
                ps.setString(1, power.player().toString());
                ps.setInt(2, seasonId);
                ps.setDouble(3, power.value());
                ps.setDouble(4, power.boost());
                ps.setTimestamp(5, Timestamp.from(power.updatedAt()));
                ps.setTimestamp(6, power.onlineSince() == null ? null : Timestamp.from(power.onlineSince()));
                ps.executeUpdate();
            }
        });
    }

    @Override
    public CompletableFuture<PlayerPower> findOrCreate(UUID player, int seasonId, PowerConfig config, Instant now) {
        return db.query(c -> {
            try (var ps = c.prepareStatement(SELECT_ONE)) {
                ps.setString(1, player.toString());
                ps.setInt(2, seasonId);
                try (var rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return read(rs);
                    }
                }
            }
            var created = PlayerPower.starting(player, config, now);
            try (var ps = c.prepareStatement(UPSERT)) {
                ps.setString(1, created.player().toString());
                ps.setInt(2, seasonId);
                ps.setDouble(3, created.value());
                ps.setDouble(4, created.boost());
                ps.setTimestamp(5, Timestamp.from(created.updatedAt()));
                ps.setTimestamp(6, null);
                ps.executeUpdate();
            }
            return created;
        });
    }

    private static PlayerPower read(ResultSet rs) throws SQLException {
        var onlineSince = rs.getTimestamp("online_since");
        return new PlayerPower(
                UUID.fromString(rs.getString("player")),
                rs.getDouble("power"),
                rs.getDouble("power_boost"),
                rs.getTimestamp("last_power_update_at").toInstant(),
                onlineSince == null ? null : onlineSince.toInstant());
    }
}
