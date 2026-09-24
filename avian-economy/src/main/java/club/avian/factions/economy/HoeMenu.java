package club.avian.factions.economy;

import club.avian.factions.api.config.ConfigHandle;
import club.avian.factions.api.economy.Currency;
import club.avian.factions.api.economy.Economy;
import club.avian.factions.api.text.Brand;
import club.avian.factions.economy.HarvesterHoe.Track;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * {@code /hoe}: buy a Harvester Hoe, or upgrade the one in hand and switch its auto-sell.
 *
 * <p>Layout (5 rows): a two-colour glass frame; the hoe itself at the top; the five upgrades in a
 * row with a big arrow button under each; auto-sell, token balance and help below; close at the
 * bottom. Clicking an upgrade or its arrow buys the next level.
 *
 * <p>Payment is taken first (off the main thread, like every balance change) and the upgrade is
 * applied only after it succeeds, to the same hoe found by its id. If that hoe is gone by then, the
 * tokens are refunded. The open menu is redrawn in place, so it never flickers closed.
 */
final class HoeMenu implements Listener {

    private static final int SIZE = 45;
    private static final int PREVIEW = 4;
    private static final Map<Integer, Track> ICON_SLOTS = Map.of(11, Track.RADIUS, 12, Track.TOKEN_BOOST,
            13, Track.MONEY_MULTIPLIER, 14, Track.RANDOM_DROPS, 15, Track.CULTIVATION);
    private static final int AUTO_SELL = 29;
    private static final int BALANCE = 31;
    private static final int HELP = 33;
    private static final int CLOSE = 40;
    // Without a hoe in hand:
    private static final int KIT = 20;
    private static final int BUY = 22;
    private static final Map<Track, Material> ICONS = Map.of(Track.RADIUS, Material.SUGAR_CANE,
            Track.TOKEN_BOOST, Material.SUNFLOWER, Track.MONEY_MULTIPLIER, Material.EMERALD,
            Track.RANDOM_DROPS, Material.ENDER_CHEST, Track.CULTIVATION, Material.EXPERIENCE_BOTTLE);
    private static final Map<Track, String> COLOURS = Map.of(Track.RADIUS, "cane", Track.TOKEN_BOOST, "token",
            Track.MONEY_MULTIPLIER, "money", Track.RANDOM_DROPS, "gem", Track.CULTIVATION, "xp");

    private final Plugin plugin;
    private final Economy economy;
    private final ConfigHandle<HoeConfig> config;

    HoeMenu(Plugin plugin, Economy economy, ConfigHandle<HoeConfig> config) {
        this.plugin = plugin;
        this.economy = economy;
        this.config = config;
    }

    /** Marks our inventories (never the title, which a renamed item could spoof). */
    private static final class Holder implements InventoryHolder {
        private Inventory inventory;

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }

    void open(Player player) {
        var holder = new Holder();
        holder.inventory = Bukkit.createInventory(holder, SIZE, Brand.title("✦ Harvester Hoe ✦"));
        render(holder.inventory, player);
        player.openInventory(holder.inventory);
        player.playSound(player.getLocation(), Sound.BLOCK_ENDER_CHEST_OPEN, 0.6f, 1.4f);
    }

    /** Redraws the menu the player has open, if it is ours. */
    private void refresh(Player player) {
        var top = player.getOpenInventory().getTopInventory();
        if (top.getHolder() instanceof Holder) {
            render(top, player);
        }
    }

    private void render(Inventory inv, Player player) {
        inv.clear();
        frame(inv);
        var held = player.getInventory().getItemInMainHand();
        var cfg = config.get();
        if (HarvesterHoe.is(held)) {
            // early hoes were diamond
            var hand = held.getType() == Material.NETHERITE_HOE ? held : held.withType(Material.NETHERITE_HOE);
            HarvesterHoe.refreshLore(hand, cfg);   // older hoes pick up the current look
            player.getInventory().setItemInMainHand(hand);
            var preview = hand.clone();
            preview.editMeta(meta -> meta.setEnchantmentGlintOverride(true));
            inv.setItem(PREVIEW, preview);
            ICON_SLOTS.forEach((slot, track) -> {
                inv.setItem(slot, upgradeIcon(hand, track, cfg));
                inv.setItem(slot + 9, arrow(hand, track, cfg));
            });
            inv.setItem(AUTO_SELL, autoSellButton(HarvesterHoe.autoSell(hand, cfg)));
        } else {
            inv.setItem(KIT, icon(Material.CHEST_MINECART, "<cane><bold>FREE WEEKLY HOE</bold></cane>", List.of(
                    "<soft>Every player gets one each week.",
                    "",
                    "<cane>➜ Click to claim</cane> <dim>(/kit harvester)</dim>")));
            inv.setItem(BUY, glow(icon(Material.DIAMOND_HOE, "<sun><bold>BUY A HARVESTER HOE</bold></sun>",
                    cfg.price() > 0 ? List.of(
                            "<soft>Harvests <cane>sugar cane</cane> in an area,",
                            "<soft>sells it on the spot and finds <token>tokens</token>.",
                            "",
                            "<soft>Price  <money>" + economy.format(Currency.MONEY, cfg.price()) + "</money>",
                            "",
                            "<sun>➜ Click to buy</sun>")
                            : List.of("<bad>Not for sale.</bad> <soft>Claim the weekly one."))));
        }
        long tokens = economy.balance(player.getUniqueId(), Currency.TOKENS);
        inv.setItem(BALANCE, icon(Material.SUNFLOWER, "<token><bold>YOUR TOKENS</bold></token>", List.of(
                "<token>" + economy.format(Currency.TOKENS, tokens) + "</token>",
                "",
                "<soft>Earn them by harvesting grown cane.")));
        inv.setItem(HELP, icon(Material.KNOWLEDGE_BOOK, "<xp><bold>HOW IT WORKS</bold></xp>", List.of(
                "<soft>Break sugar cane with the hoe:",
                "<soft>everything above the <cane>root</cane> is harvested,",
                "<soft>so nothing needs replanting.",
                "",
                "<soft>Each grown block can drop <token>tokens</token>",
                "<soft>and gives <xp>farming XP</xp>. Upgrades",
                "<soft>cost tokens and stay on the hoe.")));
        inv.setItem(CLOSE, icon(Material.BARRIER, "<bad><bold>CLOSE</bold></bad>", List.of()));
    }

