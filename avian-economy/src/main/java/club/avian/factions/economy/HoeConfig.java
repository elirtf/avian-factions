package club.avian.factions.economy;

import club.avian.factions.api.config.ConfigErrors;
import club.avian.factions.api.config.ConfigSpec;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;

import java.util.ArrayList;
import java.util.List;

/**
 * {@code hoe.conf}: the Harvester Hoe (spec §31). Bought with money, upgraded with tokens through
 * {@code /hoe}. Every cost and effect is here.
 */
@ConfigSerializable
public final class HoeConfig {

    public static final ConfigSpec<HoeConfig> SPEC = ConfigSpec.of("hoe.conf", HoeConfig.class)
            .version(1)
            .validate(HoeConfig::validate)
            .build();

    @Comment("Money to buy a Harvester Hoe from /hoe. 0 = not for sale (admins give it with /hoe give).")
    private long price = 25_000;

    @Comment("""
            true: harvested cane is sold on the spot at /shop prices (times the money multiplier).
            false: it goes to the inventory, and what does not fit drops at the player's feet.""")
    private boolean autoSell = true;

    @Comment("""
            Harvest radius. Level 0 harvests the column that was hit; each level adds a ring, so
            level 1 is 3x3 columns and level 2 is 5x5. The bottom block of every column is left
            standing, so nothing needs replanting. Costs are in tokens, one per level.""")
    private Upgrade radius = new Upgrade(1, List.of(250L, 1_000L));

    @Comment("""
            Token boost: each level multiplies the sugar cane token chance (economy.conf) by a
            further per-level percent. 25 per level at level 4 is double the chance.""")
    private Upgrade tokenBoost = new Upgrade(25, List.of(100L, 250L, 500L, 1_000L, 2_000L));

    @Comment("Money multiplier on auto-sold cane: this many percent more per level.")
    private Upgrade moneyMultiplier = new Upgrade(5, List.of(150L, 400L, 800L, 1_500L, 3_000L));

    @Comment("""
            Random drops: at level 0 there are none. Each level multiplies every drop's chance by its
            level (level 3 = 3x the chances below). Rolled once per grown cane block harvested.""")
    private Upgrade randomDrops = new Upgrade(1, List.of(200L, 600L, 1_500L));

    @Comment("""
            What random drops can give. chance is per grown block at level 1. Each drop gives
            tokens, runs a console command ({player} is replaced by the name), or both. For example
            a crate key: { chance=0.0005, command="crazycrates give virtual feather 1 {player}",
            message="a Feather Crate key" }""")
    private List<Drop> drops = new ArrayList<>(List.of(new Drop(0.002, 25, "", "25 bonus tokens")));

    public long price() {
        return price;
    }

    public boolean autoSell() {
        return autoSell;
    }

    public Upgrade radius() {
        return radius;
    }

    public Upgrade tokenBoost() {
        return tokenBoost;
    }

    public Upgrade moneyMultiplier() {
        return moneyMultiplier;
    }

    public Upgrade randomDrops() {
        return randomDrops;
    }

    public List<Drop> drops() {
        return drops;
    }

    static void validate(HoeConfig cfg, ConfigErrors e) {
        e.check(cfg.price >= 0, "price", "must be >= 0");
        for (var entry : List.of(
                java.util.Map.entry("radius", cfg.radius), java.util.Map.entry("token-boost", cfg.tokenBoost),
                java.util.Map.entry("money-multiplier", cfg.moneyMultiplier),
                java.util.Map.entry("random-drops", cfg.randomDrops))) {
            var u = entry.getValue();
            e.check(u.perLevel >= 0, entry.getKey() + ".per-level", "must be >= 0");
            e.check(u.costs.stream().allMatch(c -> c >= 0), entry.getKey() + ".costs", "must all be >= 0");
        }
        e.check(cfg.radius.costs.size() <= 4, "radius.costs", "at most 4 levels (9x9)");
        for (int i = 0; i < cfg.drops.size(); i++) {
            var d = cfg.drops.get(i);
            e.check(d.chance >= 0 && d.chance <= 1, "drops[" + i + "].chance", "must be 0-1");
            e.check(d.tokens >= 0, "drops[" + i + "].tokens", "must be >= 0");
        }
    }

    /** One upgrade track: {@code costs.size()} buyable levels above 0. */
    @ConfigSerializable
    public static final class Upgrade {

        @Comment("Effect per level (see above for what it means for this upgrade).")
        private int perLevel;

        @Comment("Token cost of each level, in order; the number of entries is the max level.")
        private List<Long> costs = new ArrayList<>();

        public Upgrade() {
        }

        Upgrade(int perLevel, List<Long> costs) {
            this.perLevel = perLevel;
            this.costs = new ArrayList<>(costs);
        }

        public int perLevel() {
            return perLevel;
        }

        public int maxLevel() {
            return costs.size();
        }

        /** Tokens to go from {@code level} to {@code level + 1}; -1 at the max. */
        public long costToUpgradeFrom(int level) {
            return level >= 0 && level < costs.size() ? costs.get(level) : -1;
        }
    }

    @ConfigSerializable
    public static final class Drop {

        private double chance;
        private long tokens;
        private String command = "";
        @Comment("Shown to the player: \"Lucky harvest: <message>!\"")
        private String message = "";

        public Drop() {
        }

        Drop(double chance, long tokens, String command, String message) {
            this.chance = chance;
            this.tokens = tokens;
            this.command = command;
            this.message = message;
        }

        public double chance() {
            return chance;
        }

        public long tokens() {
            return tokens;
        }

        public String command() {
            return command;
        }

        public String message() {
            return message;
        }
    }
}
