package club.avian.factions.core.database;

import org.junit.jupiter.api.Test;
import club.avian.factions.testing.MariaDbExtension;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MariaDbExtension.class)
class HikariDatabaseIT {

    @Test
    void migrationsAppliedAndPlayersTableExists() {
        var exists = MariaDbExtension.database().query(c -> {
            try (var rs = c.getMetaData().getTables(null, null, "players", null)) {
                return rs.next();
            }
        }).join();
        assertTrue(exists, "players table should exist after migrate()");
    }

    @Test
    void migrateIsIdempotent() {
        MariaDbExtension.database().migrate();
        var applied = MariaDbExtension.database().query(c -> {
            try (var st = c.createStatement();
                 var rs = st.executeQuery("SELECT COUNT(*) FROM flyway_schema_history WHERE success = 1")) {
                rs.next();
                return rs.getInt(1);
            }
        }).join();
        assertEquals(1, applied);
    }

    /** Write-behind from the main thread depends on this: the caller only schedules the work. */
    @Test
    void queryRunsOnTheDatabaseExecutorNotTheCaller() {
        var caller = Thread.currentThread();
        var ranOn = MariaDbExtension.database().query(conn -> Thread.currentThread()).join();
        assertNotSame(caller, ranOn);
        assertTrue(ranOn.getName().startsWith("avian-db-"), ranOn.getName());
    }

    /** #27: an insert then a delete of the same row, back to back, must leave no row. */
    @Test
    void writesSharingAKeyLandInSubmissionOrder() {
        var db = MariaDbExtension.database();
        db.execute(c -> {
            try (var st = c.createStatement()) {
                st.execute("CREATE TABLE IF NOT EXISTS ordered_writes (id INT PRIMARY KEY)");
                st.execute("DELETE FROM ordered_writes");
            }
        }).join();
        var futures = new java.util.ArrayList<java.util.concurrent.CompletableFuture<Void>>();
        for (int i = 0; i < 200; i++) {
            int id = i;
            // A slow insert followed by an instant delete: without ordering, the delete often wins.
            futures.add(db.executeInOrder(id, c -> {
                try (var st = c.createStatement()) {
                    st.execute("DO SLEEP(0.005)");
                    st.execute("INSERT INTO ordered_writes VALUES (" + id + ")");
                }
            }));
            futures.add(db.executeInOrder(id, c -> {
                try (var st = c.createStatement()) {
                    st.execute("DELETE FROM ordered_writes WHERE id = " + id);
                }
            }));
        }
        futures.forEach(java.util.concurrent.CompletableFuture::join);
        var left = db.query(c -> {
            try (var st = c.createStatement(); var rs = st.executeQuery("SELECT COUNT(*) FROM ordered_writes")) {
                rs.next();
                return rs.getInt(1);
            }
        }).join();
        assertEquals(0, left);
    }
}
