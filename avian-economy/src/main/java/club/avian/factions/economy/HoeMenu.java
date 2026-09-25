package club.avian.factions.economy;

import club.avian.factions.api.config.ConfigHandle;
import club.avian.factions.api.economy.Currency;
import club.avian.factions.api.economy.Economy;
import club.avian.factions.api.text.Brand;
import club.avian.factions.api.text.Ui;
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
        holder.inventory = Bukkit.createInventory(holder, SIZE, Ui.title(Ui.PICK, "Harvester Hoe"));
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
        Ui.accents(inv, Material.YELLOW_STAINED_GLASS_PANE, Material.LIME_STAINED_GLASS_PANE);
        var held = player.getInventory().getItemInMainHand();
        var cfg = config.get();
        long tokens = economy.balance(player.getUniqueId(), Currency.TOKENS);
        if (HarvesterHoe.is(held)) {
            // early hoes were diamond
            var hand = held.getType() == Material.NETHERITE_HOE ? held : held.withType(Material.NETHERITE_HOE);
            HarvesterHoe.refreshLore(hand, cfg);   // older hoes pick up the current look
            player.getInventory().setItemInMainHand(hand);
            inv.setItem(PREVIEW, Ui.glow(hand.clone()));
            ICON_SLOTS.forEach((slot, track) -> {
                inv.setItem(slot, upgradeIcon(hand, track, cfg));
                inv.setItem(slot + 9, arrow(hand, track, cfg, tokens));
            });
            inv.setItem(AUTO_SELL, autoSellButton(HarvesterHoe.autoSell(hand, cfg)));
        } else {
            inv.setItem(PREVIEW, Ui.item(Material.NETHERITE_HOE, "<sun><bold>" + Ui.PICK + " Harvester Hoe</bold></sun>",
                    Ui.Tooltip.of("Harvests sugar cane in an area and",
                            "sells it on the spot. Upgrade it with tokens.").lines()));
            inv.setItem(KIT, Ui.item(Material.CHEST_MINECART, "<cane><bold>" + Ui.SPARK + " Free weekly hoe</bold></cane>",
                    Ui.Tooltip.of("Every player can claim one each week.")
                            .action("Click", "Claim it (/kit harvester)").lines()));
            inv.setItem(BUY, cfg.price() > 0
                    ? Ui.glow(Ui.item(Material.NETHERITE_HOE, "<sun><bold>" + Ui.PICK + " Buy a hoe</bold></sun>",
                            Ui.Tooltip.of("Need another one?")
                                    .stat("Price", economy.format(Currency.MONEY, cfg.price()), "money")
                                    .action("Click", "Buy").lines()))
                    : Ui.item(Material.GRAY_DYE, "<dim><bold>Not for sale</bold></dim>",
                            Ui.Tooltip.of("Claim the weekly one instead.").lines()));
        }
        inv.setItem(BALANCE, Ui.item(Material.SUNFLOWER, "<token><bold>" + Ui.TOKEN + " "
                        + economy.format(Currency.TOKENS, tokens) + "</bold></token>",
                Ui.Tooltip.of("Your tokens. Earn them by harvesting", "grown sugar cane.").lines()));
        inv.setItem(HELP, Ui.item(Material.KNOWLEDGE_BOOK, "<xp><bold>" + Ui.INFO + " How it works</bold></xp>",
                Ui.Tooltip.of("Break sugar cane with the hoe: everything",
                                "above the root is harvested, so nothing",
                                "needs replanting.")
                        .line("<dim>" + Ui.DOT + "</dim> <soft>Grown blocks can drop <token>tokens</token>")
                        .line("<dim>" + Ui.DOT + "</dim> <soft>and give <xp>farming XP</xp>.")
                        .line("<dim>" + Ui.DOT + "</dim> <soft>Upgrades stay on the hoe.")
                        .action("Right-click the hoe", "Open this menu").lines()));
        inv.setItem(CLOSE, Ui.close());
    }

    private ItemStack upgradeIcon(ItemStack hoe, Track track, HoeConfig cfg) {
        var upgrade = track.upgrade(cfg);
        int level = HarvesterHoe.level(hoe, track);
        String c = COLOURS.get(track);
        var tip = Ui.Tooltip.of()
                .line(Brand.bar(level, upgrade.maxLevel()) + "  <soft>" + level + "/" + upgrade.maxLevel())
                .stat("Now", effect(track, level, cfg), c);
        if (level < upgrade.maxLevel()) {
            tip.stat("Next", effect(track, level + 1, cfg), c);
        }
        if (track == Track.MONEY_MULTIPLIER) {
            tip.line("<dim>Applies when auto-sell is on.");
        }
        var item = Ui.item(ICONS.get(track), "<" + c + "><bold>" + track.label + "</bold></" + c + ">", tip.lines());
        return level == upgrade.maxLevel() ? Ui.glow(item) : item;
    }

    /** The button under each upgrade: a green "⬆ Upgrade" with its price, or a star when maxed. */
    private ItemStack arrow(ItemStack hoe, Track track, HoeConfig cfg, long tokens) {
        int level = HarvesterHoe.level(hoe, track);
        long cost = track.upgrade(cfg).costToUpgradeFrom(level);
        if (cost < 0) {
            return Ui.glow(Ui.item(Material.NETHER_STAR, "<sun><bold>" + Ui.STAR + " Maxed</bold></sun>",
                    Ui.Tooltip.of("Nothing left to buy here.").lines()));
        }
        boolean affordable = tokens >= cost;
        return Ui.item(affordable ? Material.LIME_STAINED_GLASS_PANE : Material.RED_STAINED_GLASS_PANE,
                (affordable ? "<cane>" : "<bad>") + "<bold>" + Ui.UP + " Upgrade</bold>" + (affordable ? "</cane>" : "</bad>"),
                Ui.Tooltip.of()
                        .stat("Cost", economy.format(Currency.TOKENS, cost), affordable ? "token" : "bad")
                        .stat("Level", level + " " + Ui.ARROW + " " + (level + 1), "soft")
                        .action("Click", affordable ? "Buy" : "Not enough tokens yet").lines());
    }

    private static ItemStack autoSellButton(boolean on) {
        return on
                ? Ui.glow(Ui.item(Material.LIME_DYE, "<money><bold>" + Ui.CHECK + " Auto-sell on</bold></money>",
                        Ui.Tooltip.of("Harvested cane is sold on the spot.")
                                .action("Click", "Keep the cane instead").lines()))
                : Ui.item(Material.GRAY_DYE, "<bad><bold>" + Ui.CROSS + " Auto-sell off</bold></bad>",
                        Ui.Tooltip.of("Harvested cane goes to your inventory",
                                        "(and drops at your feet when full).")
                                .action("Click", "Sell on the spot").lines());
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

}
