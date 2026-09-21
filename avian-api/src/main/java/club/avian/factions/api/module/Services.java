package club.avian.factions.api.module;

import java.util.Optional;

/**
 * Typed cross-module service registry. A provider registers an {@code avian-api} interface in its
 * {@link AvianModule#enable}; consumers require it from their own context.
 */
public interface Services {

    /** Register {@code impl} as the implementation of {@code api}. One provider per api. */
    <T> void provide(Class<T> api, T impl);

    /**
     * The registered implementation of {@code api}.
     *
     * @throws IllegalStateException if nothing provides it, or if the provider is not among the
     *     calling module's {@link AvianModule#dependsOn()}
     */
    <T> T require(Class<T> api);

    /** Soft dependency: empty when nothing provides {@code api} or the caller may not see it. */
    <T> Optional<T> find(Class<T> api);
}
