package club.avian.factions.economy;

import club.avian.factions.api.economy.Currency;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
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

import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Grown sugar cane harvested by hand pays tokens; placed cane never does. */
class SugarCaneTokensTest {

    ServerMock server;
    EconomyService economy;
    SugarCaneTokens listener;
    World world;
    PlayerMock farmer;
    double roll;   // what the "random" roll returns: 0 always wins, 1 never does

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        economy = new EconomyService(new EconomyServiceTest.MemoryRepository(), new VaultEconomyBridgeTest.Handle(), 1);
        listener = new SugarCaneTokens(economy, new VaultEconomyBridgeTest.Handle(), () -> roll, Runnable::run, Logger.getAnonymousLogger());
        world = server.addSimpleWorld("world");
        farmer = server.addPlayer("Farmer");
        farmer.setGameMode(GameMode.SURVIVAL);
        roll = 0;
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    /** A column of cane from y=64 up; the bottom block is placed by the farmer, as in real farms. */
    private Block column(int height) {
        var base = world.getBlockAt(0, 64, 0);
        for (int y = 0; y < height; y++) {
            base.getRelative(0, y, 0).setType(Material.SUGAR_CANE);
        }
        place(base);
        return base;
    }

    private void place(Block block) {
        listener.onPlace(new BlockPlaceEvent(block, block.getState(), block.getRelative(0, -1, 0),
                new ItemStack(Material.SUGAR_CANE), farmer, true, EquipmentSlot.HAND));
    }

    private void breakBlock(Block block) {
        listener.onBreak(new BlockBreakEvent(block, farmer));
        for (var b = block; b.getType() == Material.SUGAR_CANE; b = b.getRelative(0, 1, 0)) {
            b.setType(Material.AIR);   // the break, and the blocks above popping
        }
    }

    private long tokens() {
        return economy.balance(farmer.getUniqueId(), Currency.TOKENS);
    }

    @Test
    void harvestingGrownCanePaysForEveryBlockThatBreaks() {
        var base = column(3);
        breakBlock(base.getRelative(0, 1, 0));   // the usual harvest: the two grown blocks
        assertEquals(2, tokens(), "one roll per grown block, and the roll always wins here");
    }

    @Test
    void placedCaneNeverPays() {
        var block = world.getBlockAt(5, 64, 5);
        block.setType(Material.SUGAR_CANE);
        place(block);
        breakBlock(block);
        assertEquals(0, tokens(), "place-and-break would be an infinite token farm");
    }

    @Test
    void breakingTheBaseStillPaysForTheGrownBlocksAboveIt() {
        var base = column(3);
        breakBlock(base);
        assertEquals(2, tokens(), "the placed base is skipped, the two grown blocks count");
    }

    @Test
    void aMarkerIsForgottenOnceItsBlockIsBroken() {
        var block = world.getBlockAt(5, 64, 5);
        block.setType(Material.SUGAR_CANE);
        place(block);
        breakBlock(block);
        block.setType(Material.SUGAR_CANE);   // later, grown cane occupies the same spot
        breakBlock(block);
        assertEquals(1, tokens());
    }

    @Test
    void theChanceIsRespected() {
        roll = 0.5;   // above the default 2 % chance
        breakBlock(column(3).getRelative(0, 1, 0));
        assertEquals(0, tokens());
    }

    @Test
    void creativeModeEarnsNothing() {
        farmer.setGameMode(GameMode.CREATIVE);
        breakBlock(column(3).getRelative(0, 1, 0));
        assertEquals(0, tokens());
    }
}
