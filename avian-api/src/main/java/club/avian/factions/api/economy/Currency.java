package club.avian.factions.api.economy;

/**
 * The three currencies (spec §12). Money is primary; tokens are secondary; gems are premium.
 *
 * <p>Every balance is a whole number of units in a {@code long} — there are no cents. Prices are
 * whole dollars, which suits an OP economy where the cheapest crop sells for several dollars, and
 * it removes a whole class of rounding bug: money in a {@code double} drifts, and a drifting
 * balance is an exploit.
 */
public enum Currency {

    /** {@code $} — shop, spawners, faction upgrades, utilities. The main economy. */
    MONEY("$"),
    /** Custom enchants, special items, crates, keys, progression. */
    TOKENS("tokens"),
    /** Cosmetics and perks. Never required for competitive progression unless configured. */
    GEMS("gems");

    private final String label;

    Currency(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
