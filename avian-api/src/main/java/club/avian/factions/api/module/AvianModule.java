package club.avian.factions.api.module;

import club.avian.factions.api.config.ConfigSpec;

import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

/**
 * One gameplay or infrastructure unit shaded into the single Avian jar (ADR-0001, ADR-0004).
 *
 * <p>Modules are constructed by {@code AvianFactionsPlugin} in an explicit list; that list is the
 * boot order. A module may only {@link Services#require} services from modules it names in
 * {@link #dependsOn()}, and those must appear earlier in the list.
 */
public interface AvianModule {

    /** Stable lower-case identifier: {@code "core"}, {@code "factions"}, … Used in logs and reload. */
    String id();

    /** Modules whose services this module may require. Enforced at boot. */
    default Set<Class<? extends AvianModule>> dependsOn() {
        return Set.of();
    }

    /** Config files this module owns; loaded and validated by core before any module enables (ADR-0003). */
    default List<ConfigSpec<?>> configs() {
        return List.of();
    }

    /**
     * Runs in the plugin's {@code onLoad}, before any plugin enables and before configs load.
     * Only for registrations another plugin closes at its own enable (FactionsUUID's upgrade
     * registry, ADR-0007). Everything else belongs in {@link #enable}. Throwing disables the plugin.
     */
    default void load(Logger logger) {
    }

    /**
     * Enable the module. Hold whatever you take from {@code ctx} in fields here; never reach for
     * the registry later. Throwing disables the whole plugin.
     */
    void enable(ModuleContext ctx);

    /** Disable in reverse boot order. Must not throw; failures are logged and the shutdown continues. */
    default void disable() {
    }
}
