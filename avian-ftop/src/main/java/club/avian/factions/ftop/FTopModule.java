package club.avian.factions.ftop;

import club.avian.factions.api.config.ConfigSpec;
import club.avian.factions.api.economy.Currency;
import club.avian.factions.api.economy.Economy;
import club.avian.factions.api.module.AvianModule;
import club.avian.factions.api.module.ModuleContext;
import club.avian.factions.core.CoreModule;
import club.avian.factions.economy.EconomyModule;
import dev.rosewood.rosestacker.api.RoseStackerAPI;
import org.bukkit.Bukkit;

import java.time.Clock;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.LongFunction;

/**
 * F-Top (spec §11): what each faction's base is worth. Spawner stacks come from RoseStacker,
 * claims from FactionsUUID; both must be installed or the module stays off.
 */
public final class FTopModule implements AvianModule {

    @Override
    public String id() {
        return "ftop";
    }

    @Override
    public Set<Class<? extends AvianModule>> dependsOn() {
        return Set.of(CoreModule.class, EconomyModule.class);
    }

    @Override
    public List<ConfigSpec<?>> configs() {
        return List.of(FTopConfig.SPEC);
    }

    @Override
    public void enable(ModuleContext ctx) {
        var plugins = Bukkit.getPluginManager();
        if (!plugins.isPluginEnabled("FactionsUUID") || !plugins.isPluginEnabled("RoseStacker")) {
            ctx.logger().warning("F-Top needs FactionsUUID and RoseStacker; it is off");
            return;
        }
        var config = ctx.config(FTopConfig.SPEC);
        var valuation = new AtomicReference<>(new Valuation(config.get()));
        config.onReload(c -> valuation.set(new Valuation(c)));
        var economy = ctx.services().find(Economy.class).orElse(null);
        var clock = Clock.systemUTC();
        var owners = new ClaimOwners();

        var repository = new AssetRepository(ctx.database());
        var book = new AssetBook(config.get().pickupCost().graceMinutes() * 60_000L);
        // Startup-only blocking read: the book must be whole before any block event arrives.
        book.load(repository.loadAllDuringBoot());
        ctx.logger().info("Tracking " + book.size() + " F-Top asset(s)");

        SpawnerStacks stacks = block -> {
            var stack = RoseStackerAPI.getInstance().getStackedSpawner(block);
            return stack == null ? 1 : stack.getStackSize();
        };
        var tracker = new AssetTracker(ctx.plugin(), book, repository, stacks, valuation::get,
                () -> config.get().pickupCost(), owners, economy, clock, ctx.logger());
        ctx.registerListener(tracker);

        var service = new FTopService(book, tracker, repository, valuation::get, owners, clock, ctx.logger());
        long period = config.get().recalculateMinutes() * 60L * 20L;
        // First run a few seconds after enable, once worlds and claims are up.
        Bukkit.getScheduler().runTaskTimer(ctx.plugin(), service::recalculate, 100L, period);

        LongFunction<String> money = economy != null
                ? amount -> economy.format(Currency.MONEY, amount)
                : amount -> "$" + String.format("%,d", amount);
        ctx.commands().register(new FTopCommand(service, config, money, clock).build(),
                "Faction value ranking", List.of("factiontop"));
    }
}