    /** A two-colour frame: yellow and lime glass alternating, black glass inside so colours pop. */
    private static void frame(Inventory inv) {
        var inside = icon(Material.BLACK_STAINED_GLASS_PANE, " ", List.of());
        var a = icon(Material.YELLOW_STAINED_GLASS_PANE, " ", List.of());
        var b = icon(Material.LIME_STAINED_GLASS_PANE, " ", List.of());
        for (int slot = 0; slot < SIZE; slot++) {
            int row = slot / 9;
            int col = slot % 9;
            boolean edge = row == 0 || row == SIZE / 9 - 1 || col == 0 || col == 8;
            inv.setItem(slot, edge ? ((row + col) % 2 == 0 ? a : b) : inside);
        }
    }

    private ItemStack upgradeIcon(ItemStack hoe, Track track, HoeConfig cfg) {
        var upgrade = track.upgrade(cfg);
        int level = HarvesterHoe.level(hoe, track);
        String c = COLOURS.get(track);
        var lore = new ArrayList<String>();
        lore.add(Brand.bar(level, upgrade.maxLevel()) + "  <soft>" + level + "/" + upgrade.maxLevel());
        lore.add("");
        lore.add("<soft>Now  <" + c + ">" + effect(track, level, cfg) + "</" + c + ">");
        if (level < upgrade.maxLevel()) {
            lore.add("<soft>Next <" + c + ">" + effect(track, level + 1, cfg) + "</" + c + ">");
        }
        if (track == Track.MONEY_MULTIPLIER) {
            lore.add("");
            lore.add("<dim>Applies when auto-sell is on.</dim>");
        }
        var item = icon(ICONS.get(track), "<" + c + "><bold>" + track.label.toUpperCase() + "</bold></" + c + ">", lore);
        return level == upgrade.maxLevel() ? glow(item) : item;
    }

    /** The big button under each upgrade: a green arrow with the price, or a star when maxed. */
    private ItemStack arrow(ItemStack hoe, Track track, HoeConfig cfg) {
        int level = HarvesterHoe.level(hoe, track);
        long cost = track.upgrade(cfg).costToUpgradeFrom(level);
        if (cost < 0) {
            return glow(icon(Material.NETHER_STAR, "<sun><bold>✔ MAXED</bold></sun>", List.of("<soft>Nothing left to buy here.")));
        }
        return icon(Material.LIME_STAINED_GLASS_PANE, "<cane><bold>➜ UPGRADE</bold></cane>", List.of(
                "<soft>Cost  <token>" + economy.format(Currency.TOKENS, cost) + "</token>",
                "",
                "<cane>Click to buy level " + (level + 1) + "</cane>"));
    }

    private static ItemStack autoSellButton(boolean on) {
        return on
                ? glow(icon(Material.LIME_DYE, "<money><bold>AUTO-SELL: ON</bold></money>", List.of(
                        "<soft>Harvested cane is sold on the spot.",
                        "",
                        "<cane>➜ Click to keep cane instead</cane>")))
                : icon(Material.GRAY_DYE, "<bad><bold>AUTO-SELL: OFF</bold></bad>", List.of(
                        "<soft>Harvested cane goes to your inventory",
                        "<soft>(and drops at your feet when full).",
                        "",
                        "<cane>➜ Click to sell on the spot</cane>"));
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
            case RANDOM_DROPS -> level == 0 ? "no random drops" : level * Math.max(1, per) + "x drop chance";
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
        boolean holding = HarvesterHoe.is(player.getInventory().getItemInMainHand());
        if (slot == CLOSE) {
            player.closeInventory();
            click(player);
        } else if (!holding && slot == BUY) {
            buy(player);
        } else if (!holding && slot == KIT) {
            player.closeInventory();
            player.performCommand("kit harvester");
        } else if (holding && slot == AUTO_SELL) {
            toggleAutoSell(player);
        } else if (holding && ICON_SLOTS.containsKey(slot)) {
            upgrade(player, ICON_SLOTS.get(slot));
        } else if (holding && ICON_SLOTS.containsKey(slot - 9)) {
            upgrade(player, ICON_SLOTS.get(slot - 9));
        }
    }

