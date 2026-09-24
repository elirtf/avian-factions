package club.avian.factions.economy;

import club.avian.factions.api.config.ConfigHandle;
import club.avian.factions.api.economy.Currency;
import club.avian.factions.api.economy.Economy;
import club.avian.factions.economy.HarvesterHoe.Track;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * {@code /hoe}: buy a Harvester Hoe with money, or upgrade the one in hand with tokens.
 *
 * <p>Payment is taken first (off the main thread, like every balance change) and the upgrade is
 * applied only after it succeeds, to the same hoe found by its id. If that hoe is gone by then
 * (dropped, stored), the tokens are refunded rather than lost.
 */
final class HoeMenu implements Listener {

    private static final TextColor GOLD = TextColor.color(0xE0B44A);
    private static final TextColor STONE = TextColor.color(0x9E9E9E);
    private static final Map<Integer, Track> SLOTS = Map.of(11, Track.RADIUS, 12, Track.TOKEN_BOOST,
            13, Track.MONEY_MULTIPLIER, 14, Track.RANDOM_DROPS, 15, Track.CULTIVATION);
    private static final int BUY_SLOT = 13;
    private static final Map<Track, Material> ICONS = Map.of(Track.RADIUS, Material.SUGAR_CANE,
            Track.TOKEN_BOOST, Material.SUNFLOWER, Track.MONEY_MULTIPLIER, Material.GOLD_INGOT,
            Track.RANDOM_DROPS, Material.CHEST, Track.CULTIVATION, Material.EXPERIENCE_BOTTLE);

    private final Plugin plugin;
    private final Economy economy;
    private final ConfigHandle<HoeConfig> config;

    HoeMenu(Plugin plugin, Economy economy, ConfigHandle<HoeConfig> config) {
        this.plugin = plugin;
        this.economy = economy;
        this.config = config;
    }

    /** Marks our inventories, so clicks in them are ours and nothing can be taken out. */
    private static final class Holder implements InventoryHolder {
        private Inventory inventory;

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }

    void open(Player player) {
        var holder = new Holder();
        var inv = Bukkit.createInventory(holder, 27, Component.text("Harvester Hoe", GOLD, TextDecoration.BOLD));
        holder.inventory = inv;
        var filler = icon(Material.GRAY_STAINED_GLASS_PANE, Component.text(" "), List.of());
        for (int i = 0; i < 27; i++) {
            inv.setItem(i, filler);
        }
        var hand = player.getInventory().getItemInMainHand();
        var cfg = config.get();
        if (!HarvesterHoe.is(hand)) {
            var lore = new ArrayList<Component>();
            lore.add(text("Harvests sugar cane in an area, sells it", STONE));
            lore.add(text("on the spot and finds tokens as it goes.", STONE));
            lore.add(Component.empty());
            if (cfg.price() > 0) {
                lore.add(text("Price: " + economy.format(Currency.MONEY, cfg.price()), NamedTextColor.WHITE));
                lore.add(text("Click to buy", GOLD));
            } else {
                lore.add(text("Not for sale.", NamedTextColor.RED));
            }
            lore.add(Component.empty());
            lore.add(text("Hold your hoe and run /hoe to upgrade it.", STONE));
            inv.setItem(BUY_SLOT, icon(Material.DIAMOND_HOE, Component.text("Get a Harvester Hoe", GOLD, TextDecoration.BOLD), lore));
        } else {
            SLOTS.forEach((slot, track) -> inv.setItem(slot, upgradeIcon(hand, track, cfg)));
        }
        player.openInventory(inv);
    }

    private ItemStack upgradeIcon(ItemStack hoe, Track track, HoeConfig cfg) {
        var upgrade = track.upgrade(cfg);
        int level = HarvesterHoe.level(hoe, track);
        long cost = upgrade.costToUpgradeFrom(level);
        var lore = new ArrayList<Component>();
        lore.add(text(effect(track, level, cfg), STONE));
        lore.add(text("Level " + level + "/" + upgrade.maxLevel(), NamedTextColor.WHITE));
        lore.add(Component.empty());
        if (cost < 0) {
            lore.add(text("Maxed", GOLD));
        } else {
            lore.add(text("Next: " + effect(track, level + 1, cfg), NamedTextColor.WHITE));
            lore.add(text("Cost: " + economy.format(Currency.TOKENS, cost), TextColor.color(0xF2D06B)));
            lore.add(text("Click to upgrade", GOLD));
        }
        return icon(ICONS.get(track), Component.text(track.label, GOLD, TextDecoration.BOLD), lore);
    }

