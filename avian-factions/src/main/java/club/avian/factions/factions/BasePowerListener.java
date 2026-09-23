package club.avian.factions.factions;

import club.avian.factions.api.config.ConfigHandle;
import dev.kitteh.factions.event.FactionCreateEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

/** Gives every new faction the configured base power, which FactionsUUID has no setting for. */
public final class BasePowerListener implements Listener {

    private final ConfigHandle<FactionsConfig> config;

    public BasePowerListener(ConfigHandle<FactionsConfig> config) {
        this.config = config;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onCreate(FactionCreateEvent event) {
        event.getFaction().powerBoost(config.get().factionBasePower());
    }
}
