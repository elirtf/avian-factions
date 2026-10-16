package club.avian.factions.economy;

import club.avian.factions.api.text.Brand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

/**
 * Infinite water and lava buckets (owner, 2026-10-16), for base building: pour one and it stays full. Recognised by
 * our mark, never the name. Placing still goes through everyone's checks (faction land, WorldGuard): only the
 * emptying is undone. Dispensers won't take them, so one can't be turned into a plain bucket.
 */
final class InfiniteBuckets implements Listener {

    static final NamespacedKey KEY = new NamespacedKey("avian", "infinite_bucket");

    /** {@code water} or {@code lava}; null for anything else. */
    static ItemStack create(String kind, int amount) {
        Material material = switch (kind) {
            case "water" -> Material.WATER_BUCKET;
            case "lava" -> Material.LAVA_BUCKET;
            default -> null;
        };
        if (material == null) {
            return null;
        }
        boolean water = material == Material.WATER_BUCKET;
        var item = new ItemStack(material, amount);
        item.editMeta(meta -> {
            meta.displayName(Brand.mm(water ? "<bold><gradient:#5FF3E0:#4DA3FF>Infinite Water Bucket</gradient></bold>"
                    : "<bold><gradient:#FFD23F:#FF5C5C>Infinite Lava Bucket</gradient></bold>").decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(
                    line("<soft>Pour it as often as you like:</soft>"),
                    line("<soft>it's always full.</soft>"),
                    Component.empty(),
                    line("<dim>◆</dim> <soft>Used up</soft>  <cane>never</cane>"),
                    Component.empty(),
                    line("<cane>▶ Use</cane> <dim>·</dim> <soft>pour " + (water ? "water" : "lava") + "</soft>")));
            meta.setEnchantmentGlintOverride(true);
            meta.setMaxStackSize(1);
            meta.getPersistentDataContainer().set(KEY, PersistentDataType.STRING, kind);
        });
        return item;
    }

    static boolean is(ItemStack item) {
        return item != null && item.hasItemMeta()
                && item.getItemMeta().getPersistentDataContainer().has(KEY, PersistentDataType.STRING);
    }

    private static Component line(String mm) {
        return Brand.mm(mm).decoration(TextDecoration.ITALIC, false);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEmpty(PlayerBucketEmptyEvent event) {
        var hand = event.getPlayer().getInventory().getItem(event.getHand());
        if (is(hand)) {
            event.setItemStack(hand.clone());   // stays full
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onDispense(BlockDispenseEvent event) {
        if (is(event.getItem())) {
            event.setCancelled(true);
        }
    }
}
