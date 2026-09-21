package club.avian.factions;

import club.avian.factions.api.module.AvianModule;
import club.avian.factions.api.module.ModuleContext;
import club.avian.factions.api.module.Services;
import club.avian.factions.core.CoreModule;
import club.avian.factions.core.module.ModuleBootstrap;
import club.avian.factions.core.module.ServiceRegistry;
import club.avian.factions.factions.FactionsModule;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.logging.Logger;

/** The single shipped plugin (ADR-0001). Owns the module list, which is the boot order (ADR-0004). */
public final class AvianFactionsPlugin extends JavaPlugin {

    private ModuleBootstrap bootstrap;

    @Override
    public void onEnable() {
        var registry = new ServiceRegistry();
        bootstrap = new ModuleBootstrap(
                List.of(
                        new CoreModule(),
                        new FactionsModule()
                ),
                module -> new PluginModuleContext(this, module, registry.viewFor(module)),
                getLogger());
        try {
            bootstrap.enableAll();
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

    private record PluginModuleContext(AvianFactionsPlugin plugin, AvianModule module, Services services)
            implements ModuleContext {

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
    }
}
