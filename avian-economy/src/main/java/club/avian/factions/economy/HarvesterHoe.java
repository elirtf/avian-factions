package club.avian.factions.economy;

import club.avian.factions.api.text.Brand;
import net.kyori.adventure.text.Component;
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
        RANDOM_DROPS("random_drops", "Random drops"),
        CULTIVATION("cultivation", "Cultivation");

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
                case CULTIVATION -> cfg.cultivation();
            };
        }
    }

    static final NamespacedKey ID = new NamespacedKey("avian", "harvester_hoe");
    static final NamespacedKey AUTO_SELL = new NamespacedKey("avian", "hoe_auto_sell");

    private HarvesterHoe() {
    }

    /** A new level-0 hoe with its own id. */
    static ItemStack create(HoeConfig cfg) {
        var item = new ItemStack(Material.DIAMOND_HOE);
        item.editPersistentDataContainer(pdc -> pdc.set(ID, PersistentDataType.STRING, UUID.randomUUID().toString()));
        item.editMeta(meta -> {
            meta.setUnbreakable(true);
            meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE, ItemFlag.HIDE_ATTRIBUTES);
            meta.itemName(Brand.title("Harvester Hoe"));
            meta.setEnchantmentGlintOverride(true);
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

    /** Whether this hoe sells its harvest on the spot; set per hoe from /hoe, else the config default. */
    static boolean autoSell(ItemStack item, HoeConfig cfg) {
        Byte flag = item.getPersistentDataContainer().get(AUTO_SELL, PersistentDataType.BYTE);
        return flag == null ? cfg.autoSell() : flag != 0;
    }

    static void setAutoSell(ItemStack item, boolean on, HoeConfig cfg) {
        item.editPersistentDataContainer(pdc -> pdc.set(AUTO_SELL, PersistentDataType.BYTE, (byte) (on ? 1 : 0)));
        refreshLore(item, cfg);
    }

    /** The lore shows the levels; it is display only and rebuilt from the data every time. */
    static void refreshLore(ItemStack item, HoeConfig cfg) {
        var lore = new ArrayList<Component>();
        lore.add(Brand.mm("<soft>Harvests <cane>sugar cane</cane>, leaving the roots."));
        lore.add(Component.empty());
        for (var track : Track.values()) {
            int level = level(item, track);
            int max = track.upgrade(cfg).maxLevel();
            lore.add(Brand.mm("<soft>" + track.label + "  " + Brand.bar(level, max)
                    + (level == max ? "  <sun>MAX</sun>" : "  <dim>" + level + "/" + max + "</dim>")));
        }
        lore.add(Component.empty());
        lore.add(Brand.mm(autoSell(item, cfg) ? "<soft>Auto-sell  <money>ON</money>" : "<soft>Auto-sell  <bad>OFF</bad>"));
        lore.add(Brand.mm("<dim>/hoe to upgrade</dim>"));
        item.editMeta(meta -> meta.lore(lore));
    }
}
