package club.avian.factions.core.config;

import club.avian.factions.api.config.ConfigErrors;
import club.avian.factions.api.config.ConfigHandle;
import club.avian.factions.api.config.ConfigSpec;
import club.avian.factions.api.module.AvianModule;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.hocon.HoconConfigurationLoader;
import org.spongepowered.configurate.serialize.SerializationException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Logger;

/**
 * Loads every module's {@link ConfigSpec} from HOCON files in the data folder (ADR-0003).
 *
 * <p>Per file: read (or create with commented defaults) → apply env overrides → backfill missing
 * keys with a warning → warn on unknown keys → map to the config class ({@code @Required}
 * errors surface here) → run the spec's validator → save the merged file. Errors from all files
 * are aggregated into one {@link ConfigLoadException}.
 */
public final class ConfigService {

    private static final String VERSION_KEY = "config-version";

    private final Path dir;
    private final Logger log;
    private final Function<String, String> env;
    private final Map<ConfigSpec<?>, Handle<?>> handles = new HashMap<>();

    public ConfigService(Path dir, Logger log) {
        this(dir, log, System::getenv);
    }

    ConfigService(Path dir, Logger log, Function<String, String> env) {
        this.dir = dir;
        this.log = log;
        this.env = env;
    }

    /** Loads every spec of every module. Throws with all errors if any file is invalid. */
    public void loadAll(List<AvianModule> modules) {
        List<ConfigError> errors = new ArrayList<>();
        for (var module : modules) {
            for (var spec : module.configs()) {
                loadInto(spec, errors);
            }
        }
        if (!errors.isEmpty()) {
            throw new ConfigLoadException(errors);
        }
    }

    /** The handle for a spec loaded by {@link #loadAll}. */
    public <T> ConfigHandle<T> handle(ConfigSpec<T> spec) {
        @SuppressWarnings("unchecked")
        var handle = (Handle<T>) handles.get(spec);
        if (handle == null) {
            throw new IllegalStateException(spec.fileName() + " was not declared in any module's configs()");
        }
        return handle;
    }

    private <T> void loadInto(ConfigSpec<T> spec, List<ConfigError> errors) {
        var before = errors.size();
        T value = load(spec, errors);
        if (errors.size() == before && value != null) {
            @SuppressWarnings("unchecked")
            var handle = (Handle<T>) handles.computeIfAbsent(spec, s -> new Handle<T>());
            handle.value = value;
        }
    }

    private <T> T load(ConfigSpec<T> spec, List<ConfigError> errors) {
        var file = spec.fileName();
        var path = dir.resolve(file);
        var loader = HoconConfigurationLoader.builder().path(path).build();
        try {
            Files.createDirectories(dir);
            CommentedConfigurationNode defaults = loader.createNode();
            defaults.set(spec.type(), spec.type().getDeclaredConstructor().newInstance());
            defaults.node(VERSION_KEY).set(spec.version())
                    .comment("Bump only via a registered migration; never edit by hand.");

            boolean fresh = !Files.exists(path);
            CommentedConfigurationNode root = fresh ? loader.createNode() : loader.load();
            if (fresh) {
                root.from(defaults);
                loader.save(root);   // even if mapping fails below, the admin gets a file to edit
                log.info(file + ": not found, wrote defaults");
            } else {
                checkVersion(spec, root, errors);
                backfill(file, root, defaults, "", spec.freeForm());
                warnUnknown(file, root, defaults, "", spec.freeForm());
            }
            applyEnvOverrides(spec, root);

            T value = root.get(spec.type());
            if (value == null) {
                errors.add(new ConfigError(file, "", "could not be mapped to " + spec.type().getSimpleName()));
                return null;
            }
            var before = errors.size();
            spec.validator().accept(value, (p, m) -> errors.add(new ConfigError(file, p, m)));
            if (errors.size() > before) {
                return null;
            }
            // Persist backfilled keys (with their comments) and the version; env overrides are
            // applied in memory only, so secrets never land on disk.
            removeEnvOverrides(spec, root, defaults);
            loader.save(root);
            return value;
        } catch (SerializationException e) {
            errors.add(new ConfigError(file, dotted(e.path().array()), rawMessage(e)));
        } catch (ConfigurateException e) {
            errors.add(new ConfigError(file, "", "cannot be parsed: " + rootMessage(e)));
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(spec.type().getName() + " must have a no-arg constructor", e);
        } catch (java.io.IOException e) {
            errors.add(new ConfigError(file, "", "cannot be read or written: " + e.getMessage()));
        }
        return null;
    }

