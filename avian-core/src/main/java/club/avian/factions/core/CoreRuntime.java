package club.avian.factions.core;

import club.avian.factions.api.database.Database;
import club.avian.factions.api.player.Players;
import club.avian.factions.core.config.ConfigService;

/**
 * Core's services as seen by the plugin's {@code ModuleContext}. Created by the plugin, filled in
 * by {@link CoreModule#enable}; other modules boot after core, so they always see populated fields.
 */
public final class CoreRuntime {

    private final ConfigService configs;
    private Database database;
    private Players players;

    public CoreRuntime(ConfigService configs) {
        this.configs = configs;
    }

    public ConfigService configs() {
        return configs;
    }

    public Database database() {
        return require(database, "database");
    }

    public Players players() {
        return require(players, "players");
    }

    void database(Database database) {
        this.database = database;
    }

    void players(Players players) {
        this.players = players;
    }

    private static <T> T require(T value, String name) {
        if (value == null) {
            throw new IllegalStateException("core." + name + " is not available before CoreModule has enabled");
        }
        return value;
    }
}
