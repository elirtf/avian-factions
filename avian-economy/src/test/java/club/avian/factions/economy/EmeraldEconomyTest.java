package club.avian.factions.economy;

import org.bukkit.Material;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantRecipe;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.world.WorldMock;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The emerald economy (owner, 2026-10-01): spawner villagers drop emeralds; trades never pay them. */
class EmeraldEconomyTest {

    private WorldMock world;
    private PlacedSpawnerMark mark;
    private SpawnerDrops drops;

    @BeforeEach
    void setUp() {
        var server = MockBukkit.mock();
        world = server.addSimpleWorld("world");
        mark = new PlacedSpawnerMark(MockBukkit.createMockPlugin());
        drops = new SpawnerDrops(new HarvesterHoeTest.Handle<>(new EconomyConfig()), mark);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    private List<ItemStack> die(EntityType type, boolean fromPlacedSpawner) {
        var entity = (LivingEntity) world.spawnEntity(world.getSpawnLocation(), type);
        if (fromPlacedSpawner) {
            mark.set(entity);
        }
        var loot = new ArrayList<ItemStack>();
        drops.onDeath(new EntityDeathEvent(entity, DamageSource.builder(DamageType.GENERIC).build(), loot));
        return loot;
    }

    private static int emeralds(List<ItemStack> loot) {
        return loot.stream().filter(i -> i.getType() == Material.EMERALD).mapToInt(ItemStack::getAmount).sum();
    }

    @Test
    void aSpawnerVillagerDropsAnEmerald() {
        assertEquals(1, emeralds(die(EntityType.VILLAGER, true)));
    }

    @Test
    void aBredOrWildVillagerDropsNothing() {
        assertEquals(0, emeralds(die(EntityType.VILLAGER, false)));
    }

    @Test
    void otherSpawnerMobsKeepTheirNormalDrops() {
        assertEquals(0, emeralds(die(EntityType.ZOMBIE, true)));
    }

    @Test
    void tradesThatPayEmeraldsAreRecognised() {
        assertTrue(EmeraldTradeGuard.paysEmeralds(new MerchantRecipe(new ItemStack(Material.EMERALD), 12)));
        assertFalse(EmeraldTradeGuard.paysEmeralds(new MerchantRecipe(new ItemStack(Material.ENCHANTED_BOOK), 12)));
    }
}
