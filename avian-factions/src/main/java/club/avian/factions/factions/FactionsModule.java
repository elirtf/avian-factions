package club.avian.factions.factions;

import club.avian.factions.api.config.ConfigSpec;
import club.avian.factions.api.faction.Factions;
import club.avian.factions.api.module.AvianModule;
import club.avian.factions.api.module.ModuleContext;
import club.avian.factions.core.CoreModule;
import club.avian.factions.factions.command.FactionCommand;

import java.time.Clock;
import java.util.List;
import java.util.Set;

/** Factions, claims, power, relations (spec §7–§11). Claims and power arrive next. */
public final class FactionsModule implements AvianModule {

    @Override
    public String id() {
        return "factions";
    }

    @Override
    public Set<Class<? extends AvianModule>> dependsOn() {
        return Set.of(CoreModule.class);
    }

    @Override
    public List<ConfigSpec<?>> configs() {
        return List.of(FactionsConfig.SPEC);
    }

    @Override
    public void enable(ModuleContext ctx) {
        var config = ctx.config(FactionsConfig.SPEC);
        var index = new FactionIndex(new JdbcFactionRepository(ctx.database()), Clock.systemUTC(),
                config.get().seasonId());
        // Startup-only: the index must be complete before the command can run, and no player can
        // be online yet. Every later database touch goes through the async executor.
        int loaded = index.load();
        ctx.logger().info("Loaded " + loaded + " faction(s) for season " + config.get().seasonId());

        ctx.services().provide(Factions.class, index);
        ctx.commands().register(new FactionCommand(index, config, ctx.logger()).build(),
                "Faction commands", List.of("faction", "factions"));
    }
}
