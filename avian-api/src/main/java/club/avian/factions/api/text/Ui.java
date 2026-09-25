package club.avian.factions.api.text;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * The menu kit: one look for every Avian menu, light and easy to follow rather than a wall of glass.
 *
 * <ul>
 *   <li><b>Frame:</b> colour on the top and bottom rows only; the middle stays open.</li>
 *   <li><b>Header:</b> what the menu is about sits top-centre.</li>
 *   <li><b>Tooltips</b> in one order: a short line of purpose, stats as {@code ◆ Label  value},
 *       then what a click does: {@code ▶ Click · Upgrade}. Built with {@link Tooltip}.</li>
 *   <li><b>Footer:</b> {@code ◀}/{@code ▶} pages in the bottom corners, {@code ✖ Close} in the centre.</li>
 *   <li><b>Icons:</b> glyphs the vanilla font draws, so no resource pack is needed.</li>
 * </ul>
 */
public final class Ui {

    public static final String SPARK = "✦";
    public static final String UP = "⬆";
    public static final String STAR = "★";
    public static final String CHECK = "✔";
    public static final String CROSS = "✖";
    public static final String INFO = "ⓘ";
    public static final String ARROW = "➜";
    public static final String LEFT = "◀";
    public static final String RIGHT = "▶";
    public static final String DOT = "◆";
    public static final String PICK = "⛏";
    public static final String TOKEN = "◎";
    public static final String XP = "✚";
    public static final String CLOCK = "⌛";
    public static final String CROWN = "♛";

    private Ui() {
    }

    /** An item with a brand-styled name and lore; empty lore lines stay blank. */
    public static ItemStack item(Material material, String name, List<String> lore) {
        var item = new ItemStack(material);
        item.editMeta(meta -> {
            meta.displayName(Brand.mm(name));
            meta.lore(lore.stream().map(l -> l.isEmpty() ? Component.empty() : Brand.mm(l)).toList());
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        });
        return item;
    }

    public static ItemStack glow(ItemStack item) {
        item.editMeta(meta -> meta.setEnchantmentGlintOverride(true));
        return item;
    }

    /** Colour on the top and bottom rows (alternating {@code a} and {@code b}); the rest stays open. */
    public static void accents(Inventory inv, Material a, Material b) {
        var first = item(a, " ", List.of());
        var second = item(b, " ", List.of());
        int size = inv.getSize();
        for (int col = 0; col < 9; col++) {
            var pane = col % 2 == 0 ? first : second;
            inv.setItem(col, pane);
            inv.setItem(size - 9 + col, pane);
        }
    }

    /** Bottom-centre slot of a menu this size. */
    public static int closeSlot(int size) {
        return size - 5;
    }

    public static int prevSlot(int size) {
        return size - 9;
    }

    public static int nextSlot(int size) {
        return size - 1;
    }

    public static ItemStack close() {
        return item(Material.BARRIER, "<bad><bold>" + CROSS + " Close</bold></bad>", List.of());
    }

    public static ItemStack prev(int page) {
        return item(Material.ARROW, "<cane><bold>" + LEFT + " Previous</bold></cane>", List.of("<dim>Page " + page));
    }

    public static ItemStack next(int page) {
        return item(Material.ARROW, "<cane><bold>Next " + RIGHT + "</bold></cane>", List.of("<dim>Page " + page));
    }

    /** A title for a menu: one icon and a gradient name. */
    public static Component title(String icon, String name) {
        return Brand.mm("<bold><gradient:#FFD23F:#FF7A45>" + icon + " " + name + "</gradient></bold>");
    }

    /** Tooltip lines in the kit's order: purpose, stats, then what clicking does. */
    public static final class Tooltip {
        private final List<String> about = new ArrayList<>();
        private final List<String> stats = new ArrayList<>();
        private final List<String> actions = new ArrayList<>();

        public static Tooltip of(String... purpose) {
            var t = new Tooltip();
            for (var line : purpose) {
                t.about.add("<soft>" + line);
            }
            return t;
        }

        /** {@code ◆ Label  value}; {@code colour} is a brand tag name such as "money". */
        public Tooltip stat(String label, String value, String colour) {
            stats.add("<dim>" + DOT + "</dim> <soft>" + label + "</soft>  <" + colour + ">" + value + "</" + colour + ">");
            return this;
        }

        /** A plain extra line in the stats block, already styled. */
        public Tooltip line(String miniMessage) {
            stats.add(miniMessage);
            return this;
        }

        /** {@code ▶ Click · Upgrade}. */
        public Tooltip action(String click, String what) {
            actions.add("<cane>" + RIGHT + " " + click + "</cane> <dim>·</dim> <soft>" + what);
            return this;
        }

        public List<String> lines() {
            var all = new ArrayList<String>(about);
            if (!stats.isEmpty()) {
                if (!all.isEmpty()) {
                    all.add("");
                }
                all.addAll(stats);
            }
            if (!actions.isEmpty()) {
                all.add("");
                all.addAll(actions);
            }
            return all;
        }
    }
}
