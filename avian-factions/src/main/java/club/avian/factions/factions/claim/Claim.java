package club.avian.factions.factions.claim;

import java.time.Instant;
import java.util.UUID;

/** One claimed chunk as stored. */
public record Claim(String world, int chunkX, int chunkZ, UUID factionId, UUID claimedBy, Instant claimedAt) {

    public long key() {
        return ChunkKey.of(chunkX, chunkZ);
    }
}