    private <T> void checkVersion(ConfigSpec<T> spec, ConfigurationNode root, List<ConfigError> errors) throws SerializationException {
        var node = root.node(VERSION_KEY);
        int found = node.virtual() ? spec.version() : node.getInt();
        if (found > spec.version()) {
            errors.add(new ConfigError(spec.fileName(), VERSION_KEY,
                    "is " + found + " but this build understands up to " + spec.version()));
        } else if (found < spec.version()) {
            // Versioned transformations (ADR-0003 rule 8) plug in here once the first rename ships.
            log.warning(spec.fileName() + ": " + VERSION_KEY + " " + found + " < " + spec.version()
                    + "; no migration registered, keys are backfilled as-is");
        }
        node.set(spec.version());
    }

    private void backfill(String file, ConfigurationNode target, ConfigurationNode defaults, String prefix,
                          java.util.Set<String> freeForm) {
        for (var entry : defaults.childrenMap().entrySet()) {
            var key = String.valueOf(entry.getKey());
            var path = prefix.isEmpty() ? key : prefix + "." + key;
            var def = entry.getValue();
            var node = target.node(entry.getKey());
            if (node.virtual()) {
                node.from(def);
                log.warning(file + ": added missing key " + path + " = " + render(def));
            } else if (def.isMap() && node.isMap() && !freeForm.contains(path)) {
                backfill(file, node, def, path, freeForm);
            }
        }
    }

    private void warnUnknown(String file, ConfigurationNode node, ConfigurationNode defaults, String prefix,
                             java.util.Set<String> freeForm) {
        for (var entry : node.childrenMap().entrySet()) {
            var key = String.valueOf(entry.getKey());
            var path = prefix.isEmpty() ? key : prefix + "." + key;
            var def = defaults.node(entry.getKey());
            if (def.virtual()) {
                log.warning(file + ": unknown key " + path + " (ignored)");
            } else if (def.isMap() && entry.getValue().isMap() && !freeForm.contains(path)) {
                warnUnknown(file, entry.getValue(), def, path, freeForm);
            }
        }
    }

    private void applyEnvOverrides(ConfigSpec<?> spec, ConfigurationNode root) throws SerializationException {
        for (var e : spec.envOverrides().entrySet()) {
            var value = env.apply(e.getKey());
            if (value != null && !value.isEmpty()) {
                root.node((Object[]) e.getValue().split("\\.")).set(value);
            }
        }
    }

    private void removeEnvOverrides(ConfigSpec<?> spec, ConfigurationNode root, ConfigurationNode defaults) {
        for (var e : spec.envOverrides().entrySet()) {
            if (env.apply(e.getKey()) != null) {
                Object[] path = e.getValue().split("\\.");
                root.node(path).from(defaults.node(path));
            }
        }
    }

    private static String render(ConfigurationNode node) {
        return node.isMap() ? "{…}" : String.valueOf(node.raw());
    }

    private static String dotted(Object[] path) {
        var sb = new StringBuilder();
        for (var p : path) {
            if (!sb.isEmpty()) {
                sb.append('.');
            }
            sb.append(p);
        }
        return sb.toString();
    }

    private static String rawMessage(SerializationException e) {
        var raw = e.rawMessage();
        return raw == null ? rootMessage(e) : raw;
    }

    private static String rootMessage(Throwable t) {
        var cause = t;
        while (cause.getCause() != null && cause.getCause() != cause) {
            cause = cause.getCause();
        }
        var msg = cause.getMessage();
        return msg == null ? cause.getClass().getSimpleName() : msg.lines().findFirst().orElse(msg);
    }

    private static final class Handle<T> implements ConfigHandle<T> {
        private volatile T value;
        private final List<Consumer<T>> callbacks = new ArrayList<>();

        @Override
        public T get() {
            return value;
        }

        @Override
        public void onReload(Consumer<T> callback) {
            callbacks.add(callback);
        }
    }
}
