package club.avian.factions.combat;

import club.avian.factions.testing.MariaDbExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** A killed body's death debt must survive a restart, or its loot is duplicated. */
@ExtendWith(MariaDbExtension.class)
class LogoutDeathRepositoryIT {

    final LogoutDeathRepository repo = new LogoutDeathRepository.Jdbc(MariaDbExtension.database(),
            Clock.fixed(Instant.parse("2026-09-26T01:00:00Z"), ZoneOffset.UTC));

    @Test
    void aRecordedDeathIsThereAfterAReload() {
        var player = UUID.randomUUID();
        repo.record(player, UUID.randomUUID()).join();

        assertTrue(repo.loadPending().contains(player));
    }

    @Test
    void recordingTwiceKeepsOneDebt() {
        var player = UUID.randomUUID();
        repo.record(player, null).join();
        repo.record(player, UUID.randomUUID()).join();

        assertTrue(repo.loadPending().contains(player));
    }

    @Test
    void aPaidDeathIsGone() {
        var player = UUID.randomUUID();
        repo.record(player, null).join();
        repo.clear(player).join();

        assertFalse(repo.loadPending().contains(player));
    }
}
