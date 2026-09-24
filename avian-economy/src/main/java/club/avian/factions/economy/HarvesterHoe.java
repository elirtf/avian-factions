package club.avian.factions.economy;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

/**
 * The Harvester Hoe item. Identified by its persistent data, never its name (CLAUDE.md): a renamed
 * diamond hoe is just a hoe. The levels live on the item too, so the hoe keeps them when traded,
 * stored or dropped, and a database loss cannot reset them.
 */
final class HarvesterHoe {

    enum Track {
        RADIUS("radius", "Harvest radius"),
        TOKEN_BOOST("token_boost", "Token boost"),
        MONEY_MULTIPLIER("money_multiplier", "Money multiplier"),
        RANDOM_DROPS("random_drops", "Random drops");

        final NamespacedKey key;
        final String label;

        Track(String id, String label) {
            this.key = new NamespacedKey("avian", "hoe_" + id);
            this.label = label;
        }

        HoeConfig.Upgrade upgrade(HoeConfig cfg) {
            return switch (this) {
                case RADIUS -> cfg.radius();
                case TOKEN_BOOST -> cfg.tokenBoost();
                case MONEY_MULTIPLIER -> cfg.moneyMultiplier();
                case RANDOM_DROPS -> cfg.randomDrops();
            };
        }
    }

    static final NamespacedKey ID = new NamespacedKey("avian", "harvester_hoe");
    private static final TextColor GOLD = TextColor.color(0xE0B44A);
    private static final TextColor STONE = TextColor.color(0x9E9E9E);

    private HarvesterHoe() {
    }

    /** A new level-0 hoe with its own id. */
    static ItemStack create(HoeConfig cfg) {
        var item = new ItemStack(Material.DIAMOND_HOE);
        item.editPersistentDataContainer(pdc -> pdc.set(ID, PersistentDataType.STRING, UUID.randomUUID().toString()));
        item.editMeta(meta -> {
            meta.setUnbreakable(true);
            meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE, ItemFlag.HIDE_ATTRIBUTES);
            meta.itemName(Component.text("Harvester Hoe", GOLD, TextDecoration.BOLD));
        });
        refreshLore(item, cfg);
        return item;
    }

    /** This hoe's id, or empty if {@code item} is not a Harvester Hoe. */
    static Optional<String> id(ItemStack item) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) {
            return Optional.empty();
        }
        return Optional.ofNullable(item.getPersistentDataContainer().get(ID, PersistentDataType.STRING));
    }

    static boolean is(ItemStack item) {
        return id(item).isPresent();
    }

    static int level(ItemStack item, Track track) {
        return item.getPersistentDataContainer().getOrDefault(track.key, PersistentDataType.INTEGER, 0);
    }

    static void setLevel(ItemStack item, Track track, int level, HoeConfig cfg) {
        item.editPersistentDataContainer(pdc -> pdc.set(track.key, PersistentDataType.INTEGER, level));
        refreshLore(item, cfg);
    }

    /** The lore shows the levels; it is display only and rebuilt from the data every time. */
    static void refreshLore(ItemStack item, HoeConfig cfg) {
        var lore = new ArrayList<Component>();
        lore.add(line("Harvests sugar cane, leaving the roots.", STONE));
        lore.add(Component.empty());
        for (var track : Track.values()) {
            int level = level(item, track);
            int max = track.upgrade(cfg).maxLevel();
            lore.add(Component.text(track.label + " ", STONE).decoration(TextDecoration.ITALIC, false)
                    .append(Component.text(level + "/" + max, level == max ? GOLD : NamedTextColor.WHITE)));
        }
        lore.add(Component.empty());
        lore.add(line("/hoe to upgrade", STONE));
        item.editMeta(meta -> meta.lore(lore));
    }

    private static Component line(String text, TextColor color) {
        return Component.text(text, color).decoration(TextDecoration.ITALIC, false);
    }
}
