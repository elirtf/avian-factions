package club.avian.factions.factions;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.LongSupplier;

/**
 * How many TNT hits each tough block has taken ({@link TntDurability}). A block's count is forgotten
 * once it goes {@code resetMillis} without a hit, so a wall heals after a raid. Kept in memory: a
 * restart heals every wall, which is the safe direction.
 */
final class BlockDurability {

    /** A block, by world and position. */
    record Key(UUID world, int x, int y, int z) {
    }

    private record Hits(int count, long lastHit) {
    }

    private final LongSupplier clock;
    private final Map<Key, Hits> hits = new HashMap<>();

    BlockDurability(LongSupplier clockMillis) {
        this.clock = clockMillis;
    }

    /**
     * Records one hit; true when this hit breaks the block (it has now taken {@code needed} hits), and
     * the block's count is cleared.
     */
    boolean hit(Key block, int needed, long resetMillis) {
        long now = clock.getAsLong();
        var before = hits.get(block);
        int count = (before == null || expired(before, now, resetMillis) ? 0 : before.count()) + 1;
        if (count >= needed) {
            hits.remove(block);
            return true;
        }
        hits.put(block, new Hits(count, now));
        return false;
    }

    /** Hits a block has taken and still remembers. */
    int hitsOn(Key block, long resetMillis) {
        var h = hits.get(block);
        return h == null || expired(h, clock.getAsLong(), resetMillis) ? 0 : h.count();
    }

    /** Drops forgotten entries; call now and then so the map can't grow without bound. */
    void prune(long resetMillis) {
        long now = clock.getAsLong();
        hits.values().removeIf(h -> expired(h, now, resetMillis));
    }

    int tracked() {
        return hits.size();
    }

    private static boolean expired(Hits h, long now, long resetMillis) {
        return resetMillis > 0 && now - h.lastHit() >= resetMillis;
    }
}
