package club.avian.factions.factions.power;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/** Persistence for Power (ADR-0002). Single-row upserts; the whole Season loads once at boot. */
public interface PowerRepository {

    /** Every stored row for the Season. Boot-time, main thread (see {@code Database#queryDuringBoot}). */
    List<PlayerPower> loadSeason(int seasonId);

    /** Writes one player's row. */
    CompletableFuture<Void> save(PlayerPower power, int seasonId);

    /** Reads one player's row, for a player who was not in the Season snapshot. */
    CompletableFuture<PlayerPower> findOrCreate(UUID player, int seasonId, PowerConfig config,
                                                java.time.Instant now);
}
