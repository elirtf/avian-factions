package club.avian.factions;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.filter.CompositeFilter;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginEnableEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Undoes PlayTimeManager's log filter. On enable it adds a filter to the server's root logger that
 * drops every line from any logger named "Hikari", errors included, to hide its own pool's startup
 * chatter. That silences the connection pools of every plugin enabled after it, ours, AuraSkills'
 * and LuckPerms' among them, so a database outage would never reach the log (found 2026-09-29: the
 * CI log check stopped seeing AuraSkills connect). This removes the filter the moment
 * PlayTimeManager finishes enabling; plugin.yml has us load before it so the listener is in place.
 */
final class PlayTimeManagerLogFix implements Listener {

    static final String FILTER_CLASS = "me.thegabro.playtimemanager.Database.LogFilter";

    private final Logger logger;

    PlayTimeManagerLogFix(Logger logger) {
        this.logger = logger;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPluginEnable(PluginEnableEvent event) {
        if (event.getPlugin().getName().equals("PlayTimeManager")) {
            removeFilters();
        }
    }

    /** Removes PlayTimeManager's filter from the root logger if it is there. */
    void removeFilters() {
        LoggerConfig root = ((org.apache.logging.log4j.core.Logger) LogManager.getRootLogger()).get();
        int removed = removeFrom(root);
        if (removed > 0) {
            logger.info("Removed PlayTimeManager's log filter, which hid every database pool's messages");
        }
    }

    static int removeFrom(LoggerConfig config) {
        List<Filter> filters = new ArrayList<>();
        Filter filter = config.getFilter();
        if (filter instanceof CompositeFilter composite) {
            composite.forEach(filters::add);
        } else if (filter != null) {
            filters.add(filter);
        }
        int removed = 0;
        for (Filter each : filters) {
            if (each.getClass().getName().equals(FILTER_CLASS)) {
                config.removeFilter(each);
                removed++;
            }
        }
        return removed;
    }
}
