package club.avian.factions.core.database;

import club.avian.factions.api.database.Database;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.flywaydb.core.Flyway;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

/**
 * HikariCP pool + a bounded database executor + Flyway migrations (ADR-0002).
 *
 * <p>{@link #query} and {@link #execute} never block the caller: they hand the work to the executor
 * and return at once, so scheduling a write from the main thread is the intended write-behind
 * pattern (spec §65). What must never happen on the main thread is waiting on the returned future.
 */
public final class HikariDatabase implements Database, AutoCloseable {

    private final HikariDataSource pool;
    private final ExecutorService executor;
    private final Logger log;
    private final Map<Object, CompletableFuture<Void>> tails = new ConcurrentHashMap<>();

    public HikariDatabase(String jdbcUrl, String user, String password, int poolSize,
                          Logger log) {
        var cfg = new HikariConfig();
        cfg.setJdbcUrl(jdbcUrl);
        // Class literal, not a string: in the shaded jar this becomes the relocated driver, which
        // DriverManager cannot see from a Bukkit plugin classloader. Hikari then calls the driver directly.
        cfg.setDriverClassName(org.mariadb.jdbc.Driver.class.getName());
        cfg.setUsername(user);
        cfg.setPassword(password);
        cfg.setMaximumPoolSize(poolSize);
        cfg.setPoolName("avian-db");
        cfg.setConnectionTimeout(TimeUnit.SECONDS.toMillis(10));
        cfg.setInitializationFailTimeout(TimeUnit.SECONDS.toMillis(10));
        this.pool = new HikariDataSource(cfg);
        var counter = new AtomicInteger();
        this.executor = Executors.newFixedThreadPool(poolSize, r -> {
            var t = new Thread(r, "avian-db-" + counter.incrementAndGet());
            t.setDaemon(true);
            return t;
        });
        this.log = log;
    }

    /** Applies pending migrations from {@code classpath:db/migration} using the plugin classloader. */
    public void migrate() {
        var flyway = Flyway.configure(HikariDatabase.class.getClassLoader())
                .dataSource(pool)
                .locations("classpath:db/migration")
                // LuckPerms and CoreProtect share the database and enable first, so a fresh schema
                // already holds their tables. Baseline at 0: every dated migration of ours still runs.
                .baselineOnMigrate(true)
                .baselineVersion("0")
                .load();
        var result = flyway.migrate();
        var current = flyway.info().current();
        log.info("Database schema at version " + (current == null ? "<empty>" : current.getVersion())
                + " (" + result.migrationsExecuted + " migration(s) applied)");
    }

    public DataSource dataSource() {
        return pool;
    }

    @Override
    public <R> CompletableFuture<R> query(SqlFunction<R> work) {
        return CompletableFuture.supplyAsync(() -> {
            try (var connection = pool.getConnection()) {
                return work.apply(connection);
            } catch (SQLException e) {
                throw new DatabaseException(e);
            }
        }, executor);
    }

    @Override
    public CompletableFuture<Void> execute(SqlAction work) {
        return query(connection -> {
            work.run(connection);
            return null;
        });
    }

    @Override
    public CompletableFuture<Void> executeInOrder(Object key, SqlAction work) {
        // Each key keeps the future of its last submitted write; the next one starts only when that
        // finishes, whether it succeeded or not. The entry is dropped once its tail completes.
        CompletableFuture<Void> next = tails.compute(key, (k, tail) -> (tail == null
                ? CompletableFuture.<Void>completedFuture(null)
                : tail.handle((ignored, failure) -> (Void) null))
                .thenCompose(ignored -> execute(work)));
        next.whenComplete((ignored, failure) -> tails.remove(key, next));
        return next;
    }

    @Override
    public <R> R queryDuringBoot(SqlFunction<R> work) {
        try (var connection = pool.getConnection()) {
            return work.apply(connection);
        } catch (SQLException e) {
            throw new DatabaseException(e);
        }
    }

    @Override
    public void close() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                log.warning("Database executor did not drain within 10s; pending writes may be lost");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        pool.close();
    }

    /** Unchecked wrapper so {@link SQLException}s propagate through futures. */
    public static final class DatabaseException extends RuntimeException {
        public DatabaseException(SQLException cause) {
            super(cause.getMessage(), cause);
        }
    }
}
