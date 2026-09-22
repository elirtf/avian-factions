package club.avian.factions.factions.power;

import club.avian.factions.core.database.HikariDatabase;
import club.avian.factions.factions.FactionsConfig;
import club.avian.factions.testing.MariaDbExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MariaDbExtension.class)
class JdbcPowerRepositoryIT {

    static final Instant T0 = Instant.parse("2026-09-22T12:00:00.250Z");

    final HikariDatabase db = MariaDbExtension.database();
    final PowerRepository repo = new JdbcPowerRepository(db);
    final PowerConfig config = new FactionsConfig().power();

    @Test
    void findOrCreateInsertsStartingPowerThenReturnsTheStoredRow() {
        var player = UUID.randomUUID();
        var created = repo.findOrCreate(player, 1, config, T0).join();
        assertEquals(config.starting(), created.value(), 1e-9);
        assertNull(created.onlineSince(), "a created row starts offline");

        repo.save(created.withValue(4.25, T0), 1).join();
        var reloaded = repo.findOrCreate(player, 1, config, T0.plusSeconds(60)).join();
        assertEquals(4.25, reloaded.value(), 1e-9, "the second call must not overwrite with starting power");
    }

    @Test
    void saveRoundTripsEveryColumnIncludingNullOnlineSince() {
        var player = UUID.randomUUID();
        var online = new PlayerPower(player, 7.5, 2.5, T0, T0);
        repo.save(online, 1).join();
        var loaded = load(player, 1);
        assertEquals(7.5, loaded.value(), 1e-9);
        assertEquals(2.5, loaded.boost(), 1e-9);
        assertEquals(T0, loaded.updatedAt());
        assertEquals(T0, loaded.onlineSince());

        repo.save(online.withOnlineSince(null), 1).join();
        assertNull(load(player, 1).onlineSince(), "quitting clears online_since");
    }

    @Test
    void saveIsAnUpsertNotADuplicate() {
        var player = UUID.randomUUID();
        repo.save(new PlayerPower(player, 1, 0, T0, null), 1).join();
        repo.save(new PlayerPower(player, 2, 0, T0, null), 1).join();
        assertEquals(1, rowCount(player), "a second save must update, not insert");
        assertEquals(2.0, load(player, 1).value(), 1e-9);
    }

    @Test
    void powerIsPerSeason() {
        var player = UUID.randomUUID();
        repo.save(new PlayerPower(player, 3, 0, T0, null), 1).join();
        repo.save(new PlayerPower(player, 9, 0, T0, null), 2).join();
        assertEquals(3.0, load(player, 1).value(), 1e-9);
        assertEquals(9.0, load(player, 2).value(), 1e-9);
        assertTrue(repo.loadSeason(2).stream().anyMatch(p -> p.player().equals(player) && p.value() == 9.0));
    }

    private PlayerPower load(UUID player, int season) {
        return repo.loadSeason(season).stream().filter(p -> p.player().equals(player)).findFirst().orElseThrow();
    }

    private int rowCount(UUID player) {
        return db.query(c -> {
            try (var ps = c.prepareStatement("SELECT COUNT(*) FROM player_power WHERE player = ?")) {
                ps.setString(1, player.toString());
                try (var rs = ps.executeQuery()) {
                    rs.next();
                    return rs.getInt(1);
                }
            }
        }).join();
    }
}
