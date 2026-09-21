package club.avian.factions.api.player;

import java.time.Instant;
import java.util.UUID;

/**
 * The server's read view of a player (see CONTEXT.md "Avian Player"). Thin and safe to hand
 * around; mutations go through services that take a UUID and a reason (ADR-0004).
 */
public interface AvianPlayer {

    UUID uuid();

    /** Last known name; refreshed on every login. */
    String name();

    Instant firstJoinAt();

    Instant lastSeenAt();
}
