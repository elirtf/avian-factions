package club.avian.factions.api.module;

import club.avian.factions.api.config.ConfigHandle;
import club.avian.factions.api.config.ConfigSpec;
import club.avian.factions.api.command.Commands;
import club.avian.factions.api.database.Database;
import club.avian.factions.api.player.Players;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

import java.util.logging.Logger;

/**
 * Everything a module receives at {@link AvianModule#enable} (ADR-0004). Accessors for further
 * core services (messages, scheduler, commands) are added as those services land.
 */
public interface ModuleContext {

    /** The one shipped {@link Plugin}, for Bukkit APIs that need a plugin handle. */
    Plugin plugin();

    /** Logger prefixed with the module id, e.g. {@code [AvianFactions/factions]}. */
    Logger logger();

    /** Registers a Bukkit listener against the plugin. */
    void registerListener(Listener listener);

    /** The handle for a spec this module declared in {@link AvianModule#configs()}. */
    <T> ConfigHandle<T> config(ConfigSpec<T> spec);

    /** Pooled, off-main database access for this module's repositories. */
    Database database();

    /** Brigadier command registration. */
    Commands commands();

    /** Avian Player cache and lookup. */
    Players players();

    /** Cross-module service registry, gated by {@link AvianModule#dependsOn()}. */
    Services services();
}
