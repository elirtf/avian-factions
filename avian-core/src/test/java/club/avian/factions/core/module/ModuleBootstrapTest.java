package club.avian.factions.core.module;

import club.avian.factions.api.module.AvianModule;
import club.avian.factions.api.module.ModuleContext;
import club.avian.factions.core.testing.FakeModuleContext;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModuleBootstrapTest {

    interface Greeter {
        String greet();
    }

    private final List<String> events = new ArrayList<>();
    private final ServiceRegistry registry = new ServiceRegistry();

    private ModuleBootstrap bootstrap(AvianModule... modules) {
        return new ModuleBootstrap(List.of(modules), this::context, Logger.getLogger("test"));
    }

    private ModuleContext context(AvianModule module) {
        return new FakeModuleContext(module, registry.viewFor(module));
    }

    final class Provider implements AvianModule {
        @Override public String id() { return "provider"; }
        @Override public void enable(ModuleContext ctx) {
            events.add("enable provider");
            ctx.services().provide(Greeter.class, () -> "hi");
        }
        @Override public void disable() { events.add("disable provider"); }
    }

    final class Consumer implements AvianModule {
        @Override public String id() { return "consumer"; }
        @Override public Set<Class<? extends AvianModule>> dependsOn() { return Set.of(Provider.class); }
        @Override public void enable(ModuleContext ctx) {
            events.add("enable consumer " + ctx.services().require(Greeter.class).greet());
        }
        @Override public void disable() { events.add("disable consumer"); }
    }

    final class Undeclared implements AvianModule {
        @Override public String id() { return "undeclared"; }
        @Override public void enable(ModuleContext ctx) { ctx.services().require(Greeter.class); }
    }

    @Test
    void enablesInOrderAndDisablesInReverse() {
        var b = bootstrap(new Provider(), new Consumer());
        b.enableAll();
        b.disableAll();
        assertEquals(List.of("enable provider", "enable consumer hi", "disable consumer", "disable provider"), events);
    }

    @Test
    void dependencyListedLaterFailsValidationBeforeAnyEnable() {
        var b = bootstrap(new Consumer(), new Provider());
        var e = assertThrows(IllegalStateException.class, b::enableAll);
        assertTrue(e.getMessage().contains("not listed before it"), e.getMessage());
        assertTrue(events.isEmpty());
    }

    @Test
    void requireWithoutDependsOnIsRefusedAndEverythingIsRolledBack() {
        var b = bootstrap(new Provider(), new Undeclared());
        var e = assertThrows(IllegalStateException.class, b::enableAll);
        assertTrue(e.getMessage().contains("not in undeclared.dependsOn()"), e.getMessage());
        assertEquals(List.of("enable provider", "disable provider"), events);
    }
}
