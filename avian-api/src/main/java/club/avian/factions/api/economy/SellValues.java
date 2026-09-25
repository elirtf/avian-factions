package club.avian.factions.api.economy;

import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemStack;

/**
 * What items are worth (spec §30). One table, read by {@code /shop} sell, sell-all and the
 * Harvester Hoe's autosell, so a price can never disagree with itself between features.
 */
public interface SellValues {

    /** Price of one {@code material} in whole dollars, or 0 when it cannot be sold. */
    long unitPrice(Material material);

    default boolean sellable(Material material) {
        return unitPrice(material) > 0;
    }

    /** Price of a whole stack, before any multiplier. */
    default long price(ItemStack stack) {
        return unitPrice(stack.getType()) * stack.getAmount();
    }

    /**
     * What {@code player} would actually be paid for {@code stack}: the figure a real sale must use.
     * The shop-backed table applies the player's shop multipliers and sell limits; the plain table
     * has none, so it is {@link #price}.
     */
    default long priceFor(OfflinePlayer player, ItemStack stack) {
        return price(stack);
    }

    /**
     * Applies a multiplier to a price without floating-point drift.
     *
     * @param multiplierBasisPoints 10000 = 1×, 15000 = 1.5× — the Harvester Hoe's money
     *     multiplier and any rank or event bonus are expressed this way
     */
    static long applyMultiplier(long price, int multiplierBasisPoints) {
        try {
            return Math.multiplyExact(price, (long) multiplierBasisPoints) / 10_000L;
        } catch (ArithmeticException overflow) {
            // An absurd price or multiplier must saturate, never wrap to a negative payout.
            double scaled = price * (multiplierBasisPoints / 10_000.0);
            return scaled >= Long.MAX_VALUE ? Long.MAX_VALUE : (long) scaled;
        }
    }
}
