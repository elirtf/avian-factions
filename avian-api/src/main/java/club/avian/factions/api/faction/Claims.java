package club.avian.factions.api.faction;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;

import java.util.UUID;

/**
 * Chunk ownership as other modules see it (spec §9). Every lookup is an in-memory map read; the
 * database is written to but never read on this path (#4 prior art).
 */
public interface Claims {

    /** What occupies this chunk. Never null. */
    Territory at(World world, int chunkX, int chunkZ);

    /** Convenience for a location; does the {@code >> 4} without allocating a key object. */
    default Territory at(Location location) {
        return at(location.getWorld(), location.getBlockX() >> 4, location.getBlockZ() >> 4);
    }

    default Territory at(Block block) {
        return at(block.getWorld(), block.getX() >> 4, block.getZ() >> 4);
    }

    /** How many chunks this Faction holds. O(1) — read from the reverse index, never counted. */
    int countOf(UUID factionId);

    /** Total claimed chunks across every world. */
    int total();
}
