package club.avian.factions.api.economy;

/** The three currencies (spec §12). Money is primary; tokens are secondary; gems are premium. */
public enum Currency {

    /** {@code $} — shop, spawners, faction upgrades, utilities. The main economy. */
    MONEY("$", 2),
    /** Custom enchants, special items, crates, keys, progression. */
    TOKENS("tokens", 0),
    /** Cosmetics and perks. Never required for competitive progression unless configured. */
    GEMS("gems", 0);

    private final String label;
    private final int decimals;

    Currency(String label, int decimals) {
        this.label = label;
        this.decimals = decimals;
    }

    public String label() {
        return label;
    }

    /**
     * How many decimal places this currency displays. Balances are stored as {@code long} minor
     * units — cents for money, whole units for tokens and gems — because money in a {@code double}
     * drifts, and a drifting balance is an exploit.
     */
    public int decimals() {
        return decimals;
    }

    /** Minor units per whole unit: 100 for money, 1 for tokens and gems. */
    public long minorPerUnit() {
        long factor = 1;
        for (int i = 0; i < decimals; i++) {
            factor *= 10;
        }
        return factor;
    }
}
