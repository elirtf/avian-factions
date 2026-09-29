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
        return List.of(EconomyConfig.SPEC, HoeConfig.SPEC, SellWandConfig.SPEC);
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
        // Deposits and withdrawals complete on the DB thread; anything they tell a player hops back here.
        java.util.concurrent.Executor mainThread = task -> Bukkit.getScheduler().runTask(ctx.plugin(), task);
        ctx.registerListener(new StartingBalanceListener(economy, ctx.logger()));
        ctx.registerListener(new SugarCaneTokens(economy, config,
                () -> java.util.concurrent.ThreadLocalRandom.current().nextDouble(),
                mainThread, ctx.logger()));
        ctx.registerListener(new VillagerGolemGuard(config));

        // The Harvester Hoe (spec §31): /hoe to buy and upgrade, break cane with it to harvest.
        var hoe = ctx.config(HoeConfig.SPEC);
        java.util.function.ObjDoubleConsumer<org.bukkit.entity.Player> farmingXp =
                Bukkit.getPluginManager().getPlugin("AuraSkills") != null ? AuraSkillsXp::addFarmingXp : (p, xp) -> { };
        ctx.registerListener(new HoeHarvest(economy, sellValues, hoe, config,
                () -> java.util.concurrent.ThreadLocalRandom.current().nextDouble(), farmingXp, ctx.logger()));
        var hoeMenu = new HoeMenu(ctx.plugin(), economy, hoe);
        ctx.registerListener(hoeMenu);
        ctx.commands().register(new HoeCommand(hoeMenu, hoe).build(), "Buy and upgrade the Harvester Hoe");
        // Sell wands: right-click a container to sell it all at /shop prices.
        var sellWand = ctx.config(SellWandConfig.SPEC);
        ctx.registerListener(new SellWandListener(economy, sellValues, sellWand, System::currentTimeMillis, ctx.logger()));
        ctx.commands().register(new SellWandCommand(sellWand).build(), "Give sell wands (admins)");
        // /tokens and /gems: your balance; give|take|set is how crates, votes and events pay out (#26).
        var purchase = new Purchase(economy, mainThread,
                command -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command), ctx.logger());
        for (var node : new EconomyCommand(economy, mainThread, purchase).build()) {
            ctx.commands().register(node, "Your " + node.getLiteral() + "; admins give, take or set them");
        }

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
