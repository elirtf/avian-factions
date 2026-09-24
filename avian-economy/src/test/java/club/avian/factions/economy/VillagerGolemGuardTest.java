package club.avian.factions.economy;

import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Villager-summoned iron golems are stopped; spawner golems are not. */
class VillagerGolemGuardTest {

    ServerMock server;
    VillagerGolemGuard guard;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        guard = new VillagerGolemGuard(new VaultEconomyBridgeTest.Handle());
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    private CreatureSpawnEvent spawn(EntityType type, SpawnReason reason) {
        var world = server.addSimpleWorld("world");
        var entity = (LivingEntity) world.spawnEntity(world.getSpawnLocation(), type);
        var event = new CreatureSpawnEvent(entity, reason);
        guard.onSpawn(event);
        return event;
    }

    @Test
    void villagersCannotSummonGolems() {
        assertTrue(spawn(EntityType.IRON_GOLEM, SpawnReason.VILLAGE_DEFENSE).isCancelled(),
                "an iron farm would make iron golem spawners worthless");
    }

    @Test
    void spawnerGolemsStillSpawn() {
        assertFalse(spawn(EntityType.IRON_GOLEM, SpawnReason.SPAWNER).isCancelled());
    }

    @Test
    void otherMobsAreUntouched() {
        assertFalse(spawn(EntityType.ZOMBIE, SpawnReason.VILLAGE_DEFENSE).isCancelled());
    }
}
