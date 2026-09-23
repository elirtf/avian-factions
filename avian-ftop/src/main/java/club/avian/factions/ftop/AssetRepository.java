package club.avian.factions.ftop;

import club.avian.factions.api.database.Database;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * {@code ftop_assets} and {@code ftop_results}. Loaded once at enable; after that every write is
 * write-behind, ordered per position (#27), because a stack can be blown up and replaced within a
 * tick and the delete must not land after the insert that follows it.
 */
public final class AssetRepository {

    // Dialect note (ADR-0002): ON DUPLICATE KEY UPDATE is MySQL/MariaDB-specific.
    private static final String UPSERT = """
            INSERT INTO ftop_assets (world, x, y, z, kind, type, amount, placed_at, grace_until, grace_units)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE kind = VALUES(kind), type = VALUES(type), amount = VALUES(amount),
                placed_at = VALUES(placed_at), grace_until = VALUES(grace_until), grace_units = VALUES(grace_units)
            """;
    private static final String DELETE = "DELETE FROM ftop_assets WHERE world = ? AND x = ? AND y = ? AND z = ?";
    private static final String RESULTS_KEY = "ftop_results";

    private final Database db;

    public AssetRepository(Database db) {
        this.db = db;
    }

    /** Startup only: blocks until every asset is read. */
    public List<Asset> loadAllDuringBoot() {
        return db.queryDuringBoot(c -> {
            var out = new ArrayList<Asset>();
            try (var st = c.createStatement();
                 var rs = st.executeQuery("SELECT world, x, y, z, kind, type, amount, placed_at, grace_until, grace_units FROM ftop_assets")) {
                while (rs.next()) {
                    var key = new AssetKey(UUID.fromString(rs.getString(1)), rs.getInt(2), rs.getInt(3), rs.getInt(4));
                    out.add(new Asset(key, AssetKind.valueOf(rs.getString(5)), rs.getString(6), rs.getInt(7),
                            rs.getLong(8), rs.getLong(9), rs.getInt(10)));
                }
            }
            return out;
        });
    }

    public CompletableFuture<Void> save(Asset a) {
        return db.executeInOrder(a.key(), c -> {
            try (var ps = c.prepareStatement(UPSERT)) {
                ps.setString(1, a.key().world().toString());
                ps.setInt(2, a.key().x());
                ps.setInt(3, a.key().y());
                ps.setInt(4, a.key().z());
                ps.setString(5, a.kind().name());
                ps.setString(6, a.type());
                ps.setInt(7, a.count());
                ps.setLong(8, a.placedAt());
                ps.setLong(9, a.graceUntil());
                ps.setInt(10, a.graceUnits());
                ps.executeUpdate();
            }
        });
    }

    public CompletableFuture<Void> delete(AssetKey key) {
        return db.executeInOrder(key, c -> {
            try (var ps = c.prepareStatement(DELETE)) {
                ps.setString(1, key.world().toString());
                ps.setInt(2, key.x());
                ps.setInt(3, key.y());
                ps.setInt(4, key.z());
                ps.executeUpdate();
            }
        });
    }

    /** Replaces the stored ranking in one transaction, so readers never see half of one. */
    public CompletableFuture<Void> saveResults(Ranking ranking) {
        return db.executeInOrder(RESULTS_KEY, c -> {
            boolean autoCommit = c.getAutoCommit();
            c.setAutoCommit(false);
            try (var clear = c.createStatement();
                 var ins = c.prepareStatement("""
                         INSERT INTO ftop_results (rank_no, faction_id, faction_tag, total_value, spawner_value,
                             block_value, breakdown, calculated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)""")) {
                clear.executeUpdate("DELETE FROM ftop_results");
                var at = Timestamp.from(ranking.calculatedAt());
                for (var e : ranking.entries()) {
                    ins.setInt(1, e.rank());
                    ins.setInt(2, e.owner().id());
                    ins.setString(3, e.owner().tag());
                    ins.setLong(4, e.total());
                    ins.setLong(5, e.spawnerValue());
                    ins.setLong(6, e.blockValue());
                    ins.setString(7, e.breakdown());
                    ins.setTimestamp(8, at);
                    ins.addBatch();
                }
                ins.executeBatch();
                c.commit();
            } catch (Exception e) {
                c.rollback();
                throw e;
            } finally {
                c.setAutoCommit(autoCommit);
            }
        });
    }
}
