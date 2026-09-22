package club.avian.factions.core.database;

import club.avian.factions.api.database.Database;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.flywaydb.core.Flyway;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import java.util.logging.Logger;

/**
 * HikariCP pool + a bounded database executor + Flyway migrations (ADR-0002).
 *
 * <p>The main-thread guard is a {@link BooleanSupplier} so the class is testable without Bukkit;
 * the plugin passes {@code Bukkit::isPrimaryThread}.
 */
public final class HikariDatabase implements Database, AutoCloseable {

    private final HikariDataSource pool;
    private final ExecutorService executor;
    private final BooleanSupplier isMainThread;
    private final Logger log;

    public HikariDatabase(String jdbcUrl, String user, String password, int poolSize,
                          BooleanSupplier isMainThread, Logger log) {
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
        this.isMainThread = isMainThread;
        this.log = log;
    }

    /** Applies pending migrations from {@code classpath:db/migration} using the plugin classloader. */
    public void migrate() {
        var flyway = Flyway.configure(HikariDatabase.class.getClassLoader())
                .dataSource(pool)
                .locations("classpath:db/migration")
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
        guard();
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
    public <R> R queryDuringBoot(SqlFunction<R> work) {
        try (var connection = pool.getConnection()) {
            return work.apply(connection);
        } catch (SQLException e) {
            throw new DatabaseException(e);
        }
    }

    private void guard() {
        if (isMainThread.getAsBoolean()) {
            throw new IllegalStateException("Database access from the main thread (spec §65); "
                    + "call from an async context and hop back with the scheduler");
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
