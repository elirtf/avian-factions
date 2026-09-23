package club.avian.factions.factions;

import club.avian.factions.api.config.ConfigHandle;
import dev.kitteh.factions.Faction;
import dev.kitteh.factions.event.FactionCreateEvent;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BasePowerListenerTest {

    @Test
    void newFactionGetsTheConfiguredBasePower() {
        var boosts = new ArrayList<Double>();
        new BasePowerListener(new Handle())
                .onCreate(new FactionCreateEvent(null, recordingFaction(boosts), FactionCreateEvent.Reason.COMMAND));

        assertEquals(List.of(5.0), boosts);
    }

    /** A Faction whose only working method is powerBoost(double), which it records. */
    private static Faction recordingFaction(List<Double> boosts) {
        return (Faction) Proxy.newProxyInstance(Faction.class.getClassLoader(), new Class<?>[]{Faction.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("powerBoost") && args != null && args.length == 1) {
                        boosts.add((Double) args[0]);
                        return null;
                    }
                    throw new UnsupportedOperationException(method.getName());
                });
    }

    static final class Handle implements ConfigHandle<FactionsConfig> {
        final FactionsConfig config = new FactionsConfig();
        @Override public FactionsConfig get() { return config; }
        @Override public void onReload(Consumer<FactionsConfig> callback) { }
    }
}
