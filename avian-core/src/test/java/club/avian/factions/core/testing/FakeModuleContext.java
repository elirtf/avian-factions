package club.avian.factions.core.testing;

import club.avian.factions.api.config.ConfigHandle;
import club.avian.factions.api.config.ConfigSpec;
import club.avian.factions.api.database.Database;
import club.avian.factions.api.module.AvianModule;
import club.avian.factions.api.module.ModuleContext;
import club.avian.factions.api.module.Services;
import club.avian.factions.api.player.Players;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/** ADR-0005 fixture: a ModuleContext for tests that need no Bukkit at all. */
public final class FakeModuleContext implements ModuleContext {

    private final AvianModule module;
    private final Services services;
    public final List<Listener> listeners = new ArrayList<>();
    public Database database;
    public Players players;

    public FakeModuleContext(AvianModule module, Services services) {
        this.module = module;
        this.services = services;
    }

    @Override
    public Plugin plugin() {
        throw new UnsupportedOperationException("no Bukkit in this test");
    }

    @Override
    public Logger logger() {
        return Logger.getLogger("test/" + module.id());
    }

    @Override
    public void registerListener(Listener listener) {
        listeners.add(listener);
    }

    @Override
    public <T> ConfigHandle<T> config(ConfigSpec<T> spec) {
        throw new UnsupportedOperationException("configure a ConfigService in this test");
    }

    @Override
    public Database database() {
        return database;
    }

    @Override
    public Players players() {
        return players;
    }

    @Override
    public Services services() {
        return services;
    }
}
