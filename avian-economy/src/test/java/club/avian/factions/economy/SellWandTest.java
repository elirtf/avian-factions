package club.avian.factions.economy;

import club.avian.factions.api.economy.Currency;
import club.avian.factions.api.economy.SellValues;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Container;
import org.bukkit.event.Event;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Sell wands with the default tiers: basic ×1 (100 uses), gilded ×1.1 (500), eternal ×1.25 (∞). */
class SellWandTest {

    /** Wheat at $10 each, nothing else sells. */
    static final SellValues PRICES = material -> material == Material.WHEAT ? 10 : 0;

    ServerMock server;
    EconomyService economy;
    SellWandConfig cfg;
    SellWandListener listener;
    World world;
    PlayerMock player;
    Block chest;
    long now = 1_000_000;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        economy = new EconomyService(new EconomyServiceTest.MemoryRepository(), new VaultEconomyBridgeTest.Handle(), 1);
        cfg = new SellWandConfig();
        listener = new SellWandListener(economy, PRICES, new HarvesterHoeTest.Handle<>(cfg), () -> now,
                Logger.getAnonymousLogger());
        world = server.addSimpleWorld("world");
        player = server.addPlayer("Seller");
        chest = world.getBlockAt(0, 64, 0);
        chest.setType(Material.CHEST);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    private ItemStack hold(String tier) {
        var wand = SellWand.create(tier, cfg.tier(tier));
        player.getInventory().setItemInMainHand(wand);
        return player.getInventory().getItemInMainHand();
    }

    private void fill(ItemStack... items) {
        var inv = ((Container) chest.getState()).getInventory();
        for (var item : items) {
            inv.addItem(item);
        }
    }

    private org.bukkit.inventory.Inventory contents() {
        return ((Container) chest.getState()).getInventory();
    }

    private PlayerInteractEvent click(boolean deniedByProtection) {
        var event = new PlayerInteractEvent(player, Action.RIGHT_CLICK_BLOCK, player.getInventory().getItemInMainHand(),
                chest, BlockFace.UP, EquipmentSlot.HAND);
        if (deniedByProtection) {
            event.setUseInteractedBlock(Event.Result.DENY);
        }
        listener.onUse(event);
        now += 5_000;   // past the cooldown for the next click
        return event;
    }

    private long money() {
        return economy.balance(player.getUniqueId(), Currency.MONEY);
    }

    @Test
    void sellsWhatTheShopBuysAndLeavesTheRest() {
        hold("basic");
        fill(new ItemStack(Material.WHEAT, 64), new ItemStack(Material.WHEAT, 6), new ItemStack(Material.DIRT, 32));

        var event = click(false);

        assertTrue(event.useInteractedBlock() == Event.Result.DENY, "the chest does not open");
        assertEquals(700, money(), "70 wheat at $10");
        assertFalse(contents().contains(Material.WHEAT));
        assertTrue(contents().contains(Material.DIRT, 32), "the shop doesn't buy dirt, so it stays");
    }

    @Test
    void theMultiplierScalesThePayout() {
        hold("eternal");
        fill(new ItemStack(Material.WHEAT, 40));
        click(false);
        assertEquals(500, money(), "40 × $10 × 1.25");
    }

    @Test
    void aContainerTheClickWasDeniedOnIsNotSold() {
        hold("basic");
        fill(new ItemStack(Material.WHEAT, 64));   // e.g. a chest in another faction's claim
        click(true);
        assertEquals(0, money());
        assertTrue(contents().contains(Material.WHEAT, 64));
    }

    @Test
    void sneakingOnlyShowsTheValue() {
        hold("basic");
        fill(new ItemStack(Material.WHEAT, 10));
        player.setSneaking(true);
        click(false);
        assertEquals(0, money());
        assertTrue(contents().contains(Material.WHEAT, 10));
    }

    @Test
    void ourOwnItemsAreNeverSold() {
        hold("basic");
        var marked = new ItemStack(Material.WHEAT, 5);
        marked.editPersistentDataContainer(pdc -> pdc.set(new org.bukkit.NamespacedKey("avian", "test"),
                org.bukkit.persistence.PersistentDataType.BYTE, (byte) 1));
        fill(marked);
        click(false);
        assertEquals(0, money());
    }

    @Test
    void aSaleUsesOneUseAndTheLastOneBreaksTheWand() {
        var wand = hold("basic");
        SellWand.recordSale(wand, 0, cfg.tier("basic"));   // 99 left
        assertEquals(99, SellWand.uses(wand));
        wand.editPersistentDataContainer(pdc -> pdc.set(SellWand.USES,
                org.bukkit.persistence.PersistentDataType.INTEGER, 1));
        player.getInventory().setItemInMainHand(wand);
        fill(new ItemStack(Material.WHEAT, 1));

        click(false);

        assertEquals(10, money());
        assertNull(player.getInventory().getItemInMainHand().getType().isAir() ? null : player.getInventory().getItemInMainHand());
    }

    @Test
    void anEmptySaleCostsNoUse() {
        var wand = hold("basic");
        fill(new ItemStack(Material.DIRT, 5));
        click(false);
        assertEquals(100, SellWand.uses(player.getInventory().getItemInMainHand()));
        assertEquals(100, SellWand.uses(wand));
    }

    @Test
    void anUnlimitedWandNeverRunsOut() {
        var wand = hold("eternal");
        for (int i = 0; i < 3; i++) {
            fill(new ItemStack(Material.WHEAT, 1));
            click(false);
        }
        assertEquals(-1, SellWand.uses(player.getInventory().getItemInMainHand()));
        assertEquals(36, SellWand.earned(player.getInventory().getItemInMainHand()), "3 sales of $12.50, each rounded down");
        assertTrue(SellWand.is(wand));
    }

    @Test
    void clicksInsideTheCooldownDoNothing() {
        hold("basic");
        fill(new ItemStack(Material.WHEAT, 1));
        click(false);
        now -= 5_000 - 200;   // 200 ms after the first sale
        fill(new ItemStack(Material.WHEAT, 1));
        var event = new PlayerInteractEvent(player, Action.RIGHT_CLICK_BLOCK, player.getInventory().getItemInMainHand(),
                chest, BlockFace.UP, EquipmentSlot.HAND);
        listener.onUse(event);
        assertEquals(10, money());
    }

    @Test
    void aPlainStickIsNotAWand() {
        player.getInventory().setItemInMainHand(new ItemStack(Material.STICK));
        fill(new ItemStack(Material.WHEAT, 5));
        var event = click(false);
        assertEquals(0, money());
        assertFalse(event.useInteractedBlock() == Event.Result.DENY, "the chest opens as normal");
    }
}
