package club.avian.factions.api.module;

import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

import java.util.logging.Logger;

/**
 * Everything a module receives at {@link AvianModule#enable}. Accessors for core services are
 * added here as the services land (players, database, messages, scheduler, commands, config);
 * see ADR-0004 for the full intended surface.
 */
public interface ModuleContext {

    /** The one shipped {@link Plugin}, for Bukkit APIs that need a plugin handle. */
    Plugin plugin();

    /** Logger prefixed with the module id, e.g. {@code [AvianFactions] [factions]}. */
    Logger logger();

    /** Registers a Bukkit listener against the plugin. */
    void registerListener(Listener listener);

    /** Cross-module service registry, gated by {@link AvianModule#dependsOn()}. */
    Services services();
}
