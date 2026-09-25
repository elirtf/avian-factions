package club.avian.factions.combat;

import club.avian.factions.api.config.ConfigHandle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.plugin.Plugin;

import java.util.function.Predicate;

/**
 * Stack grinding speed. RoseStacker kills one mob of a stack per hit and puts the next one in the
 * same spot, but the dead one stays for its death animation (about a second) and the client keeps
 * aiming at it, so hits land on a corpse. Removing the body right away lets every click count.
 * See {@link CombatConfig#clearStackedMobCorpses()}.
 */
final class StackedCorpseListener implements Listener {

    private final Plugin plugin;
    private final ConfigHandle<CombatConfig> config;
    private final Predicate<LivingEntity> hasMoreInStack;

    /**
     * @param hasMoreInStack whether another mob of this one's stack will take its place. Asked
     *                       before RoseStacker handles the death (it listens at HIGH, we at LOW).
     */
    StackedCorpseListener(Plugin plugin, ConfigHandle<CombatConfig> config, Predicate<LivingEntity> hasMoreInStack) {
        this.plugin = plugin;
        this.config = config;
        this.hasMoreInStack = hasMoreInStack;
    }

    /** The real check, against RoseStacker. Only call when RoseStacker is installed. */
    static Predicate<LivingEntity> roseStacker() {
        return entity -> {
            var stack = dev.rosewood.rosestacker.api.RoseStackerAPI.getInstance().getStackedEntity(entity);
            return stack != null && stack.getStackSize() > 1;
        };
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onDeath(EntityDeathEvent event) {
        var entity = event.getEntity();
        if (!config.get().clearStackedMobCorpses() || entity.getKiller() == null || !hasMoreInStack.test(entity)) {
            return;
        }
        // Next tick, not now: drops and XP are spawned after this event, from the entity's location.
        plugin.getServer().getScheduler().runTask(plugin, entity::remove);
    }
}
