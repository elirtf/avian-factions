package club.avian.factions.combat;

import club.avian.factions.api.config.ConfigHandle;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;

import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * hit-delay-ticks reaches the player as their maximum no-damage ticks, which Paper halves to decide
 * when the next hit counts. Faster hit registration is the combo feel players asked for.
 */
class HitDelayTest {

    static final class Handle implements ConfigHandle<CombatConfig> {
        final CombatConfig config = new CombatConfig();
        @Override public CombatConfig get() { return config; }
        @Override public void onReload(Consumer<CombatConfig> callback) { }
    }

    ServerMock server;
    Handle config;
    CombatListener listener;
    Player player;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        config = new Handle();
        listener = new CombatListener(config);
        player = server.addPlayer("Wigby");
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    private void setHitDelay(int ticks) throws ReflectiveOperationException {
        var field = CombatConfig.class.getDeclaredField("hitDelayTicks");
        field.setAccessible(true);
        field.setInt(config.config, ticks);
    }

    @Test
    void aConfiguredHitDelayIsAppliedToThePlayer() throws Exception {
        setHitDelay(16);
        listener.applyAttackSpeed(player);
        assertEquals(16, player.getMaximumNoDamageTicks(), "hits land every 8 ticks instead of 10");
    }

    @Test
    void zeroMeansVanilla() throws Exception {
        setHitDelay(0);
        listener.applyAttackSpeed(player);
        assertEquals(CombatListener.VANILLA_HIT_DELAY_TICKS, player.getMaximumNoDamageTicks());
    }

    @Test
    void settingItBackToZeroRestoresVanillaRatherThanKeepingTheOldValue() throws Exception {
        setHitDelay(16);
        listener.applyAttackSpeed(player);
        setHitDelay(0);
        listener.applyAttackSpeed(player);
        assertEquals(20, player.getMaximumNoDamageTicks(),
                "before this fix, the player kept 16 until they relogged");
    }
}
