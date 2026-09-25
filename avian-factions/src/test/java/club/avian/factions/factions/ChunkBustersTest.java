package club.avian.factions.factions;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Chunk busters: only in your own land, only after confirming, and they keep what matters. */
class ChunkBustersTest {

    ServerMock server;
    World world;
    PlayerMock player;
    ChunkBusters busters;
    boolean owns = true;
    long now = 1_000;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        world = server.addSimpleWorld("world");
        player = server.addPlayer("Wigby");
        busters = new ChunkBusters(MockBukkit.createMockPlugin(), new BasePowerListenerTest.Handle(),
                (p, chunk) -> owns, () -> now, (chunk, hold) -> { });
        player.getInventory().setItemInMainHand(ChunkBusters.create(2));
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    private BlockPlaceEvent place(int x, int y, int z) {
        var block = world.getBlockAt(x, y, z);
        var event = new BlockPlaceEvent(block, block.getState(), block.getRelative(0, -1, 0),
                player.getInventory().getItemInMainHand(), player, true, EquipmentSlot.HAND);
        busters.onPlace(event);
        return event;
    }

    private void runUntilDone() {
        for (int i = 0; i < 400; i++) {
            server.getScheduler().performOneTick();
        }
    }

    @Test
    void itClearsDownButKeepsBedrockSpawnersAndChests() {
        world.getBlockAt(3, 10, 3).setType(Material.STONE);
        world.getBlockAt(4, 10, 4).setType(Material.SPAWNER);
        world.getBlockAt(5, 10, 5).setType(Material.CHEST);
        world.getBlockAt(6, world.getMinHeight() + 1, 6).setType(Material.BEDROCK);
        world.getBlockAt(7, 70, 7).setType(Material.STONE);   // above the buster: untouched

        assertTrue(place(8, 64, 8).isCancelled(), "the buster is never placed as a block");
        busters.confirm(player);
        runUntilDone();

        assertEquals(Material.AIR, world.getBlockAt(3, 10, 3).getType());
        assertEquals(Material.SPAWNER, world.getBlockAt(4, 10, 4).getType());
        assertEquals(Material.CHEST, world.getBlockAt(5, 10, 5).getType());
        assertEquals(Material.BEDROCK, world.getBlockAt(6, world.getMinHeight() + 1, 6).getType());
        assertEquals(Material.STONE, world.getBlockAt(7, 70, 7).getType());
        assertEquals(1, player.getInventory().getItemInMainHand().getAmount(), "one buster used");
    }

    @Test
    void nothingHappensWithoutTheConfirm() {
        world.getBlockAt(3, 10, 3).setType(Material.STONE);
        place(8, 64, 8);
        runUntilDone();
        assertEquals(Material.STONE, world.getBlockAt(3, 10, 3).getType());
        assertEquals(2, player.getInventory().getItemInMainHand().getAmount());
    }

    @Test
    void notInSomeoneElsesLand() {
        owns = false;
        world.getBlockAt(3, 10, 3).setType(Material.STONE);
        place(8, 64, 8);
        busters.confirm(player);
        runUntilDone();
        assertEquals(Material.STONE, world.getBlockAt(3, 10, 3).getType());
        assertEquals(2, player.getInventory().getItemInMainHand().getAmount(), "nothing taken");
    }

    @Test
    void theConfirmExpires() {
        world.getBlockAt(3, 10, 3).setType(Material.STONE);
        place(8, 64, 8);
        now += 16_000;   // past the 15 s window
        busters.confirm(player);
        runUntilDone();
        assertEquals(Material.STONE, world.getBlockAt(3, 10, 3).getType());
    }

    private World nether() {
        var nether = new org.mockbukkit.mockbukkit.world.WorldMock(Material.NETHERRACK, 0, 256, 0);
        nether.setName("world_nether");
        nether.setEnvironment(World.Environment.NETHER);
        server.addWorld(nether);
        return nether;
    }

    private BlockPlaceEvent placeIn(World w, int x, int y, int z) {
        var block = w.getBlockAt(x, y, z);
        var event = new BlockPlaceEvent(block, block.getState(), block.getRelative(0, -1, 0),
                player.getInventory().getItemInMainHand(), player, true, EquipmentSlot.HAND);
        busters.onPlace(event);
        return event;
    }

    @Test
    void notOnOrAboveTheNetherRoof() {
        var nether = nether();
        int roof = nether.getLogicalHeight() - 1;
        placeIn(nether, 8, roof, 8);
        busters.confirm(player);
        runUntilDone();
        assertEquals(2, player.getInventory().getItemInMainHand().getAmount(), "refused before anything is taken");
    }

    @Test
    void inTheNetherItStopsBelowTheRoofAndSealsLavaAtTheEdges() {
        var nether = nether();
        int roof = nether.getLogicalHeight() - 1;
        nether.getBlockAt(5, roof, 5).setType(Material.BEDROCK);
        nether.getBlockAt(5, 20, 5).setType(Material.LAVA);          // lava inside the chunk
        nether.getBlockAt(16, 20, 7).setType(Material.LAVA);         // the next chunk's lava ocean
        nether.getChunkAt(1, 0).load();
        nether.getBlockAt(15, 20, 7).setType(Material.NETHERRACK);   // the edge block, next to that lava
        nether.getBlockAt(8, 20, 8).setType(Material.NETHERRACK);    // an inside block

        busters.bust(nether.getChunkAt(0, 0), 500, () -> { });   // asked for far above the roof
        runUntilDone();

        assertEquals(nether.getLogicalHeight() - 2, ChunkBusters.ceiling(nether));
        assertEquals(Material.BEDROCK, nether.getBlockAt(5, roof, 5).getType(), "the roof stays");
        assertEquals(Material.AIR, nether.getBlockAt(5, 20, 5).getType(), "lava inside is removed");
        assertEquals(Material.NETHERRACK, nether.getBlockAt(15, 20, 7).getType(), "the edge by outside lava is sealed");
        assertEquals(Material.AIR, nether.getBlockAt(8, 20, 8).getType());
    }

    @Test
    void notInTheEnd() {
        var end = new org.mockbukkit.mockbukkit.world.WorldMock(Material.END_STONE, 0, 256, 0);
        end.setName("world_the_end");
        end.setEnvironment(World.Environment.THE_END);
        server.addWorld(end);
        placeIn(end, 8, 64, 8);
        busters.confirm(player);
        assertEquals(2, player.getInventory().getItemInMainHand().getAmount());
    }

    @Test
    void anOrdinaryEndPortalFrameIsJustABlock() {
        player.getInventory().setItemInMainHand(new ItemStack(Material.END_PORTAL_FRAME));
        assertFalse(place(8, 64, 8).isCancelled());
    }
}
