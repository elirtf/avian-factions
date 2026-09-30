package club.avian.factions.cosmetics;

import club.avian.factions.api.config.ConfigHandle;
import club.avian.factions.api.text.Brand;
import club.avian.factions.api.text.Ui;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.HashMap;
import java.util.Map;

/**
 * {@code /trails}: every trail, yours to wear or locked. Menu kit layout: colour on the top and
 * bottom rows, the subject top-centre, trails in the open middle, "take it off" and close in the footer.
 */
final class TrailsMenu implements Listener {

    static final int SIZE = 54;
    static final int[] SLOTS = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34};
    static final int INFO = 4, OFF = 48;
    private static final Map<String, String> TIER = Map.of("common", "cane", "rare", "xp", "epic", "gem");

    private final ConfigHandle<TrailsConfig> config;
    private final Trails trails;

    TrailsMenu(ConfigHandle<TrailsConfig> config, Trails trails) {
        this.config = config;
        this.trails = trails;
    }

    private static final class Holder implements InventoryHolder {
        Inventory inventory;
        final Map<Integer, String> idBySlot = new HashMap<>();

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }

    void open(Player player) {
        var holder = new Holder();
        holder.inventory = Bukkit.createInventory(holder, SIZE, Ui.title(Ui.SPARK, "Trails"));
        render(holder, player);
        player.openInventory(holder.inventory);
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 0.7f, 1.4f);
    }

    private void render(Holder h, Player player) {
        var inv = h.inventory;
        inv.clear();
        h.idBySlot.clear();
        Ui.accents(inv, Material.PINK_STAINED_GLASS_PANE, Material.MAGENTA_STAINED_GLASS_PANE);
        String wearing = trails.chosen(player).orElse(null);
        var all = config.get().trails();
        long owned = all.keySet().stream().filter(id -> Trails.owns(player, id)).count();
        inv.setItem(INFO, Ui.glow(Ui.item(Material.FIREWORK_STAR, "<gem><bold>" + Ui.SPARK + " Trails</bold></gem>",
                Ui.Tooltip.of("A trail of particles at your feet", "while you move. Win them from", "the Cosmetics crate.")
                        .stat("Unlocked", owned + " / " + all.size(), "sun")
                        .stat("Wearing", wearing == null ? "none" : all.get(wearing).name(), "soft").lines())));
        int i = 0;
        for (var entry : all.entrySet()) {
            if (i >= SLOTS.length) {
                break;
            }
            String id = entry.getKey();
            var trail = entry.getValue();
            String colour = TIER.getOrDefault(trail.tier(), "soft");
            String rarity = trail.tier().substring(0, 1).toUpperCase() + trail.tier().substring(1);
            boolean mine = Trails.owns(player, id);
            boolean on = id.equals(wearing);
            var tip = Ui.Tooltip.of().stat("Rarity", rarity, colour);
            if (on) {
                tip.line("<cane>" + Ui.CHECK + " Wearing it</cane>").action("Click", "Take it off");
            } else if (mine) {
                tip.action("Click", "Wear this trail");
            } else {
                tip.line("<dim>Locked · win it from the Cosmetics crate</dim>");
            }
            var icon = Ui.item(mine ? trail.icon() : Material.GRAY_DYE,
                    "<bold>" + trail.name() + "</bold>" + (mine ? "" : " <dim>" + Ui.CROSS + "</dim>"), tip.lines());
            inv.setItem(SLOTS[i], on ? Ui.glow(icon) : icon);
            h.idBySlot.put(SLOTS[i], id);
            i++;
        }
        if (wearing != null) {
            inv.setItem(OFF, Ui.item(Material.BUCKET, "<hot><bold>Take it off</bold></hot>",
                    Ui.Tooltip.of("Stop showing a trail.").action("Click", "No trail").lines()));
        }
        inv.setItem(Ui.closeSlot(SIZE), Ui.close());
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof Holder h)) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player) || event.getClickedInventory() != h.inventory) {
            return;
        }
        int slot = event.getRawSlot();
        if (slot == Ui.closeSlot(SIZE)) {
            player.closeInventory();
            return;
        }
        if (slot == OFF && trails.chosen(player).isPresent()) {
            trails.clear(player);
            player.sendMessage(Brand.mm("<soft>Trail off.</soft>"));
        } else {
            String id = h.idBySlot.get(slot);
            if (id == null) {
                return;
            }
            if (!Trails.owns(player, id)) {
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.7f, 1.0f);
                player.sendMessage(Brand.mm("<bad>" + Ui.CROSS + "</bad> <soft>That trail is locked. Win it from the "
                        + "<gem>Cosmetics crate</gem>.</soft>"));
                return;
            }
            if (id.equals(trails.chosen(player).orElse(null))) {
                trails.clear(player);
                player.sendMessage(Brand.mm("<soft>Trail off.</soft>"));
            } else {
                trails.choose(player, id);
                player.sendMessage(Brand.mm("<gem>" + Ui.SPARK + "</gem> <soft>Now wearing</soft> <bold>"
                        + config.get().trails().get(id).name() + "</bold><soft>.</soft>"));
            }
        }
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.7f, 1.2f);
        render(h, player);
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof Holder) {
            event.setCancelled(true);
        }
    }
}
