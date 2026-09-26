package club.avian.factions.combat;

import club.avian.factions.api.database.Database;

import java.sql.Timestamp;
import java.time.Clock;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/** Players who owe a death because their logout body was killed ({@code combat_logout_deaths}). */
interface LogoutDeathRepository {

    /** Everyone still owing a death. Boot only. */
    Set<UUID> loadPending();

    CompletableFuture<Void> record(UUID player, UUID killer);

    CompletableFuture<Void> clear(UUID player);

    final class Jdbc implements LogoutDeathRepository {

        private final Database db;
        private final Clock clock;

        Jdbc(Database db, Clock clock) {
            this.db = db;
            this.clock = clock;
        }

        @Override
        public Set<UUID> loadPending() {
            return db.queryDuringBoot(c -> {
                Set<UUID> pending = new HashSet<>();
                try (var ps = c.prepareStatement("SELECT player FROM combat_logout_deaths");
                     var rs = ps.executeQuery()) {
                    while (rs.next()) {
                        pending.add(UUID.fromString(rs.getString(1)));
                    }
                }
                return pending;
            });
        }

        @Override
        public CompletableFuture<Void> record(UUID player, UUID killer) {
            return db.executeInOrder(player, c -> {
                try (var ps = c.prepareStatement("""
                        INSERT INTO combat_logout_deaths (player, killer, created_at) VALUES (?, ?, ?)
                        ON DUPLICATE KEY UPDATE killer = VALUES(killer), created_at = VALUES(created_at)
                        """)) {
                    ps.setString(1, player.toString());
                    ps.setString(2, killer == null ? null : killer.toString());
                    ps.setTimestamp(3, Timestamp.from(clock.instant()));
                    ps.executeUpdate();
                }
            });
        }

        @Override
        public CompletableFuture<Void> clear(UUID player) {
            return db.executeInOrder(player, c -> {
                try (var ps = c.prepareStatement("DELETE FROM combat_logout_deaths WHERE player = ?")) {
                    ps.setString(1, player.toString());
                    ps.executeUpdate();
                }
            });
        }
    }
}
