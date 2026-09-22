package club.avian.factions.factions;

import club.avian.factions.api.config.ConfigSpec;
import club.avian.factions.api.faction.Factions;
import club.avian.factions.api.module.AvianModule;
import club.avian.factions.api.module.ModuleContext;
import club.avian.factions.core.CoreModule;
import club.avian.factions.factions.command.FactionCommand;
import club.avian.factions.api.faction.Claims;
import club.avian.factions.factions.claim.ClaimIndex;
import club.avian.factions.factions.claim.JdbcClaimRepository;
import club.avian.factions.factions.power.JdbcPowerRepository;
import club.avian.factions.factions.power.PowerListener;
import club.avian.factions.api.faction.ProtectionPolicy;
import club.avian.factions.api.faction.Territory;
import club.avian.factions.factions.power.PowerService;
import club.avian.factions.factions.protection.BlockProtectionListener;
import club.avian.factions.factions.protection.DenyFeedback;
import club.avian.factions.factions.protection.EntityProtectionListener;
import club.avian.factions.factions.protection.ExplosionProtectionListener;
import club.avian.factions.factions.protection.FactionProtectionPolicy;
import club.avian.factions.factions.protection.InteractProtectionListener;
import club.avian.factions.factions.protection.InventoryProtectionListener;
import club.avian.factions.factions.protection.ProtectionGuard;

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

        var power = new PowerService(new JdbcPowerRepository(ctx.database()), config, Clock.systemUTC(),
                config.get().seasonId());
        ctx.logger().info("Loaded power for " + power.load() + " player(s)");

        var claimableWorlds = new java.util.HashSet<>(config.get().claims().worlds());
        var claims = new ClaimIndex(new JdbcClaimRepository(ctx.database()), index, Clock.systemUTC(),
                config.get().seasonId(), claimableWorlds::contains);
        ctx.logger().info("Loaded " + claims.load() + " claim(s)");
        // Deaths in a safezone cost nothing (spec §9); everywhere else they do. The check runs
        // before the deduction, which is the ordering IF #180/#355 got wrong.
        ctx.registerListener(new PowerListener(power,
                player -> claims.at(player.getLocation()).kind() != Territory.Kind.SAFEZONE,
                ctx.logger()));

        index.onDisband(factionId -> claims.unclaimAll(factionId)
                .exceptionally(t -> {
                    ctx.logger().log(java.util.logging.Level.SEVERE, "Could not release claims of a disbanded faction", t);
                    return null;
                }));

        // Protection: one policy, several listeners that only translate events into calls to it.
        var bypassPermission = config.get().protection().bypassPermission();
        var policy = new FactionProtectionPolicy(config,
                uuid -> {
                    var player = org.bukkit.Bukkit.getPlayer(uuid);
                    return player != null && player.hasPermission(bypassPermission);
                },
                (uuid, faction) -> faction.hasMember(uuid),
                faction -> power.isRaidable(faction, claims.countOf(faction.id())));
        var feedback = new DenyFeedback(config);
        var guard = new ProtectionGuard(claims, policy, feedback);
        ctx.registerListener(new BlockProtectionListener(guard, config));
        ctx.registerListener(new InteractProtectionListener(guard));
        ctx.registerListener(new EntityProtectionListener(guard));
        ctx.registerListener(new ExplosionProtectionListener(claims, policy));
        ctx.registerListener(new InventoryProtectionListener(claims));

        ctx.services().provide(ProtectionPolicy.class, policy);
        ctx.services().provide(Factions.class, index);
        ctx.services().provide(Claims.class, claims);
        ctx.commands().register(new FactionCommand(index, claims, power, config, ctx.logger()).build(),
                "Faction commands", List.of("faction", "factions"));
    }
}
