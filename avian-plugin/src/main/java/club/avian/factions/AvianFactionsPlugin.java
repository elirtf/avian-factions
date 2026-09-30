package club.avian.factions;

import club.avian.factions.api.config.ConfigHandle;
import club.avian.factions.api.config.ConfigSpec;
import club.avian.factions.api.command.Commands;
import club.avian.factions.api.database.Database;
import club.avian.factions.api.module.AvianModule;
import club.avian.factions.api.module.ModuleContext;
import club.avian.factions.api.module.Services;
import club.avian.factions.api.player.Players;
import club.avian.factions.combat.CombatModule;
import club.avian.factions.core.CoreModule;
import club.avian.factions.economy.EconomyModule;
import club.avian.factions.core.CoreRuntime;
import club.avian.factions.core.config.ConfigLoadException;
import club.avian.factions.core.config.ConfigService;
import club.avian.factions.core.module.ModuleBootstrap;
import club.avian.factions.core.module.ServiceRegistry;
import club.avian.factions.factions.FactionsModule;
import club.avian.factions.ftop.FTopModule;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/** The single shipped plugin (ADR-0001). Owns the module list, which is the boot order (ADR-0004). */
public final class AvianFactionsPlugin extends JavaPlugin {

    private ConfigService configs;
    private CoreRuntime core;
    private List<AvianModule> modules;
    private boolean loadFailed;
    private ModuleBootstrap bootstrap;
    private final PluginCommands commands = new PluginCommands(this);

    @Override
    public void onLoad() {
        configs = new ConfigService(getDataFolder().toPath(), getLogger());
        core = new CoreRuntime(configs);
        modules = List.of(
                new CoreModule(core),
                new CombatModule(),
                new EconomyModule(),
                new FactionsModule(),
                new FTopModule()
        );
        try {
            for (AvianModule module : modules) {
                module.load(getLogger());
            }
        } catch (RuntimeException e) {
            getLogger().log(Level.SEVERE, "A module failed to load; the plugin will not enable", e);
            loadFailed = true;
        }
    }

    @Override
    public void onEnable() {
        if (loadFailed) {
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        var logFix = new PlayTimeManagerLogFix(getLogger());
        logFix.removeFilters();
        getServer().getPluginManager().registerEvents(logFix, this);
        var registry = new ServiceRegistry();
        bootstrap = new ModuleBootstrap(modules,
                module -> new PluginModuleContext(this, module, core, registry.viewFor(module)),
                getLogger());
        try {
            configs.loadAll(modules);          // ADR-0003: all files, all errors, before any enable
            bootstrap.enableAll();
        } catch (ConfigLoadException e) {
            getLogger().severe("Configuration errors — refusing to start:");
            e.errors().forEach(err -> getLogger().severe("  " + err));
            getLogger().severe("Fix the above and restart. Nothing has been loaded.");
            getServer().getPluginManager().disablePlugin(this);
        } catch (RuntimeException e) {
            getLogger().severe("Avian Factions refused to start; see above. Disabling.");
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        if (bootstrap != null) {
            bootstrap.disableAll();
        }
    }

    private record PluginModuleContext(AvianFactionsPlugin plugin, AvianModule module, CoreRuntime core,
                                       Services services) implements ModuleContext {

        @Override
        public Logger logger() {
            var log = Logger.getLogger(plugin.getLogger().getName() + "/" + module.id());
            log.setParent(plugin.getLogger());
            return log;
        }

        @Override
        public void registerListener(Listener listener) {
            plugin.getServer().getPluginManager().registerEvents(listener, plugin);
        }

        @Override
        public <T> ConfigHandle<T> config(ConfigSpec<T> spec) {
            if (!module.configs().contains(spec)) {
                throw new IllegalStateException("module '" + module.id() + "' did not declare " + spec.fileName() + " in configs()");
            }
            return core.configs().handle(spec);
        }

        @Override
        public Database database() {
            return core.database();
        }

        @Override
        public Players players() {
            return core.players();
        }

        @Override
        public Commands commands() {
            return plugin.commands;
        }
    }
}
