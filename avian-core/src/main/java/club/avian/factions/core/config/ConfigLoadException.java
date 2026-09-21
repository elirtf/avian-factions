package club.avian.factions.core.config;

import java.util.List;

/** Every error across every file, aggregated; the plugin refuses to start (ADR-0003 rule 5). */
public final class ConfigLoadException extends RuntimeException {

    private final List<ConfigError> errors;

    public ConfigLoadException(List<ConfigError> errors) {
        super(errors.size() + " configuration error(s)");
        this.errors = List.copyOf(errors);
    }

    public List<ConfigError> errors() {
        return errors;
    }
}
