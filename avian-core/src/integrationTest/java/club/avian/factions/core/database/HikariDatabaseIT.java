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
}
