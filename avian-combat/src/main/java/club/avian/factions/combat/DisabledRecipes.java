package club.avian.factions.combat;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.Recipe;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Takes recipes out of the game ({@code combat.conf} {@code disabled-recipes}; #40: golden apples are
 * loot-only, so they stay rare). Removed recipes are kept, so taking a key off the list and reloading
 * puts that recipe back without a restart. Removal reaches crafting tables, crafters and the recipe
 * book alike, because the server no longer knows the recipe.
 */
final class DisabledRecipes {

    /** Where recipes live: the server in production, a map in tests. */
    interface Registry {
        Recipe get(NamespacedKey key);

        boolean remove(NamespacedKey key);

        void add(Recipe recipe);

        /** The server's own recipes; removals and restores resend them to players' recipe books. */
        Registry SERVER = new Registry() {
            @Override
            public Recipe get(NamespacedKey key) {
                return Bukkit.getRecipe(key);
            }

            @Override
            public boolean remove(NamespacedKey key) {
                return Bukkit.removeRecipe(key, true);
            }

            @Override
            public void add(Recipe recipe) {
                Bukkit.addRecipe(recipe, true);
            }
        };
    }

    private final Registry registry;
    private final Logger logger;
    private final Map<NamespacedKey, Recipe> removed = new HashMap<>();

    DisabledRecipes(Registry registry, Logger logger) {
        this.registry = registry;
        this.logger = logger;
    }

    /** Makes exactly these recipes unavailable: removes new ones, restores ones no longer listed. */
    void apply(Collection<String> keys) {
        var wanted = new HashSet<NamespacedKey>();
        for (var key : keys) {
            var parsed = NamespacedKey.fromString(key);
            if (parsed != null) {
                wanted.add(parsed);
            }
        }
        for (var it = removed.entrySet().iterator(); it.hasNext(); ) {
            var entry = it.next();
            if (!wanted.contains(entry.getKey())) {
                registry.add(entry.getValue());
                it.remove();
                logger.info("Recipe " + entry.getKey() + " is craftable again");
            }
        }
        for (var key : wanted) {
            if (removed.containsKey(key)) {
                continue;
            }
            var recipe = registry.get(key);
            if (recipe == null) {
                logger.warning("disabled-recipes: no recipe " + key + " on this server; nothing to remove");
                continue;
            }
            if (registry.remove(key)) {
                removed.put(key, recipe);
            }
        }
        if (!removed.isEmpty()) {
            logger.info("Recipes disabled: " + removed.keySet());
        }
    }

    boolean isDisabled(NamespacedKey key) {
        return removed.containsKey(key);
    }
}
