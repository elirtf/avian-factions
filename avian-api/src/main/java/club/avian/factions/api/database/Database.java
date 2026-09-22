package club.avian.factions.api.database;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.CompletableFuture;

/**
 * Pooled database access for repositories (ADR-0002). All work runs on the database executor;
 * calling {@link #query} or {@link #execute} from the main thread is a programming error and
 * throws (spec §65).
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
     * Runs {@code work} on the calling thread, bypassing the main-thread guard.
     *
     * <p>Only legitimate during module enable, when no player can be online and nothing is
     * ticking yet: loading an index the module needs before it will answer any command. Calling
     * it after boot blocks the server for the duration of the query.
     */
    <R> R queryDuringBoot(SqlFunction<R> work);
}
