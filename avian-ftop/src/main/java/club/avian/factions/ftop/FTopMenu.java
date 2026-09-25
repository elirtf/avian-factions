package club.avian.factions.ftop;

import club.avian.factions.api.text.Brand;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.jspecify.annotations.Nullable;

import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.LongFunction;
import java.util.function.Supplier;

/**
 * F-Top as a menu ({@code /f top}, {@code /ftop}): a podium for the top three with the leaders'
 * heads, places 4–10 below, and further pages of 28. Each faction shows its total, spawner and block
 * value, a bar against first place and its biggest holdings. Clicking a faction opens its
 * {@code /f show} page; "your faction" sits at the bottom whatever your rank.
 */
final class FTopMenu implements Listener {

    /** What the menu needs from FactionsUUID, so it can be tested without it. */
    interface Factions {
        /** The leader of this faction, for its head; null if unknown. */
        @Nullable OfflinePlayer leader(int factionId);

        int onlineMembers(int factionId);

        int members(int factionId);

        /** The viewer's faction id, or null when they have none. */
        @Nullable Integer factionOf(Player player);
    }

    static final int SIZE = 54;
    private static final int[] PODIUM = {13, 21, 23};                           // #1, #2, #3
    private static final int[] REST = {28, 29, 30, 31, 32, 33, 34};             // #4 – #10
    private static final int[] PAGE = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34, 37, 38, 39, 40, 41, 42, 43};             // 28 per later page
    private static final int INFO = 45, PREV = 48, YOURS = 49, NEXT = 50, CLOSE = 53;
    private static final String[] PLACE = {"<sun>", "<soft>", "<hot>"};         // gold, silver, bronze

    private final Supplier<Ranking> ranking;
    private final Factions factions;
    private final LongFunction<String> money;
    private final Clock clock;
    private final Supplier<Integer> recalculateMinutes;

    FTopMenu(Supplier<Ranking> ranking, Factions factions, LongFunction<String> money, Clock clock,
             Supplier<Integer> recalculateMinutes) {
        this.ranking = ranking;
        this.factions = factions;
        this.money = money;
        this.clock = clock;
        this.recalculateMinutes = recalculateMinutes;
    }

    private static final class Holder implements InventoryHolder {
        Inventory inventory;
        int page;
        final Map<Integer, String> tagsBySlot = new HashMap<>();

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }

    void open(Player player, int page) {
        var holder = new Holder();
        holder.inventory = Bukkit.createInventory(holder, SIZE, Brand.mm(
                "<bold><gradient:#FFD23F:#FF7A45:#FF5C5C>✦ F-TOP ✦</gradient></bold>"));
        render(holder, player, page);
        player.openInventory(holder.inventory);
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 0.7f, 1.2f);
    }

    int pages(Ranking r) {
        int after10 = Math.max(0, r.entries().size() - 10);
        return 1 + (after10 + PAGE.length - 1) / PAGE.length;
    }

    private void render(Holder h, Player viewer, int page) {
        var inv = h.inventory;
        var r = ranking.get();
        h.page = Math.max(1, Math.min(page, pages(r)));
        h.tagsBySlot.clear();
        inv.clear();
        frame(inv);
        var entries = r.entries();
        long top = entries.isEmpty() ? 0 : entries.getFirst().total();
        if (entries.isEmpty()) {
            inv.setItem(22, icon(Material.SPAWNER, "<soft><bold>NO FACTION HAS VALUE YET</bold></soft>", List.of(
                    "<soft>Place <cane>spawners</cane> and value blocks in",
                    "<soft>your land to climb F-Top.")));
        } else if (h.page == 1) {
            for (int i = 0; i < Math.min(3, entries.size()); i++) {
                put(h, PODIUM[i], entries.get(i), top, true);
            }
            for (int i = 3; i < Math.min(10, entries.size()); i++) {
                put(h, REST[i - 3], entries.get(i), top, false);
            }
        } else {
            int from = 10 + (h.page - 2) * PAGE.length;
            for (int i = 0; i < PAGE.length && from + i < entries.size(); i++) {
                put(h, PAGE[i], entries.get(from + i), top, false);
            }
        }
        long minutes = Duration.between(r.calculatedAt(), clock.instant()).toMinutes();
        inv.setItem(INFO, icon(Material.CLOCK, "<xp><bold>HOW F-TOP WORKS</bold></xp>", List.of(
                "<soft>Factions are ranked by the value of the",
                "<cane>spawners</cane> <soft>and</soft> <token>value blocks</token> <soft>in their land.",
                "",
                "<soft>Updated <sun>" + (r.calculatedAt().getEpochSecond() == 0 ? "soon"
                        : minutes < 1 ? "just now" : minutes + " min ago") + "</sun>",
                "<dim>Recalculates every " + recalculateMinutes.get() + " minutes.")));
        if (h.page > 1) {
            inv.setItem(PREV, icon(Material.ARROW, "<cane><bold>← PREVIOUS</bold></cane>", List.of("<soft>Page " + (h.page - 1))));
        }
        if (h.page < pages(r)) {
            inv.setItem(NEXT, icon(Material.ARROW, "<cane><bold>NEXT →</bold></cane>", List.of("<soft>Page " + (h.page + 1))));
        }
        inv.setItem(YOURS, yours(viewer, entries, top));
        inv.setItem(CLOSE, icon(Material.BARRIER, "<bad><bold>CLOSE</bold></bad>", List.of()));
    }

    private void put(Holder h, int slot, Ranking.Entry e, long top, boolean podium) {
        h.inventory.setItem(slot, entryIcon(e, top, podium));
        h.tagsBySlot.put(slot, e.owner().tag());
    }

    private ItemStack entryIcon(Ranking.Entry e, long top, boolean podium) {
        String c = e.rank() <= 3 ? PLACE[e.rank() - 1] : "<gem>";
        String end = c.replace("<", "</");
        var lore = new ArrayList<String>();
        lore.add("<soft>Worth  <money><bold>" + money.apply(e.total()) + "</bold></money>");
        lore.add(bar(e.total(), top) + (e.rank() > 1 && top > 0 ? "  <dim>" + (100 * e.total() / top) + "% of #1</dim>" : ""));
        lore.add("");
        lore.add("<soft>Spawners  <cane>" + money.apply(e.spawnerValue()) + "</cane>");
        lore.add("<soft>Blocks    <token>" + money.apply(e.blockValue()) + "</token>");
        var biggest = e.units().entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(4).toList();
        if (!biggest.isEmpty()) {
            lore.add("");
            for (var u : biggest) {
                lore.add("<dim>•</dim> <soft>" + pretty(u.getKey()) + " <xp>x" + String.format("%,d", u.getValue()) + "</xp>");
            }
        }
        lore.add("");
        lore.add("<soft>Members  <sun>" + factions.onlineMembers(e.owner().id()) + "</sun><soft> online / "
                + factions.members(e.owner().id()));
        lore.add("<cane>➜ Click to view the faction</cane>");
        String name = c + "<bold>#" + e.rank() + "  " + e.owner().tag() + "</bold>" + end;
        var leader = factions.leader(e.owner().id());
        ItemStack item;
        if (leader != null) {
            item = icon(Material.PLAYER_HEAD, name, lore);
            item.editMeta(SkullMeta.class, meta -> meta.setOwningPlayer(leader));
        } else {
            item = icon(e.rank() == 1 ? Material.GOLD_BLOCK : e.rank() == 2 ? Material.IRON_BLOCK
                    : e.rank() == 3 ? Material.COPPER_BLOCK : Material.AMETHYST_BLOCK, name, lore);
        }
        if (podium) {
            item.editMeta(meta -> meta.setEnchantmentGlintOverride(true));
        }
        return item;
    }

    private ItemStack yours(Player viewer, List<Ranking.Entry> entries, long top) {
        Integer id = factions.factionOf(viewer);
        if (id == null) {
            return icon(Material.WHITE_BANNER, "<soft><bold>YOUR FACTION</bold></soft>", List.of(
                    "<soft>You're not in a faction.", "<cane>➜ /f</cane> <soft>to create or join one."));
        }
        for (var e : entries) {
            if (e.owner().id() == id) {
                var item = entryIcon(e, top, false);
                item.editMeta(meta -> meta.displayName(Brand.mm("<cane><bold>YOUR FACTION  #" + e.rank() + "</bold></cane>")));
                return item;
            }
        }
        return icon(Material.WHITE_BANNER, "<soft><bold>YOUR FACTION</bold></soft>", List.of(
                "<soft>Not ranked yet: no spawners or value", "<soft>blocks in your land."));
    }

    /** Ten cells against first place. */
    static String bar(long value, long top) {
        int filled = top <= 0 ? 0 : (int) Math.round(10.0 * value / top);
        return "<money>" + "■".repeat(filled) + "</money><dim>" + "■".repeat(10 - filled) + "</dim>";
    }

    /** "IRON_GOLEM" → "Iron Golem". */
    static String pretty(String key) {
        var words = key.toLowerCase().split("_");
        var out = new StringBuilder();
        for (var w : words) {
            if (!w.isEmpty()) {
                out.append(out.isEmpty() ? "" : " ").append(Character.toUpperCase(w.charAt(0))).append(w.substring(1));
            }
        }
        return out.toString();
    }

    private static void frame(Inventory inv) {
        var inside = icon(Material.BLACK_STAINED_GLASS_PANE, " ", List.of());
        var a = icon(Material.ORANGE_STAINED_GLASS_PANE, " ", List.of());
        var b = icon(Material.YELLOW_STAINED_GLASS_PANE, " ", List.of());
        for (int slot = 0; slot < SIZE; slot++) {
            int row = slot / 9, col = slot % 9;
            boolean edge = row == 0 || row == SIZE / 9 - 1 || col == 0 || col == 8;
            inv.setItem(slot, edge ? ((row + col) % 2 == 0 ? a : b) : inside);
        }
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof Holder h)) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player) || event.getClickedInventory() != event.getInventory()) {
            return;
        }
        int slot = event.getRawSlot();
        if (slot == CLOSE) {
            player.closeInventory();
        } else if (slot == PREV && h.page > 1) {
            flip(h, player, h.page - 1);
        } else if (slot == NEXT && h.page < pages(ranking.get())) {
            flip(h, player, h.page + 1);
        } else if (h.tagsBySlot.containsKey(slot)) {
            player.closeInventory();
            player.performCommand("f show " + h.tagsBySlot.get(slot));
        }
    }

    private void flip(Holder h, Player player, int page) {
        render(h, player, page);
        player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 0.8f, 1.0f);
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof Holder) {
            event.setCancelled(true);
        }
    }

    private static ItemStack icon(Material material, String name, List<String> lore) {
        var item = new ItemStack(material);
        item.editMeta(meta -> {
            meta.displayName(Brand.mm(name));
            meta.lore(lore.stream().map(l -> l.isEmpty() ? Component.empty() : Brand.mm(l)).toList());
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        });
        return item;
    }
}
