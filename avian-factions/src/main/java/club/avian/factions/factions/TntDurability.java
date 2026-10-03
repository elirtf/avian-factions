package club.avian.factions.factions;

import club.avian.factions.api.config.ConfigHandle;
import dev.kitteh.factions.FLocation;
import dev.kitteh.factions.protection.Protection;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityExplodeEvent;

import java.util.HashSet;

/**
 * Raiding (spec §25; owner, 2026-10-02): TNT wears down blocks vanilla TNT can't break, obsidian first,
 * so a wall of it can still be raided. Each TNT exploding within {@code radius} is one hit; at the
 * configured count the block breaks with the explosion. Runs after FactionsUUID (HIGH), and asks
 * FactionsUUID's own explosion check for every block, so it never reaches where TNT is denied: safezone,
 * warzone, grace, a raid shield.
 */
final class TntDurability implements Listener {

    private final ConfigHandle<FactionsConfig> config;
    private final BlockDurability durability;

    TntDurability(ConfigHandle<FactionsConfig> config, BlockDurability durability) {
        this.config = config;
        this.durability = durability;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onExplode(EntityExplodeEvent event) {
        var cfg = config.get().tntDurability();
        var source = event.getEntity();
        if (!cfg.enabled() || !isTnt(source)) {
            return;
        }
        var center = event.getLocation();
        if (cfg.liquidsProtect() && center.getBlock().isLiquid()) {
            return;
        }
        long resetMillis = cfg.resetMinutes() * 60_000L;
        int r = (int) Math.ceil(cfg.radius());
        double radiusSquared = cfg.radius() * cfg.radius();
        var already = new HashSet<>(event.blockList());
        var world = center.getWorld();
        for (int dx = -r; dx <= r; dx++) {
            for (int dy = -r; dy <= r; dy++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (dx * dx + dy * dy + dz * dz > radiusSquared) {
                        continue;
                    }
                    Block block = world.getBlockAt(center.getBlockX() + dx, center.getBlockY() + dy, center.getBlockZ() + dz);
                    Integer needed = cfg.blocks().get(block.getType().name());
                    if (needed == null || already.contains(block)
                            || Protection.denyExplode(source, new FLocation(block.getLocation()))) {
                        continue;
                    }
                    var key = new BlockDurability.Key(world.getUID(), block.getX(), block.getY(), block.getZ());
                    if (durability.hit(key, needed, resetMillis)) {
                        event.blockList().add(block);
                    }
                }
            }
        }
    }

    /** Prunes forgotten hits (scheduled every few minutes). */
    void prune() {
        durability.prune(config.get().tntDurability().resetMinutes() * 60_000L);
    }

    private static boolean isTnt(Entity entity) {
        return entity != null && (entity.getType() == EntityType.TNT || entity.getType() == EntityType.TNT_MINECART);
    }
}
