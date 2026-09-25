package club.avian.factions.ftop;

import club.avian.factions.api.text.Brand;
import club.avian.factions.api.text.Ui;
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
    private static final int INFO = 4, PREV = 45, YOURS = 47, CLOSE = 49, NEXT = 53;
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
        holder.inventory = Bukkit.createInventory(holder, SIZE, Ui.title(Ui.CROWN, "F-Top"));
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
        Ui.accents(inv, Material.ORANGE_STAINED_GLASS_PANE, Material.YELLOW_STAINED_GLASS_PANE);
        var entries = r.entries();
        long top = entries.isEmpty() ? 0 : entries.getFirst().total();
        if (entries.isEmpty()) {
            inv.setItem(22, Ui.item(Material.SPAWNER, "<soft><bold>No faction has value yet</bold></soft>",
                    Ui.Tooltip.of("Place spawners and value blocks in", "your land to climb F-Top.").lines()));
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
        String updated = r.calculatedAt().getEpochSecond() == 0 ? "soon" : minutes < 1 ? "just now" : minutes + " min ago";
        inv.setItem(INFO, Ui.glow(Ui.item(Material.GOLDEN_HELMET, "<sun><bold>" + Ui.CROWN + " F-Top</bold></sun>",
                Ui.Tooltip.of("Factions ranked by the value of the", "spawners and value blocks in their land.")
                        .stat("Updated", updated, "sun")
                        .stat("Every", recalculateMinutes.get() + " min", "soft").lines())));
        if (h.page > 1) {
            inv.setItem(PREV, Ui.prev(h.page - 1));
        }
        if (h.page < pages(r)) {
            inv.setItem(NEXT, Ui.next(h.page + 1));
        }
        inv.setItem(YOURS, yours(viewer, entries, top));
        inv.setItem(CLOSE, Ui.close());
    }

    private void put(Holder h, int slot, Ranking.Entry e, long top, boolean podium) {
        h.inventory.setItem(slot, entryIcon(e, top, podium));
        h.tagsBySlot.put(slot, e.owner().tag());
    }

    private ItemStack entryIcon(Ranking.Entry e, long top, boolean podium) {
        String c = e.rank() <= 3 ? PLACE[e.rank() - 1] : "<gem>";
        String end = c.replace("<", "</");
        var tip = Ui.Tooltip.of()
                .stat("Worth", "<bold>" + money.apply(e.total()) + "</bold>", "money")
                .line(bar(e.total(), top) + (e.rank() > 1 && top > 0 ? "  <dim>" + (100 * e.total() / top) + "% of #1</dim>" : ""))
                .stat("Spawners", money.apply(e.spawnerValue()), "cane")
                .stat("Blocks", money.apply(e.blockValue()), "token");
        e.units().entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(4)
                .forEach(u -> tip.line("   <soft>" + pretty(u.getKey()) + " <xp>x" + String.format("%,d", u.getValue()) + "</xp>"));
        tip.stat("Online", factions.onlineMembers(e.owner().id()) + " / " + factions.members(e.owner().id()), "sun")
                .action("Click", "View the faction");
        String name = c + "<bold>" + (e.rank() <= 3 ? Ui.CROWN + " " : "") + "#" + e.rank() + "  " + e.owner().tag() + "</bold>" + end;
        var leader = factions.leader(e.owner().id());
        ItemStack item;
        if (leader != null) {
            item = Ui.item(Material.PLAYER_HEAD, name, tip.lines());
            item.editMeta(SkullMeta.class, meta -> meta.setOwningPlayer(leader));
        } else {
            item = Ui.item(e.rank() == 1 ? Material.GOLD_BLOCK : e.rank() == 2 ? Material.IRON_BLOCK
                    : e.rank() == 3 ? Material.COPPER_BLOCK : Material.AMETHYST_BLOCK, name, tip.lines());
        }
        return podium ? Ui.glow(item) : item;
    }

    private ItemStack yours(Player viewer, List<Ranking.Entry> entries, long top) {
        Integer id = factions.factionOf(viewer);
        if (id == null) {
            return Ui.item(Material.WHITE_BANNER, "<soft><bold>Your faction</bold></soft>",
                    Ui.Tooltip.of("You're not in a faction yet.").action("Type /f", "Create or join one").lines());
        }
        for (var e : entries) {
            if (e.owner().id() == id) {
                var item = entryIcon(e, top, false);
                item.editMeta(meta -> meta.displayName(Brand.mm("<cane><bold>" + Ui.STAR + " Your faction  #" + e.rank() + "</bold></cane>")));
                return item;
            }
        }
        return Ui.item(Material.WHITE_BANNER, "<soft><bold>Your faction</bold></soft>",
                Ui.Tooltip.of("Not ranked yet: no spawners or", "value blocks in your land.").lines());
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

}
