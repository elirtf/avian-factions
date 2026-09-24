package club.avian.factions.economy;

import club.avian.factions.api.config.ConfigHandle;
import club.avian.factions.api.economy.Currency;
import club.avian.factions.api.economy.SellValues;
import club.avian.factions.economy.HarvesterHoe.Track;
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

import java.util.function.Consumer;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The Harvester Hoe: what it harvests, what it leaves, and what it pays. */
class HarvesterHoeTest {

    static final class Handle<T> implements ConfigHandle<T> {
        final T value;
        Handle(T value) { this.value = value; }
        @Override public T get() { return value; }
        @Override public void onReload(Consumer<T> callback) { }
    }

    /** Sugar cane at $16 each, nothing else. */
    static final SellValues PRICES = material -> material == Material.SUGAR_CANE ? 16 : 0;

    ServerMock server;
    EconomyService economy;
    HoeConfig cfg;
    HoeHarvest harvest;
    World world;
    PlayerMock farmer;
    ItemStack hoe;
    double roll;
    double xp;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        economy = new EconomyService(new EconomyServiceTest.MemoryRepository(), new VaultEconomyBridgeTest.Handle(), 1);
        cfg = new HoeConfig();
        harvest = new HoeHarvest(economy, PRICES, new Handle<>(cfg), new VaultEconomyBridgeTest.Handle(),
                () -> roll, (p, xp) -> this.xp += xp, Logger.getAnonymousLogger());
        // Registered, so the protection probe on neighbouring columns gets its verdict.
        server.getPluginManager().registerEvents(harvest, MockBukkit.createMockPlugin());
        world = server.addSimpleWorld("world");
        farmer = server.addPlayer("Farmer");
        farmer.setGameMode(GameMode.SURVIVAL);
        hoe = HarvesterHoe.create(cfg);
        farmer.getInventory().setItemInMainHand(hoe);
        roll = 1;   // no tokens or drops unless a test wants them
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    /** A cane column at (x, 64, z); its root is placed by a player, as in every real farm. */
    private Block column(int x, int z, int height) {
        var root = world.getBlockAt(x, 64, z);
        for (int y = 0; y < height; y++) {
            root.getRelative(0, y, 0).setType(Material.SUGAR_CANE);
        }
        SugarCaneTokens.mark(root);
        return root;
    }

    /** Upgrades the hoe and puts it back in hand (the hand holds a copy, as on a real server). */
    private void upgrade(Track track, int level) {
        HarvesterHoe.setLevel(hoe, track, level, cfg);
        farmer.getInventory().setItemInMainHand(hoe);
    }

    private BlockBreakEvent swing(Block block) {
        var event = new BlockBreakEvent(block, farmer);
        harvest.onBreak(event);
        return event;
    }

    private long balance(Currency currency) {
        return economy.balance(farmer.getUniqueId(), currency);
    }

    @Test
    void itHarvestsAboveTheRootAndSellsOnTheSpot() {
        var root = column(0, 0, 3);
        var event = swing(root.getRelative(0, 2, 0));
        assertTrue(event.isCancelled(), "the hoe harvests; vanilla must not also break and drop");
        assertEquals(Material.SUGAR_CANE, root.getType(), "the root stays, so nothing needs replanting");
        assertEquals(Material.AIR, root.getRelative(0, 1, 0).getType());
        assertEquals(32, balance(Currency.MONEY), "two cane at $16");
    }

    @Test
    void hittingTheRootStillHarvestsTheColumn() {
        var root = column(0, 0, 3);
        swing(root);
        assertEquals(Material.SUGAR_CANE, root.getType());
        assertEquals(32, balance(Currency.MONEY));
    }

