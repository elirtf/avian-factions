package club.avian.factions.economy;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

/**
 * Marks mobs that a spawner a player placed spawned (set by {@link StackingRules} from RoseStacker's
 * spawn event). Stacking and spawner drops both ask it: wild mobs, bred mobs and mobs from dungeon or
 * fortress spawners are never marked, so they neither stack nor drop spawner loot like emeralds.
 */
final class PlacedSpawnerMark {

    private final NamespacedKey key;

    PlacedSpawnerMark(Plugin plugin) {
        this.key = new NamespacedKey(plugin, "from_placed_spawner");
    }

    boolean has(Entity entity) {
        return entity.getPersistentDataContainer().has(key, PersistentDataType.BYTE);
    }

    void set(Entity entity) {
        entity.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 1);
    }
}
