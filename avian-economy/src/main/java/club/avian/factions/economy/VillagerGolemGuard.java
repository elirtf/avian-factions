package club.avian.factions.economy;

import club.avian.factions.api.config.ConfigHandle;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;

/** Stops villagers summoning iron golems, so iron farms cannot undercut golem spawners. */
record VillagerGolemGuard(ConfigHandle<EconomyConfig> config) implements Listener {

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onSpawn(CreatureSpawnEvent event) {
        if (event.getEntityType() == EntityType.IRON_GOLEM
                && event.getSpawnReason() == CreatureSpawnEvent.SpawnReason.VILLAGE_DEFENSE
                && !config.get().villagerIronGolems()) {
            event.setCancelled(true);
        }
    }
}