    static String effect(Track track, int level, HoeConfig cfg) {
        int per = track.upgrade(cfg).perLevel();
        return switch (track) {
            case RADIUS -> {
                int side = 2 * Math.min(level * Math.max(1, per), 4) + 1;
                yield side + "x" + side + " columns";
            }
            case TOKEN_BOOST -> "+" + per * level + "% token chance";
            case MONEY_MULTIPLIER -> "+" + per * level + "% sell price";
            case RANDOM_DROPS -> level == 0 ? "No random drops" : level * Math.max(1, per) + "x drop chance";
            case CULTIVATION -> "+" + per * level + "% farming XP";
        };
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof Holder)) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player) || event.getClickedInventory() != event.getInventory()) {
            return;
        }
        int slot = event.getRawSlot();
        if (slot == BUY_SLOT && !HarvesterHoe.is(player.getInventory().getItemInMainHand())) {
            buy(player);
        } else if (SLOTS.containsKey(slot)) {
            upgrade(player, SLOTS.get(slot));
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof Holder) {
            event.setCancelled(true);
        }
    }

    private void buy(Player player) {
        var cfg = config.get();
        if (cfg.price() <= 0) {
            return;
        }
        player.closeInventory();
        economy.withdraw(player.getUniqueId(), Currency.MONEY, cfg.price(), "hoe:buy").thenAccept(result ->
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (!result.ok()) {
                        player.sendMessage(Component.text("You need " + economy.format(Currency.MONEY, cfg.price())
                                + " for a Harvester Hoe.", NamedTextColor.RED));
                        return;
                    }
                    give(player, HarvesterHoe.create(config.get()));
                    player.sendMessage(Component.text("You bought a Harvester Hoe. Break sugar cane with it; /hoe to upgrade.", GOLD));
                }));
    }

    private void upgrade(Player player, Track track) {
        var hand = player.getInventory().getItemInMainHand();
        var id = HarvesterHoe.id(hand).orElse(null);
        if (id == null) {
            player.closeInventory();
            return;
        }
        int level = HarvesterHoe.level(hand, track);
        long cost = track.upgrade(config.get()).costToUpgradeFrom(level);
        if (cost < 0) {
            return;   // maxed
        }
        player.closeInventory();
        economy.withdraw(player.getUniqueId(), Currency.TOKENS, cost, "hoe:upgrade:" + track.name().toLowerCase())
                .thenAccept(result -> Bukkit.getScheduler().runTask(plugin, () -> {
                    if (!result.ok()) {
                        player.sendMessage(Component.text("You need " + economy.format(Currency.TOKENS, cost)
                                + " for that upgrade.", NamedTextColor.RED));
                        return;
                    }
                    int slot = find(player, id);
                    var hoe = slot < 0 ? null : player.getInventory().getItem(slot);
                    // Re-check the level: two clicks in a row must not both buy the same level.
                    if (hoe == null || HarvesterHoe.level(hoe, track) != level) {
                        economy.deposit(player.getUniqueId(), Currency.TOKENS, cost, "hoe:refund");
                        player.sendMessage(Component.text("The upgrade could not be applied; your tokens were refunded.", NamedTextColor.RED));
                        return;
                    }
                    HarvesterHoe.setLevel(hoe, track, level + 1, config.get());
                    player.getInventory().setItem(slot, hoe);
                    player.sendMessage(Component.text(track.label + " is now level " + (level + 1) + ": "
                            + effect(track, level + 1, config.get()) + ".", GOLD));
                    open(player);
                }));
    }

    /** The inventory slot holding the hoe with this id, or -1. */
    private static int find(Player player, String id) {
        var contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            if (contents[i] != null && HarvesterHoe.id(contents[i]).filter(id::equals).isPresent()) {
                return i;
            }
        }
        return -1;
    }

    static void give(Player player, ItemStack item) {
        player.getInventory().addItem(item).values()
                .forEach(left -> player.getWorld().dropItemNaturally(player.getLocation(), left));
    }

    private static ItemStack icon(Material material, Component name, List<Component> lore) {
        var item = new ItemStack(material);
        item.editMeta(meta -> {
            meta.displayName(name.decoration(TextDecoration.ITALIC, false));
            meta.lore(lore);
        });
        return item;
    }

    private static Component text(String s, TextColor color) {
        return Component.text(s, color).decoration(TextDecoration.ITALIC, false);
    }
}
