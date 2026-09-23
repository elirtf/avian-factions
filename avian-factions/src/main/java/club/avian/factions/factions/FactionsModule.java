package club.avian.factions.factions;

import club.avian.factions.api.config.ConfigSpec;
import club.avian.factions.api.module.AvianModule;
import club.avian.factions.api.module.ModuleContext;
import club.avian.factions.core.CoreModule;
import org.bukkit.Bukkit;

import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Our additions to FactionsUUID (ADR-0007), which owns factions, claims, power, relations and
 * protection. Everything here talks to FactionsUUID through its API; nothing re-implements it.
 */
public final class FactionsModule implements AvianModule {

    static final String FACTIONS_PLUGIN = "FactionsUUID";

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
    public void load(Logger logger) {
        // Only the plugin being present is checked here: classes resolve, but it has not enabled.
        if (Bukkit.getPluginManager().getPlugin(FACTIONS_PLUGIN) == null) {
            return;
        }
        ClaimBoost.register();
        logger.info("Registered the " + ClaimBoost.NAME + " upgrade with " + FACTIONS_PLUGIN);
    }

    @Override
    public void enable(ModuleContext ctx) {
        // FactionsUUID is a soft dependency: the rest of Avian (economy, combat) works without it,
        // and loading our listener without its classes present would fail at registration.
        if (!Bukkit.getPluginManager().isPluginEnabled(FACTIONS_PLUGIN)) {
            ctx.logger().warning(FACTIONS_PLUGIN + " is not installed; faction additions are off");
            return;
        }
        var config = ctx.config(FactionsConfig.SPEC);
        ctx.registerListener(new BasePowerListener(config));
        UpgradeSwitch.apply(config.get().enabledUpgrades(), ctx.logger());
        config.onReload(c -> UpgradeSwitch.apply(c.enabledUpgrades(), ctx.logger()));
        ctx.logger().info("Hooked " + FACTIONS_PLUGIN + ": new factions start with "
                + config.get().factionBasePower() + " base power");
    }
}
