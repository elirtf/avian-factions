package club.avian.factions.factions;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.function.Predicate;
import java.util.logging.Logger;

/**
 * Keeps {@code /rtp} out of faction land (#45). BetterRTP's own FactionsUUID hook targets the
 * pre-4.x API and never enables, so we veto its candidates instead: BetterRTP fires
 * {@code RTP_FindLocationEvent} for every spot it rolls, and cancelling it makes it roll again (up
 * to its {@code MaxAttempts}).
 *
 * <p>Registered by class name, so BetterRTP is not a build dependency for one event.
 */
final class RtpClaimGuard implements Listener {

    static final String EVENT = "me.SuperRonanCraft.BetterRTP.references.customEvents.RTP_FindLocationEvent";

    private final Predicate<Location> claimed;

    RtpClaimGuard(Predicate<Location> claimed) {
        this.claimed = claimed;
    }

    /** True when the candidate was refused. */
    boolean veto(Location candidate, Cancellable event) {
        if (candidate != null && claimed.test(candidate)) {
            event.setCancelled(true);
            return true;
        }
        return false;
    }

    /** Hooks BetterRTP if it is running; otherwise does nothing. */
    static void register(Plugin plugin, Predicate<Location> claimed, Logger log) {
        if (!Bukkit.getPluginManager().isPluginEnabled("BetterRTP")) {
            return;
        }
        try {
            @SuppressWarnings("unchecked")
            var type = (Class<? extends Event>) Class.forName(EVENT, true,
                    Bukkit.getPluginManager().getPlugin("BetterRTP").getClass().getClassLoader());
            Method location = type.getMethod("getLocation");
            var guard = new RtpClaimGuard(claimed);
            Bukkit.getPluginManager().registerEvent(type, guard, EventPriority.NORMAL, (listener, event) -> {
                if (type.isInstance(event) && event instanceof Cancellable cancellable && !cancellable.isCancelled()) {
                    try {
                        guard.veto((Location) location.invoke(event), cancellable);
                    } catch (ReflectiveOperationException e) {
                        log.warning("RTP claim check failed: " + e);
                    }
                }
            }, plugin);
            log.info("/rtp avoids faction claims (BetterRTP hooked)");
        } catch (ReflectiveOperationException | ClassCastException e) {
            log.warning("BetterRTP is running but its location event was not found; /rtp may land in claims: " + e);
        }
    }
}
