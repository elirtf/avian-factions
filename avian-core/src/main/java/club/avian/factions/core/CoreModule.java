package club.avian.factions.core;

import club.avian.factions.api.config.ConfigSpec;
import club.avian.factions.api.module.AvianModule;
import club.avian.factions.api.module.ModuleContext;
import club.avian.factions.api.player.Players;
import club.avian.factions.core.database.HikariDatabase;
import club.avian.factions.core.player.JdbcPlayerRepository;
import club.avian.factions.core.player.PlayerListener;
import club.avian.factions.core.player.PlayerService;


import java.time.Clock;
import java.util.List;

/** Bootstrap, configuration, database, profiles (spec §5). Messages, scheduler, commands follow. */
public final class CoreModule implements AvianModule {

    private final CoreRuntime runtime;
    private HikariDatabase database;

    public CoreModule(CoreRuntime runtime) {
        this.runtime = runtime;
    }

    @Override
    public String id() {
        return "core";
    }

    @Override
    public List<ConfigSpec<?>> configs() {
        return List.of(CoreConfig.SPEC);
    }

    @Override
    public void enable(ModuleContext ctx) {
        var cfg = ctx.config(CoreConfig.SPEC).get();
        var db = cfg.database();
        ctx.logger().info("Connecting to " + db.jdbcUrl() + " as " + db.user());
        // Startup-only blocking I/O: the pool handshake and migrations run before any player can
        // join. Everything after this point goes through Database#query on the db executor.
        database = new HikariDatabase(db.jdbcUrl(), db.user(), db.password(), db.poolSize(),
                ctx.logger());
        database.migrate();
        runtime.database(database);

        var players = new PlayerService(new JdbcPlayerRepository(database), Clock.systemUTC());
        runtime.players(players);
        ctx.registerListener(new PlayerListener(players, ctx.logger()));
        ctx.services().provide(Players.class, players);

        warmLegacyMaterials(ctx.logger());

        // Flat bedrock for chunks generated before it was switched on (new ones come flat from Paper).
        var config = ctx.config(CoreConfig.SPEC);
        ctx.registerListener(new club.avian.factions.core.world.BedrockFlattener(ctx.plugin(),
                () -> config.get().flattenOldBedrock()));

        ctx.logger().info(cfg.server().name() + " core ready (" + cfg.server().address() + ")");
    }

    /**
     * Pays Paper's one-time legacy-material conversion during startup instead of mid-game. The first
     * lookup of a pre-1.13 material converts every legacy id on the main thread, which froze the server
     * for over 10 seconds on the first /ah after a restart (CrazyAuctions sorts every material into its
     * categories; Paper watchdog, 2026-09-29). Here it happens before anyone can join.
     */
    @SuppressWarnings({"deprecation", "removal"})   // LEGACY_* is exactly what we want to touch
    private static void warmLegacyMaterials(java.util.logging.Logger log) {
        long start = System.nanoTime();
        try {
            org.bukkit.Material.LEGACY_APPLE.isEdible();
            log.info("Legacy materials converted at startup in " + (System.nanoTime() - start) / 1_000_000 + " ms");
        } catch (RuntimeException | LinkageError e) {
            log.warning("Could not warm legacy materials (the first /ah may stall): " + e);
        }
    }

    @Override
    public void disable() {
        if (database != null) {
            database.close();
        }
    }
}
