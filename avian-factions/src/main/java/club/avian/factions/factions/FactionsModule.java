package club.avian.factions.factions;

import club.avian.factions.api.module.AvianModule;
import club.avian.factions.api.module.ModuleContext;
import club.avian.factions.core.CoreModule;

import java.util.Set;

/** Factions, claims, power, relations (spec §7–§11). Skeleton only; #13 fills it in. */
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
    public void enable(ModuleContext ctx) {
        ctx.logger().info("factions skeleton registered");
    }
}
