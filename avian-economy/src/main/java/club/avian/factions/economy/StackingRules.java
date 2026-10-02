package club.avian.factions.economy;

import club.avian.factions.api.config.ConfigHandle;
import dev.rosewood.rosestacker.event.EntityStackEvent;
import dev.rosewood.rosestacker.event.PostStackedSpawnerSpawnEvent;
import dev.rosewood.rosestacker.stack.StackedEntity;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.ArrayList;
import java.util.List;

/**
 * Which mobs RoseStacker may stack ({@code economy.conf} {@code stacking}; owner, 2026-10-01): only
 * mobs from spawners a player placed, and never the emerald-economy types (villagers, iron golems,
 * silverfish). RoseStacker's own {@code only-stack-from-spawners} already keeps wild mobs apart; it
 * can't tell a placed spawner from a dungeon one, so mobs from placed spawners are marked here as they
 * spawn, and a stack with an unmarked mob in it is refused.
 */
final class StackingRules implements Listener {

    private final ConfigHandle<EconomyConfig> config;
    private final PlacedSpawnerMark mark;

    StackingRules(PlacedSpawnerMark mark, ConfigHandle<EconomyConfig> config) {
        this.config = config;
        this.mark = mark;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSpawnerSpawn(PostStackedSpawnerSpawnEvent event) {
        if (!event.getStack().isPlacedByPlayer()) {
            return;
        }
        for (var stack : event.getSpawnedStacks()) {
            markStack(stack);
        }
        for (var stack : event.getModifiedStacks()) {
            markStack(stack);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onStack(EntityStackEvent event) {
        var entities = new ArrayList<Entity>();
        entities.add(event.getStack().getEntity());
        for (var target : event.getTargets()) {
            entities.add(target.getEntity());
        }
        if (!mayStack(entities, config.get().stacking())) {
            event.setCancelled(true);
        }
    }

    /** Whether all these mobs may become one stack. */
    boolean mayStack(List<? extends Entity> entities, EconomyConfig.Stacking rules) {
        for (var entity : entities) {
            if (entity == null || rules.neverStack().contains(entity.getType().name())) {
                return false;
            }
            if (rules.onlyPlayerPlacedSpawners() && !mark.has(entity)) {
                return false;
            }
        }
        return true;
    }

    private void markStack(StackedEntity stack) {
        if (stack.getEntity() != null) {
            mark.set(stack.getEntity());
        }
    }
}
