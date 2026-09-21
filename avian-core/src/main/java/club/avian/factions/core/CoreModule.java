package club.avian.factions.core;

import club.avian.factions.api.module.AvianModule;
import club.avian.factions.api.module.ModuleContext;

/**
 * Bootstrap, configuration, profiles, database, messages (spec §5). For now: proves the module
 * pipeline works end to end. Database and Configurate arrive with #10.
 */
public final class CoreModule implements AvianModule {

    @Override
    public String id() {
        return "core";
    }

    @Override
    public void enable(ModuleContext ctx) {
        ctx.logger().info("Hello from avian-core on " + ctx.plugin().getServer().getName()
                + " " + ctx.plugin().getServer().getMinecraftVersion());
    }
}
