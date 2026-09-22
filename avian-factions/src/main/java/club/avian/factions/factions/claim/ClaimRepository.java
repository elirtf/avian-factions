package club.avian.factions.factions.claim;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Persistence for Claims (ADR-0002). Single-row upserts and deletes — never a whole-world blob
 * rewritten on each change, which is how MassiveCraft lost data (#4).
 */
public interface ClaimRepository {

    /** Every claim in the Season. Boot-time, main thread (see {@code Database#queryDuringBoot}). */
    List<Claim> loadSeason(int seasonId);

    CompletableFuture<Void> save(Claim claim, int seasonId);

    CompletableFuture<Void> delete(String world, int chunkX, int chunkZ, int seasonId);

    /** Removes every claim of a Faction, for disband and unclaim-all. */
    CompletableFuture<Void> deleteByFaction(UUID factionId, int seasonId);
}
