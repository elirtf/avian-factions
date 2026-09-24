package club.avian.factions.combat;

import club.avian.factions.api.config.ConfigHandle;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.plugin.Plugin;

/**
 * Spawner grinding speed. RoseStacker kills one mob of a stack per hit and puts the next one in
 * the same spot, but the dead one stays for its death animation (about a second) and the client
 * keeps aiming at it, so hits land on a corpse. Removing the body right away lets every click
 * count. See {@link CombatConfig#clearSpawnerMobCorpses()}.
 */
final class SpawnerCorpseListener implements Listener {

    /** RoseStacker's own tag for "came out of a spawner" (PersistentDataUtils.tagSpawnedFromSpawner). */
    static final NamespacedKey SPAWNER_SPAWNED = new NamespacedKey("rosestacker", "spawner_spawned");

    private final Plugin plugin;
    private final ConfigHandle<CombatConfig> config;

    SpawnerCorpseListener(Plugin plugin, ConfigHandle<CombatConfig> config) {
        this.plugin = plugin;
        this.config = config;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDeath(EntityDeathEvent event) {
        var entity = event.getEntity();
        if (!config.get().clearSpawnerMobCorpses() || entity.getKiller() == null || !isSpawnerMob(entity)) {
            return;
        }
        // Next tick, not now: drops and XP are spawned after this event, from the entity's location.
        plugin.getServer().getScheduler().runTask(plugin, entity::remove);
    }

    static boolean isSpawnerMob(Entity entity) {
        return entity.getPersistentDataContainer().has(SPAWNER_SPAWNED);
    }
}
