package club.avian.factions.combat;

import club.avian.factions.api.config.ConfigSpec;
import club.avian.factions.api.module.AvianModule;
import club.avian.factions.api.module.ModuleContext;
import club.avian.factions.core.CoreModule;
import org.bukkit.Bukkit;

import java.time.Clock;
import java.util.List;
import java.util.Set;

/** Classic PvP feel (spec §44) and the combat tag with logout bodies (spec §43). */
public final class CombatModule implements AvianModule {

    private LogoutBodies bodies;

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
        ctx.registerListener(new ConsumableCooldowns(config, System::currentTimeMillis));
        ctx.registerListener(new EnchantLimit(config));   // at most max-enchantments-per-item per item
        // Golden apples are loot-only (#40).
        var recipes = new DisabledRecipes(DisabledRecipes.Registry.SERVER, ctx.logger());
        recipes.apply(config.get().disabledRecipes());
        config.onReload(cfg -> recipes.apply(cfg.disabledRecipes()));
        bodies = new LogoutBodies(ctx.plugin(), config,
                new LogoutDeathRepository.Jdbc(ctx.database(), Clock.systemUTC()), System::currentTimeMillis);
        var tags = new CombatTagListener(config, new CombatTags(System::currentTimeMillis), bodies);
        ctx.registerListener(bodies);
        ctx.registerListener(tags);
        Bukkit.getScheduler().runTaskTimer(ctx.plugin(), tags::tick, 5, 5);
        Bukkit.getScheduler().runTaskTimer(ctx.plugin(), bodies::tick, 20, 20);
        if (Bukkit.getPluginManager().getPlugin("RoseStacker") != null) {
            ctx.registerListener(new StackedCorpseListener(ctx.plugin(), config, StackedCorpseListener.roseStacker()));
        }

        // /avian reload and a mid-session preset change must reach players already online.
        config.onReload(cfg -> Bukkit.getOnlinePlayers().forEach(listener::applyAttackSpeed));
        Bukkit.getOnlinePlayers().forEach(listener::applyAttackSpeed);

        var cfg = config.get();
        ctx.logger().info("Combat: preset " + cfg.preset() + ", attack-speed "
                + (cfg.disableAttackCooldown() ? cfg.attackSpeed() : "vanilla")
                + ", sweep " + (cfg.disableSweepAttack() ? "off" : "on")
                + ", combat tag " + cfg.combatTagSeconds() + "s, logout body " + cfg.logoutBodySeconds() + "s");
    }

    @Override
    public void disable() {
        if (bodies != null) {
            bodies.removeAll();
        }
    }
}
