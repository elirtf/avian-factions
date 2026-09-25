package club.avian.factions.core.world;

import org.bukkit.Material;
import org.bukkit.World;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.world.WorldMock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Old bumpy bedrock becomes one flat layer; everything else is untouched. */
class BedrockFlattenerTest {

    ServerMock server;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void overworldBedrockIsFlattenedToTheBottomLayer() {
        var world = server.addSimpleWorld("world");
        int bottom = world.getMinHeight();
        world.getBlockAt(1, bottom + 2, 1).setType(Material.BEDROCK);   // a bump
        world.getBlockAt(2, bottom + 4, 2).setType(Material.BEDROCK);   // the highest a bump goes
        world.getBlockAt(3, bottom + 1, 3).setType(Material.DIAMOND_ORE);
        world.getBlockAt(4, bottom + 8, 4).setType(Material.BEDROCK);   // too high to be noise: a build

        int changed = BedrockFlattener.flatten(world.getChunkAt(0, 0));

        assertEquals(2, changed);
        assertEquals(Material.DEEPSLATE, world.getBlockAt(1, bottom + 2, 1).getType());
        assertEquals(Material.DEEPSLATE, world.getBlockAt(2, bottom + 4, 2).getType());
        assertEquals(Material.DIAMOND_ORE, world.getBlockAt(3, bottom + 1, 3).getType(), "ores stay");
        assertEquals(Material.BEDROCK, world.getBlockAt(4, bottom + 8, 4).getType(), "placed bedrock above the noise stays");
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                assertEquals(Material.BEDROCK, world.getBlockAt(x, bottom, z).getType(), "a full bottom layer");
            }
        }
        assertTrue(world.getChunkAt(0, 0).getPersistentDataContainer().has(BedrockFlattener.DONE), "marked done");
    }

    @Test
    void netherFloorAndRoofAreBothFlattened() {
        var nether = new WorldMock(Material.NETHERRACK, 0, 256, 0);
        nether.setName("world_nether");
        nether.setEnvironment(World.Environment.NETHER);
        server.addWorld(nether);
        int top = nether.getLogicalHeight() - 1;
        nether.getBlockAt(1, 2, 1).setType(Material.BEDROCK);
        nether.getBlockAt(1, top - 3, 1).setType(Material.BEDROCK);

        BedrockFlattener.flatten(nether.getChunkAt(0, 0));

        assertEquals(Material.NETHERRACK, nether.getBlockAt(1, 2, 1).getType());
        assertEquals(Material.NETHERRACK, nether.getBlockAt(1, top - 3, 1).getType());
        assertEquals(Material.BEDROCK, nether.getBlockAt(1, 0, 1).getType());
        assertEquals(Material.BEDROCK, nether.getBlockAt(1, top, 1).getType());
    }
}