    @Test
    void radiusHarvestsTheColumnsAround() {
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                column(x, z, 3);
            }
        }
        upgrade(Track.RADIUS, 1);
        swing(world.getBlockAt(0, 65, 0));
        assertEquals(9 * 2 * 16, balance(Currency.MONEY), "3x3 columns, two cane each");
    }

    @Test
    void theMoneyMultiplierAppliesToTheSale() {
        upgrade(Track.MONEY_MULTIPLIER, 5);   // +25 %
        swing(column(0, 0, 5));   // four cane: $64
        assertEquals(80, balance(Currency.MONEY));
    }

    @Test
    void grownCaneRollsForTokensButPlacedCaneNeverDoes() {
        var root = column(0, 0, 2);
        var placed = root.getRelative(0, 2, 0);
        placed.setType(Material.SUGAR_CANE);   // a player stacked one on top
        new SugarCaneTokens(economy, new VaultEconomyBridgeTest.Handle(), () -> 1, Logger.getAnonymousLogger())
                .onPlace(new BlockPlaceEvent(placed, placed.getState(), root.getRelative(0, 1, 0),
                        new ItemStack(Material.SUGAR_CANE), farmer, true, EquipmentSlot.HAND));
        roll = 0;   // every roll wins
        swing(root);
        assertEquals(1, balance(Currency.TOKENS), "only the grown block earns a token");
        assertEquals(32, balance(Currency.MONEY), "both still sell");
    }

    @Test
    void randomDropsNeedTheUpgrade() {
        roll = 0;
        swing(column(0, 0, 2));
        assertEquals(1, balance(Currency.TOKENS), "level 0: just the ordinary token roll");
        upgrade(Track.RANDOM_DROPS, 1);
        swing(column(5, 5, 2));
        assertEquals(1 + 1 + 25, balance(Currency.TOKENS), "the default drop: 25 bonus tokens");
    }

    @Test
    void grownCaneGivesFarmingXpAndCultivationAddsToIt() {
        swing(column(0, 0, 3));
        assertEquals(4.0, xp, 1e-9, "two grown blocks at 2 XP");
        upgrade(Track.CULTIVATION, 5);   // +100 %
        swing(column(5, 5, 3));
        assertEquals(4.0 + 8.0, xp, 1e-9);
    }

    @Test
    void withAutoSellOffTheCaneGoesToTheInventory() {
        HarvesterHoe.setAutoSell(hoe, false, cfg);
        farmer.getInventory().setItemInMainHand(hoe);
        swing(column(0, 0, 3));
        assertEquals(0, balance(Currency.MONEY), "nothing sold");
        assertTrue(farmer.getInventory().contains(Material.SUGAR_CANE, 2), "both cane blocks kept");
        assertEquals(4.0, xp, 1e-9, "XP still comes with it");
    }

    @Test
    void autoSellFollowsTheConfigUntilThePlayerChoosesAndIsKeptOnTheItem() {
        assertTrue(HarvesterHoe.autoSell(hoe, cfg), "hoe.conf default is on");
        HarvesterHoe.setAutoSell(hoe, false, cfg);
        assertFalse(HarvesterHoe.autoSell(hoe.clone(), cfg), "the choice travels with the hoe");
    }

    @Test
    void rightClickOpensTheMenuOnlyForTheRealHoe() {
        var menu = new HoeMenu(MockBukkit.createMockPlugin(), economy, new Handle<>(cfg));
        var plain = new ItemStack(Material.NETHERITE_HOE);
        var click = new org.bukkit.event.player.PlayerInteractEvent(farmer, org.bukkit.event.block.Action.RIGHT_CLICK_AIR,
                plain, null, org.bukkit.block.BlockFace.SELF, EquipmentSlot.HAND);
        menu.onRightClick(click);
        assertEquals(org.bukkit.event.Event.Result.DEFAULT, click.useItemInHand(), "a plain netherite hoe still tills");

        var real = new org.bukkit.event.player.PlayerInteractEvent(farmer, org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK,
                hoe, world.getBlockAt(0, 63, 0), org.bukkit.block.BlockFace.UP, EquipmentSlot.HAND);
        menu.onRightClick(real);
        assertEquals(org.bukkit.event.Event.Result.DENY, real.useItemInHand(), "the Harvester Hoe opens /hoe instead of tilling");
        assertEquals(45, farmer.getOpenInventory().getTopInventory().getSize());
        assertEquals(Material.NETHERITE_HOE, hoe.getType());
    }

    @Test
    void anOrdinaryHoeIsJustAHoe() {
        var plain = new ItemStack(Material.DIAMOND_HOE);
        plain.editMeta(meta -> meta.itemName(net.kyori.adventure.text.Component.text("Harvester Hoe")));
        farmer.getInventory().setItemInMainHand(plain);
        var event = swing(column(0, 0, 3).getRelative(0, 1, 0));
        assertFalse(event.isCancelled(), "the name alone must not make it a Harvester Hoe");
        assertEquals(0, balance(Currency.MONEY));
    }

    @Test
    void levelsLiveOnTheItemAndCostsComeFromConfig() {
        assertEquals(0, HarvesterHoe.level(hoe, Track.TOKEN_BOOST));
        HarvesterHoe.setLevel(hoe, Track.TOKEN_BOOST, 3, cfg);
        assertEquals(3, HarvesterHoe.level(hoe.clone(), Track.TOKEN_BOOST), "a copy carries the level");
        assertEquals(1_000, cfg.tokenBoost().costToUpgradeFrom(3));
        assertEquals(-1, cfg.tokenBoost().costToUpgradeFrom(5), "maxed");
        assertEquals("5x5 columns", HoeMenu.effect(Track.RADIUS, 2, cfg));
    }
}
