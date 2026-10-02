package club.avian.factions.economy;

import club.avian.factions.api.config.ConfigHandle;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Extra drops for mobs from spawners players placed ({@code economy.conf} {@code spawner-drops}).
 * The emerald economy (owner, 2026-10-01): a villager from a villager spawner drops an emerald,
 * however it dies, so it farms like every other spawner. Bred, traded-for or wild villagers are never
 * marked ({@link PlacedSpawnerMark}), so breeding can't become an emerald farm.
 */
record SpawnerDrops(ConfigHandle<EconomyConfig> config, PlacedSpawnerMark mark) implements Listener {

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDeath(EntityDeathEvent event) {
        var drops = config.get().spawnerDrops().get(event.getEntityType().name());
        if (drops == null || drops.isEmpty() || !mark.has(event.getEntity())) {
            return;
        }
        drops.forEach((material, amount) -> {
            var type = Material.matchMaterial(material);
            if (type != null && amount > 0) {
                event.getDrops().add(new ItemStack(type, amount));
            }
        });
    }
}
