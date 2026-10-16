package club.avian.factions.factions;

import club.avian.factions.api.database.Database;

import java.sql.Timestamp;
import java.util.concurrent.CompletableFuture;

/** Who holds the Aerie ({@code factions_aerie}): loaded at boot, written behind on every change. */
interface AerieRepository {

    /** The saved hold, or null. Boot only. */
    Aerie.Hold load();

    /** Saves {@code hold}, or clears it when null. */
    CompletableFuture<Void> save(Aerie.Hold hold);

    final class Jdbc implements AerieRepository {

        private static final String ID = "aerie";
        private final Database db;

        Jdbc(Database db) {
            this.db = db;
        }

        @Override
        public Aerie.Hold load() {
            return db.queryDuringBoot(c -> {
                try (var ps = c.prepareStatement("SELECT holder, held_since, milestone_done FROM factions_aerie WHERE id = ?")) {
                    ps.setString(1, ID);
                    try (var rs = ps.executeQuery()) {
                        return rs.next()
                                ? new Aerie.Hold(rs.getInt(1), rs.getTimestamp(2).getTime(), rs.getBoolean(3))
                                : null;
                    }
                }
            });
        }

        @Override
        public CompletableFuture<Void> save(Aerie.Hold hold) {
            return db.executeInOrder(ID, c -> {
                if (hold == null) {
                    try (var ps = c.prepareStatement("DELETE FROM factions_aerie WHERE id = ?")) {
                        ps.setString(1, ID);
                        ps.executeUpdate();
                    }
                    return;
                }
                try (var ps = c.prepareStatement("""
                        INSERT INTO factions_aerie (id, holder, held_since, milestone_done) VALUES (?, ?, ?, ?)
                        ON DUPLICATE KEY UPDATE holder = VALUES(holder), held_since = VALUES(held_since),
                            milestone_done = VALUES(milestone_done)
                        """)) {
                    ps.setString(1, ID);
                    ps.setInt(2, hold.faction());
                    ps.setTimestamp(3, new Timestamp(hold.since()));
                    ps.setBoolean(4, hold.milestoneDone());
                    ps.executeUpdate();
                }
            });
        }
    }
}