    /** Right-clicking the Harvester Hoe (only the real one, found by its id) opens this menu. */
    @EventHandler(priority = EventPriority.HIGH)
    public void onRightClick(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND
                || (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK)
                || !HarvesterHoe.is(event.getItem())) {
            return;
        }
        event.setCancelled(true);   // no tilling: the hoe is for cane
        open(event.getPlayer());
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof Holder) {
            event.setCancelled(true);
        }
    }

    private void toggleAutoSell(Player player) {
        var hand = player.getInventory().getItemInMainHand();
        var cfg = config.get();
        boolean on = !HarvesterHoe.autoSell(hand, cfg);
        HarvesterHoe.setAutoSell(hand, on, cfg);
        player.getInventory().setItemInMainHand(hand);
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.8f, on ? 1.6f : 0.8f);
        refresh(player);
    }

    private void buy(Player player) {
        var cfg = config.get();
        if (cfg.price() <= 0) {
            fail(player);
            return;
        }
        economy.withdraw(player.getUniqueId(), Currency.MONEY, cfg.price(), "hoe:buy").thenAccept(result ->
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (!result.ok()) {
                        fail(player);
                        player.sendMessage(Brand.mm("<bad>You need <money>" + economy.format(Currency.MONEY, cfg.price())
                                + "</money> for a Harvester Hoe.</bad>"));
                        return;
                    }
                    give(player, HarvesterHoe.create(config.get()));
                    success(player);
                    player.closeInventory();
                    player.sendMessage(Brand.mm("<sun><bold>HARVESTER HOE!</bold></sun> <soft>Break <cane>sugar cane</cane> with it; <sun>/hoe</sun> to upgrade."));
                }));
    }

    private void upgrade(Player player, Track track) {
        var hand = player.getInventory().getItemInMainHand();
        var id = HarvesterHoe.id(hand).orElse(null);
        if (id == null) {
            return;
        }
        int level = HarvesterHoe.level(hand, track);
        long cost = track.upgrade(config.get()).costToUpgradeFrom(level);
        if (cost < 0) {
            fail(player);   // maxed
            return;
        }
        economy.withdraw(player.getUniqueId(), Currency.TOKENS, cost, "hoe:upgrade:" + track.name().toLowerCase())
                .thenAccept(result -> Bukkit.getScheduler().runTask(plugin, () -> {
                    if (!result.ok()) {
                        fail(player);
                        player.sendMessage(Brand.mm("<bad>You need <token>" + economy.format(Currency.TOKENS, cost)
                                + "</token> for that upgrade.</bad>"));
                        return;
                    }
                    int slot = find(player, id);
                    var hoe = slot < 0 ? null : player.getInventory().getItem(slot);
                    // Re-check the level: two clicks in a row must not both buy the same level.
                    if (hoe == null || HarvesterHoe.level(hoe, track) != level) {
                        economy.deposit(player.getUniqueId(), Currency.TOKENS, cost, "hoe:refund");
                        fail(player);
                        player.sendMessage(Brand.mm("<bad>The upgrade could not be applied; your tokens were refunded.</bad>"));
                        return;
                    }
                    HarvesterHoe.setLevel(hoe, track, level + 1, config.get());
                    player.getInventory().setItem(slot, hoe);
                    success(player);
                    String c = COLOURS.get(track);
                    player.sendMessage(Brand.mm("<sun><bold>UPGRADED!</bold></sun> <" + c + ">" + track.label + " "
                            + (level + 1) + "</" + c + "> <dim>➜</dim> <soft>" + effect(track, level + 1, config.get())));
                    refresh(player);
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

    private static void click(Player player) {
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.2f);
    }

    private static void success(Player player) {
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.7f, 1.4f);
    }

    private static void fail(Player player) {
        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.7f, 1.0f);
    }

    private static ItemStack icon(Material material, String name, List<String> lore) {
        var item = new ItemStack(material);
        item.editMeta(meta -> {
            meta.displayName(Brand.mm(name));
            meta.lore(lore.stream().map(line -> line.isEmpty() ? Component.empty() : Brand.mm(line)).toList());
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        });
        return item;
    }

    private static ItemStack glow(ItemStack item) {
        item.editMeta(meta -> meta.setEnchantmentGlintOverride(true));
        return item;
    }
}
