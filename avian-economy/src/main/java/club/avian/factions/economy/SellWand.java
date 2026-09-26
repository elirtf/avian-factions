package club.avian.factions.economy;

import club.avian.factions.api.text.Brand;
import net.kyori.adventure.text.Component;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

/**
 * The sell wand item. Identified by its persistent data, never its name (CLAUDE.md). Its tier and
 * remaining uses live on the item; the multiplier is read from the tier in {@code sellwand.conf}
 * each use, so retuning a tier reaches wands already handed out.
 */
final class SellWand {

    static final NamespacedKey ID = new NamespacedKey("avian", "sell_wand");
    static final NamespacedKey TIER = new NamespacedKey("avian", "sell_wand_tier");
    static final NamespacedKey USES = new NamespacedKey("avian", "sell_wand_uses");
    static final NamespacedKey EARNED = new NamespacedKey("avian", "sell_wand_earned");

    private SellWand() {
    }

    /** A fresh wand of this tier, with its own id. */
    static ItemStack create(String tierId, SellWandConfig.Tier tier) {
        var item = new ItemStack(tier.material());
        item.editPersistentDataContainer(pdc -> {
            pdc.set(ID, PersistentDataType.STRING, UUID.randomUUID().toString());
            pdc.set(TIER, PersistentDataType.STRING, tierId);
            pdc.set(USES, PersistentDataType.INTEGER, tier.uses());
            pdc.set(EARNED, PersistentDataType.LONG, 0L);
        });
        item.editMeta(meta -> {
            meta.itemName(Brand.mm("<bold>" + tier.name() + "</bold>"));
            meta.setEnchantmentGlintOverride(true);
            meta.setMaxStackSize(1);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        });
        refreshLore(item, tier);
        return item;
    }

    static boolean is(ItemStack item) {
        return tierId(item).isPresent();
    }

    /** This wand's tier id, or empty if {@code item} is not a sell wand. */
    static Optional<String> tierId(ItemStack item) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()
                || !item.getPersistentDataContainer().has(ID, PersistentDataType.STRING)) {
            return Optional.empty();
        }
        return Optional.ofNullable(item.getPersistentDataContainer().get(TIER, PersistentDataType.STRING));
    }

    /** Sales left; -1 = unlimited. */
    static int uses(ItemStack item) {
        return item.getPersistentDataContainer().getOrDefault(USES, PersistentDataType.INTEGER, -1);
    }

    static long earned(ItemStack item) {
        return item.getPersistentDataContainer().getOrDefault(EARNED, PersistentDataType.LONG, 0L);
    }

    /**
     * Records one sale on the wand: one use fewer, the money added to its lifetime total.
     *
     * @return false when that was its last use (the caller removes it)
     */
    static boolean recordSale(ItemStack item, long money, SellWandConfig.Tier tier) {
        int uses = uses(item);
        int left = uses < 0 ? -1 : uses - 1;
        item.editPersistentDataContainer(pdc -> {
            pdc.set(USES, PersistentDataType.INTEGER, left);
            pdc.set(EARNED, PersistentDataType.LONG, earned(item) + money);
        });
        refreshLore(item, tier);
        return left != 0;
    }

    /** The lore is display only, rebuilt from the item's data and its tier. */
    static void refreshLore(ItemStack item, SellWandConfig.Tier tier) {
        var number = new DecimalFormat("#,##0");
        int uses = uses(item);
        var lore = new ArrayList<Component>();
        lore.add(Brand.mm("<soft>Sells a whole container at <cane>/shop</cane> prices.</soft>"));
        lore.add(Component.empty());
        lore.add(Brand.mm("<dim>◆</dim> <soft>Multiplier</soft>  <money>×" + multiplierText(tier.multiplier()) + "</money>"));
        lore.add(Brand.mm("<dim>◆</dim> <soft>Uses left</soft>  "
                + (uses < 0 ? "<gem>∞</gem>" : "<white>" + number.format(uses) + "</white>")));
        lore.add(Brand.mm("<dim>◆</dim> <soft>Earned</soft>  <money>$" + number.format(earned(item)) + "</money>"));
        lore.add(Component.empty());
        lore.add(Brand.mm("<cane>▶</cane> <soft>Right-click a container · sell it all</soft>"));
        lore.add(Brand.mm("<cane>▶</cane> <soft>Sneak + right-click · see what it's worth</soft>"));
        item.editMeta(meta -> meta.lore(lore));
    }

    static String multiplierText(int percent) {
        return new DecimalFormat("0.##").format(percent / 100.0);
    }
}
