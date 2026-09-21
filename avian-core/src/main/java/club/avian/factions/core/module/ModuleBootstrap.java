package club.avian.factions.core.module;

import club.avian.factions.api.module.AvianModule;
import club.avian.factions.api.module.ModuleContext;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Boots modules in list order and tears them down in reverse (ADR-0004).
 *
 * <p>Phases: validate the list (unique ids, every {@code dependsOn} earlier in the list) →
 * enable each module → on any exception, disable the already-enabled modules in reverse and
 * rethrow so the plugin disables itself. The config-loading phase from ADR-0003 slots in
 * between validation and enable once the database and Configurate land.
 */
public final class ModuleBootstrap {

    private final List<AvianModule> modules;
    private final Function<AvianModule, ModuleContext> contexts;
    private final Logger log;
    private final Deque<AvianModule> enabled = new ArrayDeque<>();

    public ModuleBootstrap(List<AvianModule> modules, Function<AvianModule, ModuleContext> contexts, Logger log) {
        this.modules = List.copyOf(modules);
        this.contexts = contexts;
        this.log = log;
    }

    /** Validates then enables every module. Throws after cleaning up if any step fails. */
    public void enableAll() {
        validate();
        for (var module : modules) {
            try {
                module.enable(contexts.apply(module));
                enabled.push(module);
                log.info("Enabled module '" + module.id() + "'");
            } catch (RuntimeException e) {
                log.log(Level.SEVERE, "Module '" + module.id() + "' failed to enable; disabling everything", e);
                disableAll();
                throw e;
            }
        }
        log.info("Enabled " + enabled.size() + " modules");
    }

    /** Disables enabled modules in reverse order. Never throws. */
    public void disableAll() {
        while (!enabled.isEmpty()) {
            var module = enabled.pop();
            try {
                module.disable();
                log.info("Disabled module '" + module.id() + "'");
            } catch (RuntimeException e) {
                log.log(Level.SEVERE, "Module '" + module.id() + "' threw during disable; continuing", e);
            }
        }
    }

    public List<AvianModule> modules() {
        return modules;
    }

    private void validate() {
        Set<String> ids = new HashSet<>();
        Set<Class<?>> seen = new HashSet<>();
        for (var module : modules) {
            if (!ids.add(module.id())) {
                throw new IllegalStateException("Duplicate module id '" + module.id() + "'");
            }
            for (var dep : module.dependsOn()) {
                if (!seen.contains(dep)) {
                    throw new IllegalStateException("Module '" + module.id() + "' depends on " + dep.getSimpleName()
                            + ", which is not listed before it in AvianFactionsPlugin");
                }
            }
            seen.add(module.getClass());
        }
    }
}
