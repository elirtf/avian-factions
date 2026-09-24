package club.avian.factions.combat;

import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.persistence.PersistentDataType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** A spawner mob killed by a player loses its body a tick later; anything else keeps it. */
class SpawnerCorpseListenerTest {

    ServerMock server;
    SpawnerCorpseListener listener;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        var plugin = MockBukkit.createMockPlugin();
        listener = new SpawnerCorpseListener(plugin, new HitDelayTest.Handle());
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    private LivingEntity blaze(boolean fromSpawner) {
        var world = server.addSimpleWorld("world");
        var blaze = (LivingEntity) world.spawnEntity(world.getSpawnLocation(), EntityType.BLAZE);
        if (fromSpawner) {
            blaze.getPersistentDataContainer().set(SpawnerCorpseListener.SPAWNER_SPAWNED, PersistentDataType.BYTE, (byte) 1);
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
    void aSpawnerMobKilledByAPlayerIsRemovedAtOnce() {
        var blaze = blaze(true);
        kill(blaze, true);
        assertFalse(blaze.isValid(), "its body would have blocked the next hit on the stack");
    }

    @Test
    void aNaturalMobKeepsItsDeathAnimation() {
        var blaze = blaze(false);
        kill(blaze, true);
        assertTrue(blaze.isValid());
    }

    @Test
    void aSpawnerMobThatDiedWithoutAPlayerIsLeftAlone() {
        var blaze = blaze(true);
        kill(blaze, false);
        assertTrue(blaze.isValid());
    }
}
