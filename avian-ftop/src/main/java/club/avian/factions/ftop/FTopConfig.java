package club.avian.factions.ftop;

import club.avian.factions.api.config.ConfigErrors;
import club.avian.factions.api.config.ConfigSpec;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/** {@code ftop.conf}: what a faction's base is worth (spec §11). Money is whole dollars. */
@ConfigSerializable
public final class FTopConfig {

    public static final ConfigSpec<FTopConfig> SPEC = ConfigSpec.of("ftop.conf", FTopConfig.class)
            .version(1)
            .validate(FTopConfig::validate)
            .build();

    @Comment("How often the ranking is recalculated, in minutes. /ftop recalc forces one now.")
    private int recalculateMinutes = 5;

    @Comment("How many factions /ftop lists per page.")
    private int pageSize = 10;

    @Comment("Value of one spawner, by the mob it spawns. A spawner type not listed is worth 0.")
    private Map<String, Long> spawnerValues = defaultSpawnerValues();

    @Comment("Value of one placed block, by material. Only these blocks are tracked at all.")
    private Map<String, Long> blockValues = defaultBlockValues();

    private Aging aging = new Aging();
    private PickupCost pickupCost = new PickupCost();

    private static Map<String, Long> defaultSpawnerValues() {
        // Spec §11's example values; §28's harder spawners sit above them.
        var values = new LinkedHashMap<String, Long>();
        values.put("ZOMBIE", 250_000L);
        values.put("SPIDER", 250_000L);
        values.put("SKELETON", 300_000L);
        values.put("BLAZE", 400_000L);
        values.put("CREEPER", 500_000L);
        values.put("ENDERMAN", 600_000L);
        values.put("IRON_GOLEM", 1_000_000L);
        return values;
    }

    private static Map<String, Long> defaultBlockValues() {
        var values = new LinkedHashMap<String, Long>();
        values.put("GOLD_BLOCK", 250L);
        values.put("DIAMOND_BLOCK", 500L);
        values.put("EMERALD_BLOCK", 750L);
        return values;
    }

    public int recalculateMinutes() {
        return recalculateMinutes;
    }

    public int pageSize() {
        return pageSize;
    }

    public Map<String, Long> spawnerValues() {
        return spawnerValues;
    }

    public Map<String, Long> blockValues() {
        return blockValues;
    }

    public Aging aging() {
        return aging;
    }

    public PickupCost pickupCost() {
        return pickupCost;
    }

    @ConfigSerializable
    public static final class Aging {
        @Comment("A newly placed spawner or block starts at part of its value and grows to full, so a\n"
                + "faction cannot buy its way up F-Top the night before a payout. false = full value at once.")
        private boolean enabled = true;

        @Comment("Percent of full value on placement.")
        private double startingPercent = 10.0;

        @Comment("Hours until full value.")
        private double hoursToFullValue = 72.0;

        public boolean enabled() {
            return enabled;
        }

        public double startingPercent() {
            return startingPercent;
        }

        public double hoursToFullValue() {
            return hoursToFullValue;
        }
    }

    @ConfigSerializable
    public static final class PickupCost {
        @Comment("Breaking your own spawners costs money once the grace period is over, so a faction\n"
                + "cannot mine its spawners up to hide them from a raid or an F-Top check.\n"
                + "Explosions never pay it: raids take spawners for free.")
        private boolean enabled = true;

        @Comment("Minutes after placing during which spawners can be picked up free.")
        private int graceMinutes = 5;

        @Comment("Cost per spawner, as a percent of its current (aged) value.")
        private double percentOfValue = 50.0;

        @Comment("Also charge for picking up valuable blocks. Off: blocks are for building.")
        private boolean appliesToBlocks = false;

        @Comment("Players with this permission never pay (staff).")
        private String bypassPermission = "avian.ftop.pickup-cost.bypass";

        public boolean enabled() {
            return enabled;
        }

        public int graceMinutes() {
            return graceMinutes;
        }

        public double percentOfValue() {
            return percentOfValue;
        }

        public boolean appliesToBlocks() {
            return appliesToBlocks;
        }

        public String bypassPermission() {
            return bypassPermission;
        }
    }

    static void validate(FTopConfig cfg, ConfigErrors e) {
        e.check(cfg.recalculateMinutes >= 1, "recalculate-minutes", "must be >= 1 (got %d)", cfg.recalculateMinutes);
        e.check(cfg.pageSize >= 1 && cfg.pageSize <= 45, "page-size", "must be 1-45 (got %d)", cfg.pageSize);
        cfg.spawnerValues.forEach((type, value) -> e.check(value != null && value >= 0,
                "spawner-values." + type, "must be >= 0 (got %s)", value));
        cfg.blockValues.forEach((type, value) -> e.check(value != null && value >= 0,
                "block-values." + type, "must be >= 0 (got %s)", value));
        cfg.blockValues.keySet().forEach(type -> e.check(org.bukkit.Material.matchMaterial(type) != null,
                "block-values." + type, "is not a block material"));
        e.check(cfg.aging.startingPercent >= 0 && cfg.aging.startingPercent <= 100, "aging.starting-percent",
                "must be 0-100 (got %s)", cfg.aging.startingPercent);
        e.check(cfg.aging.hoursToFullValue > 0, "aging.hours-to-full-value", "must be > 0 (got %s)",
                cfg.aging.hoursToFullValue);
        e.check(cfg.pickupCost.graceMinutes >= 0, "pickup-cost.grace-minutes", "must be >= 0 (got %d)",
                cfg.pickupCost.graceMinutes);
        e.check(cfg.pickupCost.percentOfValue >= 0, "pickup-cost.percent-of-value", "must be >= 0 (got %s)",
                cfg.pickupCost.percentOfValue);
    }

    /** Values keyed upper-case, whatever case the file used. */
    Map<String, Long> normalised(Map<String, Long> values) {
        var out = new LinkedHashMap<String, Long>();
        values.forEach((k, v) -> out.put(k.toUpperCase(Locale.ROOT), v));
        return out;
    }
}
