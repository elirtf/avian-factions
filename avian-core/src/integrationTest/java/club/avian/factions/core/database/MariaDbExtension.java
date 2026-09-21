package club.avian.factions.core.database;

import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.testcontainers.mariadb.MariaDBContainer;

import java.util.logging.Logger;

/**
 * ADR-0005 fixture: one MariaDB container per JVM (same image as docker-compose.yml), migrated
 * once, shared by every repository test. The database is closed with the JVM.
 */
public final class MariaDbExtension implements BeforeAllCallback {

    private static MariaDBContainer container;
    private static HikariDatabase database;

    @Override
    public void beforeAll(ExtensionContext context) {
        if (database == null) {
            container = new MariaDBContainer(System.getProperty("avian.mariadb.image", "mariadb:11.8.9"));
            container.start();
            database = new HikariDatabase(container.getJdbcUrl(), container.getUsername(), container.getPassword(),
                    4, () -> false, Logger.getLogger("integrationTest"));
            database.migrate();
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                database.close();
                container.stop();
            }));
        }
    }

    public static HikariDatabase database() {
        return database;
    }

    public static MariaDBContainer container() {
        return container;
    }
}
