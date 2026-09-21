package club.avian.factions.api.module;

import java.util.Set;

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

    /**
     * Enable the module. Hold whatever you take from {@code ctx} in fields here; never reach for
     * the registry later. Throwing disables the whole plugin.
     */
    void enable(ModuleContext ctx);

    /** Disable in reverse boot order. Must not throw; failures are logged and the shutdown continues. */
    default void disable() {
    }
}
