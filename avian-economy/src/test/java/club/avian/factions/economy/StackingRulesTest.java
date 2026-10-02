package club.avian.factions.economy;

import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.world.WorldMock;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Owner, 2026-10-01: only mobs from placed spawners stack, never villagers, iron golems or silverfish. */
class StackingRulesTest {

    private ServerMock server;
    private WorldMock world;
    private StackingRules rules;
    private final EconomyConfig.Stacking defaults = new EconomyConfig().stacking();

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        world = server.addSimpleWorld("world");
        rules = new StackingRules(MockBukkit.createMockPlugin(), new HarvesterHoeTest.Handle<>(new EconomyConfig()));
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    private LivingEntity spawn(EntityType type, boolean fromPlacedSpawner) {
        var entity = (LivingEntity) world.spawnEntity(world.getSpawnLocation(), type);
        if (fromPlacedSpawner) {
            rules.markFromPlacedSpawner(entity);
        }
        return entity;
    }

    @Test
    void mobsFromPlacedSpawnersStack() {
        assertTrue(rules.mayStack(List.of(spawn(EntityType.ZOMBIE, true), spawn(EntityType.ZOMBIE, true)), defaults));
    }

    @Test
    void anUnmarkedMobKeepsTheStackApart() {
        // A dungeon spawner's zombie (or a wild one) next to a farm's zombie.
        assertFalse(rules.mayStack(List.of(spawn(EntityType.ZOMBIE, true), spawn(EntityType.ZOMBIE, false)), defaults));
    }

    @Test
    void emeraldEconomyMobsNeverStack() {
        for (var type : List.of(EntityType.VILLAGER, EntityType.IRON_GOLEM, EntityType.SILVERFISH)) {
            assertFalse(rules.mayStack(List.of(spawn(type, true), spawn(type, true)), defaults), type + " stacked");
        }
    }

    @Test
    void turningTheSpawnerRuleOffLetsWildMobsStack() throws ReflectiveOperationException {
        var anyMob = new EconomyConfig().stacking();
        var rule = EconomyConfig.Stacking.class.getDeclaredField("onlyPlayerPlacedSpawners");
        rule.setAccessible(true);
        rule.setBoolean(anyMob, false);
        assertTrue(rules.mayStack(List.of(spawn(EntityType.ZOMBIE, false), spawn(EntityType.ZOMBIE, false)), anyMob));
        assertFalse(rules.mayStack(List.of(spawn(EntityType.VILLAGER, false), spawn(EntityType.VILLAGER, false)), anyMob),
                "never-stack still applies");
    }
}
