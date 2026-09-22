package club.avian.factions.economy;

import club.avian.factions.api.economy.SellValues;
import me.gypopo.economyshopgui.api.EconomyShopGUIHook;
import me.gypopo.economyshopgui.util.EcoType;
import me.gypopo.economyshopgui.util.EconomyType;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemStack;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Sell prices read from EconomyShopGUI, so the shop and the Harvester Hoe's autosell can never
 * disagree about what something is worth.
 *
 * <p>Two price lists would be an exploit rather than an inconsistency: whichever paid more is the
 * one every player would use. The shop owns prices because that is where an admin edits them and
 * where players see them. {@code economy.conf}'s table remains the fallback for a server running
 * without the shop plugin, and for any lookup the shop cannot answer.
 *
 * <p>The player-aware call is the one that matters: EconomyShopGUI applies per-player multipliers
 * and sell limits, and an autosell that ignored them would pay the wrong amount.
 */
public final class ShopSellValues implements SellValues {

    /** Money, as EconomyShopGUI names it — our balances reach it through the Vault bridge. */
    private static final EcoType VAULT = new EcoType(EconomyType.VAULT);

    private final SellValues fallback;
    private final Logger log;
    private volatile boolean warned;

    public ShopSellValues(SellValues fallback, Logger log) {
        this.fallback = fallback;
        this.log = log;
    }

    /**
     * Player-agnostic price, for display and estimates. Per-player multipliers cannot be applied
     * without a player, so this deliberately uses our own table; {@link #priceFor} is the figure
     * an actual sale must use.
     */
    @Override
    public long unitPrice(Material material) {
        return fallback.unitPrice(material);
    }

    /**
     * What this player would actually be paid for {@code stack}, including their multipliers and
     * any sell limit they have reached. Falls back to the configured table if the shop cannot
     * answer, so a pricing failure never breaks a harvest.
     */
    public long priceFor(OfflinePlayer player, ItemStack stack) {
        if (stack == null || stack.getType().isAir()) {
            return 0;
        }
        try {
            var price = EconomyShopGUIHook.getSellPrice(player, stack);
            if (price.isEmpty()) {
                return 0;   // the shop does not buy this
            }
            // Prices are whole dollars for us; the shop speaks doubles.
            return Math.round(price.get().getPrice(VAULT));
        } catch (RuntimeException | LinkageError e) {
            if (!warned) {
                warned = true;
                log.log(Level.WARNING, "EconomyShopGUI price lookup failed; using economy.conf prices", e);
            }
            return fallback.price(stack);
        }
    }
}
