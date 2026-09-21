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
                runtime.isMainThread(), ctx.logger());
        database.migrate();
        runtime.database(database);

        var players = new PlayerService(new JdbcPlayerRepository(database), Clock.systemUTC());
        runtime.players(players);
        ctx.registerListener(new PlayerListener(players, ctx.logger()));
        ctx.services().provide(Players.class, players);

        ctx.logger().info(cfg.server().name() + " core ready (" + cfg.server().address() + ")");
    }

    @Override
    public void disable() {
        if (database != null) {
            database.close();
        }
    }
}
