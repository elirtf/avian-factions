package club.avian.factions.combat;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** At most 6 enchantments per item, vanilla and custom together (the default). */
class EnchantLimitTest {

    @BeforeEach
    void setUp() {
        MockBukkit.mock();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    private static ItemStack sword(int enchantments) {
        var item = new ItemStack(Material.DIAMOND_SWORD);
        var all = List.of(Enchantment.SHARPNESS, Enchantment.UNBREAKING, Enchantment.MENDING, Enchantment.LOOTING,
                Enchantment.FIRE_ASPECT, Enchantment.KNOCKBACK, Enchantment.SWEEPING_EDGE);
        for (int i = 0; i < enchantments; i++) {
            item.addUnsafeEnchantment(all.get(i), 1);
        }
        return item;
    }

    @Test
    void sixIsAllowedSevenIsNot() {
        assertFalse(EnchantLimit.overLimit(sword(6), 6));
        assertTrue(EnchantLimit.overLimit(sword(7), 6));
    }

    @Test
    void booksAreNotCapped() {
        var book = new ItemStack(Material.ENCHANTED_BOOK);
        book.addUnsafeEnchantment(Enchantment.SHARPNESS, 1);
        for (var extra : List.of(Enchantment.UNBREAKING, Enchantment.MENDING)) {
            book.addUnsafeEnchantment(extra, 1);
        }
        assertFalse(EnchantLimit.overLimit(book, 1), "a book with 3 is still fine under a limit of 1");
    }

    @Test
    void zeroMeansNoLimitAndNoResultIsFine() {
        assertFalse(EnchantLimit.overLimit(sword(7), 0));
        assertFalse(EnchantLimit.overLimit(null, 6));
    }
}
