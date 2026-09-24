package club.avian.factions.combat;

import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDeathEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** A stacked mob killed by a player loses its body a tick later; anything else keeps it. */
class StackedCorpseListenerTest {

    ServerMock server;
    StackedCorpseListener listener;
    java.util.Set<org.bukkit.entity.Entity> stacked = new java.util.HashSet<>();

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        var plugin = MockBukkit.createMockPlugin();
        listener = new StackedCorpseListener(plugin, new HitDelayTest.Handle(), e -> stacked.contains(e));
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    private LivingEntity blaze(boolean inAStack) {
        var world = server.addSimpleWorld("world");
        var blaze = (LivingEntity) world.spawnEntity(world.getSpawnLocation(), EntityType.BLAZE);
        if (inAStack) {
            stacked.add(blaze);
        }
        return blaze;
    }

    private void kill(LivingEntity entity, boolean byPlayer) {
        if (byPlayer) {
            entity.setKiller(server.addPlayer());
        }
        listener.onDeath(new EntityDeathEvent(entity, org.bukkit.damage.DamageSource.builder(
                org.bukkit.damage.DamageType.GENERIC).build(), new ArrayList<>()));
        server.getScheduler().performOneTick();
    }

    @Test
    void aStackedMobKilledByAPlayerIsRemovedAtOnce() {
        var blaze = blaze(true);
        kill(blaze, true);
        assertFalse(blaze.isValid(), "its body would have blocked the next hit on the stack");
    }

    @Test
    void aLoneMobKeepsItsDeathAnimation() {
        var blaze = blaze(false);
        kill(blaze, true);
        assertTrue(blaze.isValid());
    }

    @Test
    void aStackedMobThatDiedWithoutAPlayerIsLeftAlone() {
        var blaze = blaze(true);
        kill(blaze, false);
        assertTrue(blaze.isValid());
    }
}
