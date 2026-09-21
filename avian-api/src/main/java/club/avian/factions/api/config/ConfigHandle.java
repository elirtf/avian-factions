package club.avian.factions.api.config;

import java.util.function.Consumer;

/**
 * How a module reads its configuration: {@link #get()} on every use, never cached in a field.
 * Anything derived from config is rebuilt in an {@link #onReload} callback (main thread).
 */
public interface ConfigHandle<T> {

    T get();

    void onReload(Consumer<T> callback);
}
