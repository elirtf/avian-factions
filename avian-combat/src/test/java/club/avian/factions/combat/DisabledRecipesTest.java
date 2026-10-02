package club.avian.factions.combat;

import org.bukkit.Keyed;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Golden apples are loot-only (#40): the recipe goes, and comes back when taken off the list. */
class DisabledRecipesTest {

    private static final NamespacedKey GOLDEN_APPLE = NamespacedKey.minecraft("golden_apple");

    /** The server's recipes, as a map (MockBukkit's removeRecipe leaves the recipe in place). */
    private final Map<NamespacedKey, Recipe> server = new HashMap<>();
    private DisabledRecipes recipes;

    @BeforeEach
    void setUp() {
        MockBukkit.mock();
        var recipe = new ShapedRecipe(GOLDEN_APPLE, new ItemStack(Material.GOLDEN_APPLE));
        recipe.shape("GGG", "GAG", "GGG");
        recipe.setIngredient('G', Material.GOLD_INGOT);
        recipe.setIngredient('A', Material.APPLE);
        server.put(GOLDEN_APPLE, recipe);
        recipes = new DisabledRecipes(new DisabledRecipes.Registry() {
            @Override
            public Recipe get(NamespacedKey key) {
                return server.get(key);
            }

            @Override
            public boolean remove(NamespacedKey key) {
                return server.remove(key) != null;
            }

            @Override
            public void add(Recipe r) {
                server.put(((Keyed) r).getKey(), r);
            }
        }, Logger.getLogger("test"));
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void theDefaultTakesTheGoldenAppleRecipeAway() {
        recipes.apply(new CombatConfig().disabledRecipes());
        assertNull(server.get(GOLDEN_APPLE));
        assertTrue(recipes.isDisabled(GOLDEN_APPLE));
    }

    @Test
    void takingItOffTheListBringsItBack() {
        recipes.apply(List.of("minecraft:golden_apple"));
        recipes.apply(List.of());
        assertNotNull(server.get(GOLDEN_APPLE));
        assertFalse(recipes.isDisabled(GOLDEN_APPLE));
    }

    @Test
    void reloadingWithTheSameListChangesNothing() {
        recipes.apply(List.of("minecraft:golden_apple"));
        recipes.apply(List.of("minecraft:golden_apple"));
        assertNull(server.get(GOLDEN_APPLE));
        recipes.apply(List.of());
        assertNotNull(server.get(GOLDEN_APPLE), "still restorable after a no-op reload");
    }

    @Test
    void anUnknownRecipeIsSkipped() {
        recipes.apply(List.of("minecraft:no_such_recipe", "minecraft:golden_apple"));
        assertNull(server.get(GOLDEN_APPLE));
        assertFalse(recipes.isDisabled(NamespacedKey.minecraft("no_such_recipe")));
    }
}
