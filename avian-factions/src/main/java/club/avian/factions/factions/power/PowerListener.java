package club.avian.factions.factions.power;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Drives {@link PowerService} from the player lifecycle.
 *
 * <p>The death handler carries a same-tick duplicate guard: other plugins re-firing
 * {@link PlayerDeathEvent} for the same death would otherwise charge the penalty twice
 * (#4: MassiveCraft learned this the hard way).
 */
public final class PowerListener implements Listener {

    private final PowerService power;
    private final Predicate<Player> powerLossApplies;
    private final Logger log;
    private final Map<UUID, Long> lastDeathTick = new ConcurrentHashMap<>();

    /**
     * @param powerLossApplies whether this death costs power — the world/territory check, run
     *     <em>before</em> the deduction (#4: IF #180/#355 deducted first and checked after)
     */
    public PowerListener(PowerService power, Predicate<Player> powerLossApplies, Logger log) {
        this.power = power;
        this.powerLossApplies = powerLossApplies;
        this.log = log;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        power.handleLogin(event.getPlayer().getUniqueId())
                .exceptionally(logFailure("load power for " + event.getPlayer().getName()));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        power.handleQuit(event.getPlayer().getUniqueId())
                .exceptionally(logFailure("save power for " + event.getPlayer().getName()));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(PlayerDeathEvent event) {
        var player = event.getEntity();
        if (!powerLossApplies.test(player)) {
            return;
        }
        long tick = player.getWorld().getFullTime();
        var previous = lastDeathTick.put(player.getUniqueId(), tick);
        if (previous != null && previous == tick) {
            return;
        }
        power.handleDeath(player.getUniqueId())
                .exceptionally(logFailure("apply death power loss for " + player.getName()));
    }

    private java.util.function.Function<Throwable, Void> logFailure(String what) {
        return throwable -> {
            log.log(Level.WARNING, "Could not " + what, throwable);
            return null;
        };
    }
}
