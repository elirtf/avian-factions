package club.avian.factions.api.database;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.CompletableFuture;

/**
 * Pooled database access for repositories (ADR-0002). All work runs on the database executor, so
 * {@link #query} and {@link #execute} return immediately and are safe to call from any thread,
 * including the main thread. Never block the main thread on the returned future (spec §65).
 */
public interface Database {

    @FunctionalInterface
    interface SqlFunction<R> {
        R apply(Connection connection) throws SQLException;
    }

    @FunctionalInterface
    interface SqlAction {
        void run(Connection connection) throws SQLException;
    }

    /** Runs {@code work} with a pooled connection on the database executor. */
    <R> CompletableFuture<R> query(SqlFunction<R> work);

    /** Like {@link #query} for work with no result. */
    CompletableFuture<Void> execute(SqlAction work);

    /**
     * Like {@link #execute}, but work sharing {@code key} runs one at a time, in the order it was
     * submitted. Plain {@code execute} work may finish in any order across the executor's threads,
     * so a delete can land before the insert it follows (#27). Use this for write-behind of state
     * that can change twice before the first write lands; key it by what the row is keyed by.
     * A failed write does not hold up the ones behind it.
     */
    CompletableFuture<Void> executeInOrder(Object key, SqlAction work);

    /**
     * Runs {@code work} on the calling thread, blocking it until the query finishes.
     *
     * <p>Only legitimate during module enable, when no player can be online and nothing is
     * ticking yet: loading an index the module needs before it will answer any command. Calling
     * it after boot blocks the server for the duration of the query.
     */
    <R> R queryDuringBoot(SqlFunction<R> work);
}
