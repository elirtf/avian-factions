package club.avian.factions.factions.claim;

import club.avian.factions.api.database.Database;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class JdbcClaimRepository implements ClaimRepository {

    private static final String SELECT_SEASON =
            "SELECT world, chunk_x, chunk_z, faction_id, claimed_by, claimed_at FROM faction_claims WHERE season_id = ?";
    private static final String UPSERT = """
            INSERT INTO faction_claims (world, chunk_x, chunk_z, season_id, faction_id, claimed_by, claimed_at)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE faction_id = VALUES(faction_id), claimed_by = VALUES(claimed_by),
                claimed_at = VALUES(claimed_at)
            """;
    private static final String DELETE =
            "DELETE FROM faction_claims WHERE world = ? AND chunk_x = ? AND chunk_z = ? AND season_id = ?";
    private static final String DELETE_BY_FACTION =
            "DELETE FROM faction_claims WHERE faction_id = ? AND season_id = ?";

    private final Database db;

    public JdbcClaimRepository(Database db) {
        this.db = db;
    }

    @Override
    public List<Claim> loadSeason(int seasonId) {
        return db.queryDuringBoot(c -> {
            List<Claim> claims = new ArrayList<>();
            try (var ps = c.prepareStatement(SELECT_SEASON)) {
                ps.setInt(1, seasonId);
                try (var rs = ps.executeQuery()) {
                    while (rs.next()) {
                        claims.add(new Claim(rs.getString("world"), rs.getInt("chunk_x"), rs.getInt("chunk_z"),
                                UUID.fromString(rs.getString("faction_id")),
                                UUID.fromString(rs.getString("claimed_by")),
                                rs.getTimestamp("claimed_at").toInstant()));
                    }
                }
            }
            return claims;
        });
    }

    @Override
    public CompletableFuture<Void> save(Claim claim, int seasonId) {
        return db.execute(c -> {
            try (var ps = c.prepareStatement(UPSERT)) {
                ps.setString(1, claim.world());
                ps.setInt(2, claim.chunkX());
                ps.setInt(3, claim.chunkZ());
                ps.setInt(4, seasonId);
                ps.setString(5, claim.factionId().toString());
                ps.setString(6, claim.claimedBy().toString());
                ps.setTimestamp(7, Timestamp.from(claim.claimedAt()));
                ps.executeUpdate();
            }
        });
    }

    @Override
    public CompletableFuture<Void> delete(String world, int chunkX, int chunkZ, int seasonId) {
        return db.execute(c -> {
            try (var ps = c.prepareStatement(DELETE)) {
                ps.setString(1, world);
                ps.setInt(2, chunkX);
                ps.setInt(3, chunkZ);
                ps.setInt(4, seasonId);
                ps.executeUpdate();
            }
        });
    }

    @Override
    public CompletableFuture<Void> deleteByFaction(UUID factionId, int seasonId) {
        return db.execute(c -> {
            try (var ps = c.prepareStatement(DELETE_BY_FACTION)) {
                ps.setString(1, factionId.toString());
                ps.setInt(2, seasonId);
                ps.executeUpdate();
            }
        });
    }
}
