package club.avian.factions.factions.protection;

import club.avian.factions.api.faction.Claims;
import club.avian.factions.api.faction.ProtectionPolicy;
import it.unimi.dsi.fastutil.longs.Long2BooleanMap;
import it.unimi.dsi.fastutil.longs.Long2BooleanOpenHashMap;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;

import java.util.List;

/**
 * Explosion protection (spec §25–26).
 *
 * <p>The algorithm matters: decide once per <em>distinct chunk</em>, then filter the block list —
 * never once per block. A TNT stack touches thousands of blocks but only a handful of chunks, and
 * the plugins that decide per block allocate a key and do a map lookup for every one of them (#4).
 */
public final class ExplosionProtectionListener implements Listener {

    private final Claims claims;
    private final ProtectionPolicy policy;

    public ExplosionProtectionListener(Claims claims, ProtectionPolicy policy) {
        this.claims = claims;
        this.policy = policy;
    }

    /** TNT, creepers, fireballs, withers, minecarts, wind charges. */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        filter(event.getLocation().getWorld(), event.blockList());
    }

    /** Beds, respawn anchors and end crystals, which explode without an entity (MC #1254). */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        filter(event.getBlock().getWorld(), event.blockList());
    }

    /**
     * Removes from {@code blocks} everything in a chunk where explosions are denied.
     *
     * <p>Cost is one map lookup per block plus one policy call per distinct chunk, with no object
     * allocated per block.
     */
    private void filter(World world, List<Block> blocks) {
        if (blocks.isEmpty()) {
            return;
        }
        Long2BooleanMap denyByChunk = new Long2BooleanOpenHashMap();
        blocks.removeIf(block -> {
            int chunkX = block.getX() >> 4;
            int chunkZ = block.getZ() >> 4;
            long key = ((long) chunkX << 32) | (chunkZ & 0xFFFFFFFFL);
            if (denyByChunk.containsKey(key)) {
                return denyByChunk.get(key);
            }
            boolean deny = policy.denyExplosion(claims.at(world, chunkX, chunkZ));
            denyByChunk.put(key, deny);
            return deny;
        });
    }
}
