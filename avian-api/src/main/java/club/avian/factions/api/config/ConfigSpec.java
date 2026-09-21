package club.avian.factions.api.config;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * A module's declaration of its one config file (ADR-0003). Core collects every spec before any
 * module enables, loads and validates them all, and hands each module a {@link ConfigHandle}.
 *
 * @param fileName  e.g. {@code "factions.conf"}, flat in the plugin data folder
 * @param type      a {@code @ConfigSerializable} final class with initialised fields
 * @param version   current {@code config-version}; bumped only alongside a migration
 * @param validator invariants; report each failure through the {@link ConfigErrors}
 * @param envOverrides environment variable → dotted path; a set variable replaces the file value
 *                     (secrets never need to be on disk)
 */
public record ConfigSpec<T>(String fileName, Class<T> type, int version, BiConsumer<T, ConfigErrors> validator,
                            Map<String, String> envOverrides) {

    public static <T> Builder<T> of(String fileName, Class<T> type) {
        return new Builder<>(fileName, type);
    }

    public static final class Builder<T> {
        private final String fileName;
        private final Class<T> type;
        private int version = 1;
        private BiConsumer<T, ConfigErrors> validator = (cfg, errors) -> { };
        private final Map<String, String> envOverrides = new LinkedHashMap<>();

        private Builder(String fileName, Class<T> type) {
            this.fileName = fileName;
            this.type = type;
        }

        public Builder<T> version(int version) {
            this.version = version;
            return this;
        }

        /** Lets {@code envVar}, when set, override the value at {@code path} (e.g. {@code database.password}). */
        public Builder<T> envOverride(String envVar, String path) {
            envOverrides.put(envVar, path);
            return this;
        }

        public Builder<T> validate(BiConsumer<T, ConfigErrors> validator) {
            this.validator = validator;
            return this;
        }

        public ConfigSpec<T> build() {
            return new ConfigSpec<>(fileName, type, version, validator, Map.copyOf(envOverrides));
        }
    }
}
