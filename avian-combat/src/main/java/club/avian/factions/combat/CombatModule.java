package club.avian.factions.combat;

import club.avian.factions.api.config.ConfigSpec;
import club.avian.factions.api.module.AvianModule;
import club.avian.factions.api.module.ModuleContext;
import club.avian.factions.core.CoreModule;
import org.bukkit.Bukkit;

import java.util.List;
import java.util.Set;

/** Classic PvP feel: no attack cooldown, 1.8 knockback, no sweep (spec §44). */
public final class CombatModule implements AvianModule {

    @Override
    public String id() {
        return "combat";
    }

    @Override
    public Set<Class<? extends AvianModule>> dependsOn() {
        return Set.of(CoreModule.class);
    }

    @Override
    public List<ConfigSpec<?>> configs() {
        return List.of(CombatConfig.SPEC);
    }

    @Override
    public void enable(ModuleContext ctx) {
        var config = ctx.config(CombatConfig.SPEC);
        var listener = new CombatListener(config);
        ctx.registerListener(listener);
        if (Bukkit.getPluginManager().getPlugin("RoseStacker") != null) {
            ctx.registerListener(new StackedCorpseListener(ctx.plugin(), config, StackedCorpseListener.roseStacker()));
        }

        // /avian reload and a mid-session preset change must reach players already online.
        config.onReload(cfg -> Bukkit.getOnlinePlayers().forEach(listener::applyAttackSpeed));
        Bukkit.getOnlinePlayers().forEach(listener::applyAttackSpeed);

        var cfg = config.get();
        ctx.logger().info("Combat: preset " + cfg.preset() + ", attack-speed "
                + (cfg.disableAttackCooldown() ? cfg.attackSpeed() : "vanilla")
                + ", sweep " + (cfg.disableSweepAttack() ? "off" : "on"));
    }
}
