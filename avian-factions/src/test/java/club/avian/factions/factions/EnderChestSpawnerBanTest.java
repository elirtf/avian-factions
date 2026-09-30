package club.avian.factions.factions;

import org.bukkit.Material;
import org.bukkit.block.ShulkerBox;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Spawners never stay in an ender chest, not even inside a shulker box. */
class EnderChestSpawnerBanTest {

    ServerMock server;
    PlayerMock player;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        server.addSimpleWorld("world");
        player = server.addPlayer("Wigby");
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    private static ItemStack shulkerWith(ItemStack inside) {
        var box = new ItemStack(Material.PURPLE_SHULKER_BOX);
        box.editMeta(BlockStateMeta.class, meta -> {
            var state = (ShulkerBox) meta.getBlockState();
            state.getInventory().addItem(inside);
            meta.setBlockState(state);
        });
        return box;
    }

    @Test
    void spawnersAndShulkersHoldingThemCount() {
        assertTrue(EnderChestSpawnerBan.carriesSpawner(new ItemStack(Material.SPAWNER, 3)));
        assertTrue(EnderChestSpawnerBan.carriesSpawner(shulkerWith(new ItemStack(Material.SPAWNER))));
        assertFalse(EnderChestSpawnerBan.carriesSpawner(shulkerWith(new ItemStack(Material.DIAMOND))));
        assertFalse(EnderChestSpawnerBan.carriesSpawner(new ItemStack(Material.DIAMOND)));
        assertFalse(EnderChestSpawnerBan.carriesSpawner(null));
    }

    @Test
    void theSweepGivesSpawnersBackAndLeavesTheRest() {
        var chest = player.getEnderChest();
        chest.setItem(0, new ItemStack(Material.SPAWNER, 2));
        chest.setItem(1, new ItemStack(Material.DIAMOND, 5));

        assertEquals(1, EnderChestSpawnerBan.sweep(chest, player));

        assertNull(chest.getItem(0));
        assertEquals(Material.DIAMOND, chest.getItem(1).getType());
        assertTrue(player.getInventory().contains(Material.SPAWNER, 2));
    }
}
