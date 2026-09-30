package club.avian.factions.factions;

import club.avian.factions.api.text.Brand;
import org.bukkit.Material;
import org.bukkit.block.Container;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.BundleMeta;
import org.bukkit.plugin.Plugin;

/**
 * Spawners can't be kept in an ender chest (owner, 2026-09-29: /ec is for everyone, spawners stay
 * out). An ender chest can't be raided, so spawners hidden there would be safe from raids and
 * invisible to F-Top. After any click or drag in an ender chest, and when one opens, every spawner in
 * it, loose or inside a shulker box or bundle, goes back to the player. Sweeping afterwards catches
 * every way of moving an item (shift-click, number keys, drags, the offhand key) without listing them.
 */
final class EnderChestSpawnerBan implements Listener {

    private final Plugin plugin;

    EnderChestSpawnerBan(Plugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onOpen(InventoryOpenEvent event) {
        if (event.getInventory().getType() == InventoryType.ENDER_CHEST && event.getPlayer() instanceof Player player) {
            plugin.getServer().getScheduler().runTask(plugin, () -> sweep(event.getInventory(), player));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onClick(InventoryClickEvent event) {
        later(event.getView().getTopInventory(), event.getWhoClicked());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDrag(InventoryDragEvent event) {
        later(event.getView().getTopInventory(), event.getWhoClicked());
    }

    private void later(Inventory top, org.bukkit.entity.HumanEntity who) {
        if (top.getType() == InventoryType.ENDER_CHEST && who instanceof Player player) {
            plugin.getServer().getScheduler().runTask(plugin, () -> sweep(top, player));
        }
    }

    /** Moves every spawner-carrying stack out of {@code chest} to the player; returns how many stacks moved. */
    static int sweep(Inventory chest, Player player) {
        int moved = 0;
        for (int slot = 0; slot < chest.getSize(); slot++) {
            ItemStack item = chest.getItem(slot);
            if (carriesSpawner(item)) {
                chest.setItem(slot, null);
                player.getInventory().addItem(item).values()
                        .forEach(left -> player.getWorld().dropItemNaturally(player.getLocation(), left));
                moved++;
            }
        }
        if (moved > 0) {
            player.sendMessage(Brand.mm("<bad>✖</bad> <soft>Spawners can't be kept in an ender chest. "
                    + "They're back in your inventory.</soft>"));
        }
        return moved;
    }

    /** A spawner, or a shulker box or bundle with a spawner inside (at any depth). */
    static boolean carriesSpawner(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return false;
        }
        if (item.getType() == Material.SPAWNER) {
            return true;
        }
        var meta = item.getItemMeta();
        if (meta instanceof BlockStateMeta state && state.hasBlockState() && state.getBlockState() instanceof Container box) {
            for (ItemStack inside : box.getInventory().getContents()) {
                if (carriesSpawner(inside)) {
                    return true;
                }
            }
        }
        if (meta instanceof BundleMeta bundle) {
            return bundle.getItems().stream().anyMatch(EnderChestSpawnerBan::carriesSpawner);
        }
        return false;
    }
}
