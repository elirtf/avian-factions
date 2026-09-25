package club.avian.factions.core;

import club.avian.factions.api.config.ConfigErrors;
import club.avian.factions.api.config.ConfigSpec;
import club.avian.factions.api.config.RequiresRestart;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;
import org.spongepowered.configurate.objectmapping.meta.Required;

/** {@code core.conf}: server identity and the database connection (ADR-0003 reference shape). */
@ConfigSerializable
public final class CoreConfig {

    public static final ConfigSpec<CoreConfig> SPEC = ConfigSpec.of("core.conf", CoreConfig.class)
            .version(1)
            .envOverride("AVIAN_DB_HOST", "database.host")
            .envOverride("AVIAN_DB_PORT", "database.port")
            .envOverride("AVIAN_DB_NAME", "database.name")
            .envOverride("AVIAN_DB_USER", "database.user")
            .envOverride("AVIAN_DB_PASSWORD", "database.password")
            .validate(CoreConfig::validate)
            .build();

    private Server server = new Server();
    private Database database = new Database();

    @Comment("""
            Make old chunks' bedrock flat: the first time a chunk generated before flat bedrock was
            switched on (paper-world-defaults generator.generate-flat-bedrock) loads, bedrock above the
            bottom layer becomes deepslate (Nether: netherrack) and the Nether roof is flattened too.
            Each chunk is done once and marked. The End is left alone.""")
    private boolean flattenOldBedrock = true;

    public Server server() {
        return server;
    }

    public Database database() {
        return database;
    }

    public boolean flattenOldBedrock() {
        return flattenOldBedrock;
    }

    static void validate(CoreConfig cfg, ConfigErrors e) {
        e.check(!cfg.server.name.isBlank(), "server.name", "must not be blank");
        e.check(cfg.database.port > 0 && cfg.database.port <= 65535, "database.port", "must be 1-65535 (got %d)", cfg.database.port);
        e.check(cfg.database.poolSize >= 1 && cfg.database.poolSize <= 64, "database.pool-size", "must be 1-64 (got %d)", cfg.database.poolSize);
        e.check(!cfg.database.name.isBlank(), "database.name", "must not be blank");
        e.check(!cfg.database.user.isBlank(), "database.user", "must not be blank");
    }

    @ConfigSerializable
    public static final class Server {
        @Comment("Display name used in messages and the tab header.")
        private String name = "Avian Factions";

        @Comment("Public address shown to players. Placeholder only; nothing binds to it.")
        private String address = "mc.avian.club";

        public String name() {
            return name;
        }

        public String address() {
            return address;
        }
    }

    @ConfigSerializable
    public static final class Database {
        @RequiresRestart
        @Comment("MariaDB host. Overridable with AVIAN_DB_HOST.")
        private String host = "127.0.0.1";

        @RequiresRestart
        @Comment("Overridable with AVIAN_DB_PORT.")
        private int port = 3306;

        @RequiresRestart
        @Comment("Overridable with AVIAN_DB_NAME.")
        private String name = "avian";

        @RequiresRestart
        @Comment("Overridable with AVIAN_DB_USER.")
        private String user = "avian";

        @Required
        @RequiresRestart
        @Comment("No default on purpose: set it here or via AVIAN_DB_PASSWORD (preferred).")
        private String password;

        @RequiresRestart
        @Comment("Hikari maximum pool size. Also bounds the database executor threads.")
        private int poolSize = 8;

        public String host() {
            return host;
        }

        public int port() {
            return port;
        }

        public String name() {
            return name;
        }

        public String user() {
            return user;
        }

        public String password() {
            return password;
        }

        public int poolSize() {
            return poolSize;
        }

        public String jdbcUrl() {
            return "jdbc:mariadb://" + host + ":" + port + "/" + name;
        }
    }
}
