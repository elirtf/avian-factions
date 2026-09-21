package club.avian.factions.core.module;

import club.avian.factions.api.module.AvianModule;
import club.avian.factions.api.module.Services;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * The single registry behind every module's {@link Services} view. {@link #viewFor} returns a
 * per-module facade that records who provides what and refuses lookups the caller has not
 * declared in {@link AvianModule#dependsOn()}.
 */
public final class ServiceRegistry {

    private record Entry(Object impl, AvianModule provider) {
    }

    private final Map<Class<?>, Entry> entries = new HashMap<>();

    public Services viewFor(AvianModule caller) {
        return new Services() {
            @Override
            public <T> void provide(Class<T> api, T impl) {
                var previous = entries.putIfAbsent(api, new Entry(impl, caller));
                if (previous != null) {
                    throw new IllegalStateException(api.getName() + " is already provided by module '"
                            + previous.provider().id() + "'; '" + caller.id() + "' may not provide it again");
                }
            }

            @Override
            public <T> T require(Class<T> api) {
                return find(api).orElseThrow(() -> new IllegalStateException(
                        "module '" + caller.id() + "' requires " + api.getName() + " but "
                                + describeMissing(api, caller)));
            }

            @Override
            public <T> Optional<T> find(Class<T> api) {
                var entry = entries.get(api);
                if (entry == null || !mayUse(caller, entry.provider())) {
                    return Optional.empty();
                }
                return Optional.of(api.cast(entry.impl()));
            }
        };
    }

    private static boolean mayUse(AvianModule caller, AvianModule provider) {
        return provider == caller || caller.dependsOn().contains(provider.getClass());
    }

    private String describeMissing(Class<?> api, AvianModule caller) {
        var entry = entries.get(api);
        if (entry == null) {
            return "no module provides it";
        }
        return "it is provided by '" + entry.provider().id() + "', which is not in " + caller.id()
                + ".dependsOn()";
    }
}
