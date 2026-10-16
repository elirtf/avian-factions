package club.avian.factions.economy;

import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Key merging: 5 Common make 1 Rare; click merges once, shift-click all; nothing is made if the take fails. */
class KeyMergeTest {

    private static final class Store implements KeyMerge.Keys {
        final Map<String, Integer> keys = new HashMap<>();
        boolean takeFails;

        public int virtual(Player p, String crate) {
            return keys.getOrDefault(crate, 0);
        }

        public boolean take(Player p, String crate, int amount) {
            if (takeFails || virtual(p, crate) < amount) {
                return false;
            }
            keys.merge(crate, -amount, Integer::sum);
            return true;
        }

        public void give(Player p, String crate, int amount) {
            keys.merge(crate, amount, Integer::sum);
        }
    }

    private final KeyMergeConfig.Step commonToRare = new KeyMergeConfig.Step("Common", "Rare", 5);

    @Test
    void aClickMergesOnce() {
        var store = new Store();
        store.keys.put("Common", 12);
        assertEquals(1, KeyMerge.merge(store, null, commonToRare, false));
        assertEquals(7, store.keys.get("Common"));
        assertEquals(1, store.keys.get("Rare"));
    }

    @Test
    void shiftClickMergesAllItCan() {
        var store = new Store();
        store.keys.put("Common", 12);
        assertEquals(2, KeyMerge.merge(store, null, commonToRare, true));
        assertEquals(2, store.keys.get("Common"), "the 2 left over stay");
        assertEquals(2, store.keys.get("Rare"));
    }

    @Test
    void tooFewMakesNothing() {
        var store = new Store();
        store.keys.put("Common", 4);
        assertEquals(0, KeyMerge.merge(store, null, commonToRare, true));
        assertEquals(4, store.keys.get("Common"));
        assertEquals(null, store.keys.get("Rare"));
    }

    @Test
    void aFailedTakeNeverMakesAKey() {
        var store = new Store();
        store.keys.put("Common", 10);
        store.takeFails = true;
        assertEquals(0, KeyMerge.merge(store, null, commonToRare, true));
        assertEquals(null, store.keys.get("Rare"));
    }

    @Test
    void enchantCrateNamesReadWell() {
        assertEquals("Enchant Legendary", KeyMerge.label("EnchantLegendary"));
        assertEquals("Common", KeyMerge.label("Common"));
    }
}
