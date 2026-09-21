package club.avian.factions.core.player;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerServiceTest {

    static final Instant T0 = Instant.parse("2026-09-21T12:00:00Z");

    /** In-memory repository with the same contract as the JDBC one. */
    static final class MemoryRepository implements PlayerRepository {
        final Map<UUID, PlayerProfile> rows = new HashMap<>();

        @Override
        public CompletableFuture<Optional<PlayerProfile>> find(UUID uuid) {
            return CompletableFuture.completedFuture(Optional.ofNullable(rows.get(uuid)));
        }

        @Override
        public CompletableFuture<PlayerProfile> findOrCreate(UUID uuid, String name, Instant now) {
            var p = rows.containsKey(uuid) ? rows.get(uuid).withSeen(name, now) : new PlayerProfile(uuid, name, now, now);
            rows.put(uuid, p);
            return CompletableFuture.completedFuture(p);
        }

        @Override
        public CompletableFuture<Void> touchLastSeen(UUID uuid, Instant at) {
            rows.computeIfPresent(uuid, (u, p) -> p.withSeen(p.name(), at));
            return CompletableFuture.completedFuture(null);
        }
    }

    final MemoryRepository repo = new MemoryRepository();
    final PlayerService service = new PlayerService(repo, Clock.fixed(T0, ZoneOffset.UTC));
    final UUID uuid = UUID.randomUUID();

    @Test
    void loginCachesAndQuitEvicts() {
        service.handleLogin(uuid, "Raven").join();
        assertEquals("Raven", service.online(uuid).orElseThrow().name());
        service.handleQuit(uuid).join();
        assertTrue(service.online(uuid).isEmpty());
        assertEquals(T0, repo.rows.get(uuid).lastSeenAt());
    }

    @Test
    void firstJoinSurvivesRenameAndRejoin() {
        service.handleLogin(uuid, "Raven").join();
        service.handleQuit(uuid).join();
        service.handleLogin(uuid, "Hawk").join();
        var p = service.online(uuid).orElseThrow();
        assertEquals("Hawk", p.name());
        assertEquals(T0, p.firstJoinAt());
    }

    @Test
    void loadFallsBackToRepositoryForOfflinePlayers() {
        repo.findOrCreate(uuid, "Offline", T0).join();
        assertEquals("Offline", service.load(uuid).join().orElseThrow().name());
        assertTrue(service.load(UUID.randomUUID()).join().isEmpty());
    }

    @Test
    void discardDropsAHalfFinishedLogin() {
        service.handleLogin(uuid, "Raven").join();
        service.discard(uuid);
        assertEquals(0, service.onlineCount());
    }
}
