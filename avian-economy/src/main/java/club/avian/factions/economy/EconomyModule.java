package club.avian.factions.economy;

import club.avian.factions.api.config.ConfigSpec;
import club.avian.factions.api.economy.Economy;
import club.avian.factions.api.economy.SellValues;
import club.avian.factions.api.module.AvianModule;
import club.avian.factions.api.module.ModuleContext;
import club.avian.factions.core.CoreModule;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.ServicePriority;

import java.time.Clock;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

/** Money, tokens and gems, with an audited transaction trail (spec §12, §66). */
public final class EconomyModule implements AvianModule {

    private EconomyService economy;

    @Override
    public String id() {
        return "economy";
    }

    @Override
    public Set<Class<? extends AvianModule>> dependsOn() {
        return Set.of(CoreModule.class);
    }

    @Override
    public List<ConfigSpec<?>> configs() {
        return List.of(EconomyConfig.SPEC);
    }

    @Override
    public void enable(ModuleContext ctx) {
        var config = ctx.config(EconomyConfig.SPEC);
        economy = new EconomyService(new JdbcEconomyRepository(ctx.database(), Clock.systemUTC()),
                config, config.get().seasonId());
        ctx.logger().info("Loaded balances for " + economy.load() + " player(s)");

        var sellValues = new ConfiguredSellValues(config);
        ctx.logger().info(sellValues.size() + " sellable material(s)");

        ctx.services().provide(Economy.class, economy);
        ctx.services().provide(SellValues.class, sellValues);
        ctx.registerListener(new StartingBalanceListener(economy, ctx.logger()));

        if (config.get().provideVault() && Bukkit.getPluginManager().getPlugin("Vault") != null) {
            var bridge = new VaultEconomyBridge(economy, "Avian");
            Bukkit.getServicesManager().register(net.milkbowl.vault.economy.Economy.class, bridge,
                    ctx.plugin(), ServicePriority.Highest);
            ctx.logger().info("Registered as Vault's economy provider");
        }
    }

    /** Gives a player their starting balances the first time they join. */
    private record StartingBalanceListener(EconomyService economy, java.util.logging.Logger log)
            implements Listener {

        @EventHandler(priority = EventPriority.MONITOR)
        public void onJoin(PlayerJoinEvent event) {
            var uuid = event.getPlayer().getUniqueId();
            // First join only. A player who already has balances has been paid; paying again on
            // every login would mint money.
            if (economy.hasAccount(uuid)) {
                return;
            }
            economy.grantStartingBalances(uuid).exceptionally(t -> {
                log.log(Level.WARNING, "Could not grant starting balances to " + event.getPlayer().getName(), t);
                return null;
            });
        }
    }
}
