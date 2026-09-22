package club.avian.factions.core.database;

import org.junit.jupiter.api.Test;
import club.avian.factions.testing.MariaDbExtension;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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

    @Test
    void mainThreadAccessIsRefused() {
        var c = MariaDbExtension.container();
        var guarded = new HikariDatabase(c.getJdbcUrl(), c.getUsername(), c.getPassword(), 1,
                () -> true, Logger.getLogger("guard"));
        try {
            var e = assertThrows(IllegalStateException.class, () -> guarded.query(conn -> 1));
            assertTrue(e.getMessage().contains("main thread"), e.getMessage());
        } finally {
            guarded.close();
        }
    }
}
