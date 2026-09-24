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
        return List.of(EconomyConfig.SPEC, HoeConfig.SPEC);
    }

    @Override
    public void enable(ModuleContext ctx) {
        var config = ctx.config(EconomyConfig.SPEC);
        economy = new EconomyService(new JdbcEconomyRepository(ctx.database(), Clock.systemUTC()),
                config, config.get().seasonId());
        ctx.logger().info("Loaded balances for " + economy.load() + " player(s)");

        var configured = new ConfiguredSellValues(config);
        // EconomyShopGUI owns prices when it is installed: an admin edits them there and players
        // see them there, and two price lists would be an exploit rather than an inconsistency.
        SellValues sellValues = configured;
        if (Bukkit.getPluginManager().getPlugin("EconomyShopGUI") != null) {
            sellValues = new ShopSellValues(configured, ctx.logger());
            ctx.logger().info("Sell prices come from EconomyShopGUI; economy.conf is the fallback");
        } else {
            ctx.logger().info(configured.size() + " sellable material(s) from economy.conf");
        }

        ctx.services().provide(Economy.class, economy);
        ctx.services().provide(SellValues.class, sellValues);
        ctx.registerListener(new StartingBalanceListener(economy, ctx.logger()));
        ctx.registerListener(new SugarCaneTokens(economy, config,
                () -> java.util.concurrent.ThreadLocalRandom.current().nextDouble(), ctx.logger()));
        ctx.registerListener(new VillagerGolemGuard(config));

        // The Harvester Hoe (spec §31): /hoe to buy and upgrade, break cane with it to harvest.
        var hoe = ctx.config(HoeConfig.SPEC);
        ctx.registerListener(new HoeHarvest(economy, sellValues, hoe, config,
                () -> java.util.concurrent.ThreadLocalRandom.current().nextDouble(), ctx.logger()));
        var hoeMenu = new HoeMenu(ctx.plugin(), economy, hoe);
        ctx.registerListener(hoeMenu);
        ctx.commands().register(new HoeCommand(hoeMenu, hoe).build(), "Buy and upgrade the Harvester Hoe");
        // /avian tokens|gems|money give|take|set: how crates, votes and events pay out (#26).
        ctx.commands().register(new EconomyCommand(economy).build(), "Give, take or set a player's money, tokens or gems");

        if (config.get().provideVault() && Bukkit.getPluginManager().getPlugin("Vault") != null) {
            var bridge = new VaultEconomyBridge(economy, "Avian");
            Bukkit.getServicesManager().register(net.milkbowl.vault.economy.Economy.class, bridge,
                    ctx.plugin(), ServicePriority.Highest);
            ctx.logger().info("Registered as Vault's economy provider");
        }

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new AvianPlaceholders(economy).register();
            ctx.logger().info("Registered %avian_...% placeholders");
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
