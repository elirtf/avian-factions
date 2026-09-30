package club.avian.factions.factions;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Mining ancient debris is announced; placed debris and a vein's second block are not. */
class NetheriteAnnouncerTest {

    ServerMock server;
    World world;
    PlayerMock miner;
    PlayerMock listener;
    NetheriteAnnouncer announcer;
    long now = 1_000;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        world = server.addSimpleWorld("world");
        miner = server.addPlayer("Wigby");
        listener = server.addPlayer("sqw");
        announcer = new NetheriteAnnouncer(new BasePowerListenerTest.Handle(), () -> now);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    private void mine(int x, int y, int z) {
        var block = world.getBlockAt(x, y, z);
        block.setType(Material.ANCIENT_DEBRIS);
        announcer.onBreak(new BlockBreakEvent(block, miner));
    }

    /** The next chat line the other player saw, colour codes removed (the gradient splits every letter). */
    private String nextMessage() {
        String message = listener.nextMessage();
        return message == null ? null : message.replaceAll("§.", "");
    }

    @Test
    void aFindIsAnnouncedToEveryone() {
        mine(1, 10, 1);
        var message = nextMessage();
        assertNotNull(message);
        assertTrue(message.contains("Wigby") && message.contains("Ancient Debris"), message);
    }

    @Test
    void aVeinIsOneMessage() {
        mine(1, 10, 1);
        nextMessage();
        mine(2, 10, 1);
        assertNull(nextMessage(), "within the cooldown");
        now += 61_000;
        mine(3, 10, 1);
        assertNotNull(nextMessage(), "after the cooldown");
    }

    @Test
    void placedDebrisIsNotAFind() {
        var block = world.getBlockAt(1, 10, 1);
        block.setType(Material.ANCIENT_DEBRIS);
        announcer.onPlace(new BlockPlaceEvent(block, block.getState(), block.getRelative(0, -1, 0),
                new ItemStack(Material.ANCIENT_DEBRIS), miner, true, EquipmentSlot.HAND));
        announcer.onBreak(new BlockBreakEvent(block, miner));
        assertNull(nextMessage());

        mine(1, 10, 1);   // the same spot, now natural again (the mark was cleared)
        assertNotNull(nextMessage());
    }
}
