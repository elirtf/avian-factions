package club.avian.factions.core.player;

import club.avian.factions.testing.MariaDbExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MariaDbExtension.class)
class JdbcPlayerRepositoryIT {

    static final Instant T0 = Instant.parse("2026-09-21T12:00:00.123Z");
    static final Instant T1 = Instant.parse("2026-09-22T08:30:00.456Z");

    final PlayerRepository repo = new JdbcPlayerRepository(MariaDbExtension.database());

    @Test
    void firstJoinCreatesRowAndRejoinKeepsFirstJoinButRefreshesNameAndLastSeen() {
        var uuid = UUID.randomUUID();
        var first = repo.findOrCreate(uuid, "Raven", T0).join();
        assertEquals("Raven", first.name());
        assertEquals(T0, first.firstJoinAt());
        assertEquals(T0, first.lastSeenAt());

        var again = repo.findOrCreate(uuid, "Hawk", T1).join();
        assertEquals("Hawk", again.name());
        assertEquals(T0, again.firstJoinAt(), "first join must survive a rejoin");
        assertEquals(T1, again.lastSeenAt());
    }

    @Test
    void touchLastSeenUpdatesOnlyThatColumn() {
        var uuid = UUID.randomUUID();
        repo.findOrCreate(uuid, "Raven", T0).join();
        repo.touchLastSeen(uuid, T1).join();
        var p = repo.find(uuid).join().orElseThrow();
        assertEquals("Raven", p.name());
        assertEquals(T0, p.firstJoinAt());
        assertEquals(T1, p.lastSeenAt());
    }

    @Test
    void unknownPlayerIsEmpty() {
        assertTrue(repo.find(UUID.randomUUID()).join().isEmpty());
    }
}
