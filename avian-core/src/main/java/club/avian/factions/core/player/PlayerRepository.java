package club.avian.factions.core.player;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/** Persistence for Avian Player profiles (ADR-0002: the only way core touches the players table). */
public interface PlayerRepository {

    CompletableFuture<Optional<PlayerProfile>> find(UUID uuid);

    /**
     * Loads the profile, creating it on first join; refreshes {@code name} and {@code lastSeenAt}
     * either way. Returns the stored state after the write.
     */
    CompletableFuture<PlayerProfile> findOrCreate(UUID uuid, String name, Instant now);

    /** Records the player leaving. */
    CompletableFuture<Void> touchLastSeen(UUID uuid, Instant at);
}
