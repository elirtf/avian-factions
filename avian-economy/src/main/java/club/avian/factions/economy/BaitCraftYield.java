package club.avian.factions.economy;

import club.avian.factions.api.config.ConfigHandle;
import org.bukkit.NamespacedKey;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.Bukkit;
import org.bukkit.Keyed;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.persistence.PersistentDataType;

/**
 * Crafted EvenMoreFish baits come out several at a time (owner, 2026-10-16: craftable, a challenge but not crazy
 * difficult). A bait covers one catch and EvenMoreFish's recipes make one, so a craft would buy a single boosted
 * catch; {@code economy.conf}'s {@code bait-craft-yield} says how many each recipe makes instead. Baits are
 * recognised by EvenMoreFish's own mark on the item, which holds the bait's id. Every bait recipe is also put in
 * each player's recipe book when they join, so the baits can be found without a wiki.
 */
final class BaitCraftYield implements Listener {

    static final NamespacedKey BAIT = new NamespacedKey("evenmorefish", "emf-bait");

    private final ConfigHandle<EconomyConfig> config;

    BaitCraftYield(ConfigHandle<EconomyConfig> config) {
        this.config = config;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPrepare(PrepareItemCraftEvent event) {
        var result = event.getInventory().getResult();
        if (result == null || !result.hasItemMeta()) {
            return;
        }
        var id = result.getItemMeta().getPersistentDataContainer().get(BAIT, PersistentDataType.STRING);
        Integer amount = id == null ? null : config.get().baitCraftYield().get(id);
        if (amount != null && amount > 1 && result.getAmount() != amount) {
            var more = result.clone();
            more.setAmount(Math.min(amount, more.getMaxStackSize()));
            event.getInventory().setResult(more);
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        var keys = new java.util.ArrayList<NamespacedKey>();
        Bukkit.recipeIterator().forEachRemaining(recipe -> {
            if (recipe instanceof Keyed keyed && keyed.getKey().getNamespace().equals("evenmorefish")
                    && keyed.getKey().getKey().startsWith("bait-")) {
                keys.add(keyed.getKey());
            }
        });
        event.getPlayer().discoverRecipes(keys);
    }
}
