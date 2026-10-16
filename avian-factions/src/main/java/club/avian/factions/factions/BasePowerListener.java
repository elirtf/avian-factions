package club.avian.factions.factions;

import club.avian.factions.api.config.ConfigHandle;
import dev.kitteh.factions.event.FactionCreateEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

/**
 * Gives every faction the configured base power, which FactionsUUID has no setting for: new ones on creation, and
 * existing ones on start-up when the base is raised (a faction already boosted above it keeps its boost).
 */
public final class BasePowerListener implements Listener {

    private final ConfigHandle<FactionsConfig> config;

    public BasePowerListener(ConfigHandle<FactionsConfig> config) {
        this.config = config;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onCreate(FactionCreateEvent event) {
        event.getFaction().powerBoost(config.get().factionBasePower());
    }

    /** Raises every faction's boost to at least the base. Returns how many changed. */
    public int raiseExisting() {
        double base = config.get().factionBasePower();
        int changed = 0;
        for (var faction : dev.kitteh.factions.Factions.factions().all()) {
            if (faction.isNormal() && faction.powerBoost() < base) {
                faction.powerBoost(base);
                changed++;
            }
        }
        return changed;
    }
}
