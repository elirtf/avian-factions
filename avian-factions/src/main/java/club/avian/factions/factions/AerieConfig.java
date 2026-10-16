package club.avian.factions.factions;

import club.avian.factions.api.config.ConfigErrors;
import club.avian.factions.api.config.ConfigSpec;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;

import java.util.List;

/**
 * {@code aerie.conf}: the Aerie (#21), a capturable outpost in the warzone. Owner, 2026-10-16: +30% sell bonus,
 * 5 minutes to capture; every hour held a Rare key and 25 tokens, every 2 hours a Common key, for each online
 * member; 8 hours held without losing it earns every member the Aerie chat tag.
 */
@ConfigSerializable
public final class AerieConfig {

    public static final ConfigSpec<AerieConfig> SPEC = ConfigSpec.of("aerie.conf", AerieConfig.class)
            .version(1)
            .validate(AerieConfig::validate)
            .build();

    @Comment("Whether the Aerie runs at all.")
    private boolean enabled = true;

    @Comment("""
            The capture zone: a circle around the centre, in this world, covering the whole nest
            (pad, rim logs and fences), between two heights. It matches the build placed by
            /function avian:aerie/build (dev-server/world/datapacks/avian-aerie); move both together.""")
    private String world = "world";

    private int centerX = 0;
    private int centerZ = 190;
    @Comment("Blocks from the centre that count, measured flat. The nest's outer rim is 10 out.")
    private double radius = 10.5;
    private int minY = 163;
    private int maxY = 172;

    @Comment("Where the floating signs showing the holder hang: over the nest's entrance and at the foot of the stair.")
    private List<Point> signs = List.of(new Point(0.5, 168.0, 199.5), new Point(-12.5, 145.5, 190.5));

    @Comment("Seconds a faction must stand in the zone alone to capture it.")
    private int captureSeconds = 300;

    @Comment("""
            The holding faction's sell bonus: online members get this permission, and EconomyShopGUI's
            sell-multipliers (plugins/EconomyShopGUI/config.yml, entry "aerie") says how big it is (30%).
            /sell, the sell wand and the Harvester Hoe all price through the shop, so it applies to all three.""")
    private String sellBonusPermission = "EconomyShopGUI.sell-multipliers.aerie";

    @Comment("What the holder's sell bonus is, for messages only (the shop config sets the real one).")
    private int sellBonusPercent = 30;

    @Comment("""
            Paid while held, to each online member of the holding faction, every so many minutes after
            the capture. %player% is the member, %faction% the faction.""")
    private List<TimedReward> rewards = List.of(
            new TimedReward(60, "an hourly Rare key and 25 tokens",
                    List.of("crates give virtual Rare 1 %player% -s", "tokens give %player% 25 aerie")),
            new TimedReward(120, "a Common key every 2 hours",
                    List.of("crates give virtual Common 1 %player% -s")));

    @Comment("Hours one faction must hold it without losing it to earn the milestone below. 0 turns it off.")
    private int milestoneHours = 8;

    @Comment("""
            Run once per hold that reaches milestone-hours, for every member of the faction (online or
            not). %player% is the member. The Aerie tag is defined in plugins/DeluxeTags/config.yml.""")
    private List<String> milestoneCommands = List.of("lp user %player% permission set deluxetags.tag.aerie true");

    @Comment("Seconds before the same faction's \"is capturing\" is announced again (stepping in and out).")
    private int announceCooldownSeconds = 60;

    public boolean enabled() {
        return enabled;
    }

    public String world() {
        return world;
    }

    /** Whether a player standing at this spot (their feet) is in the zone. */
    public boolean contains(double x, double y, double z) {
        double dx = x - (centerX + 0.5), dz = z - (centerZ + 0.5);
        return y >= minY && y < maxY + 1 && dx * dx + dz * dz <= radius * radius;
    }

    public int centerX() {
        return centerX;
    }

    public int centerY() {
        return minY;
    }

    public int centerZ() {
        return centerZ;
    }

    public List<Point> signs() {
        return signs;
    }

    public int captureSeconds() {
        return captureSeconds;
    }

    public String sellBonusPermission() {
        return sellBonusPermission;
    }

    public int sellBonusPercent() {
        return sellBonusPercent;
    }

    public List<TimedReward> rewards() {
        return rewards;
    }

    public int milestoneHours() {
        return milestoneHours;
    }

    public List<String> milestoneCommands() {
        return milestoneCommands;
    }

    public int announceCooldownSeconds() {
        return announceCooldownSeconds;
    }

    long[] rewardEveryMillis() {
        return rewards.stream().mapToLong(r -> r.everyMinutes * 60_000L).toArray();
    }

    static void validate(AerieConfig cfg, ConfigErrors e) {
        e.check(cfg.minY <= cfg.maxY, "min-y", "must be at most max-y");
        e.check(cfg.radius > 0 && cfg.radius <= 64, "radius", "must be above 0 and at most 64");
        e.check(cfg.captureSeconds >= 10 && cfg.captureSeconds <= 3600, "capture-seconds", "must be 10-3600");
        e.check(cfg.sellBonusPercent >= 0, "sell-bonus-percent", "must be >= 0");
        e.check(cfg.milestoneHours >= 0, "milestone-hours", "must be >= 0");
        e.check(cfg.announceCooldownSeconds >= 0, "announce-cooldown-seconds", "must be >= 0");
        for (int i = 0; i < cfg.rewards.size(); i++) {
            e.check(cfg.rewards.get(i).everyMinutes >= 1, "rewards[" + i + "].every-minutes", "must be at least 1");
        }
    }

    /** A spot in the Aerie's world. */
    @ConfigSerializable
    public static final class Point {

        private double x;
        private double y;
        private double z;

        public Point() {
        }

        Point(double x, double y, double z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        public double x() {
            return x;
        }

        public double y() {
            return y;
        }

        public double z() {
            return z;
        }
    }

    /** Commands run for each online member every {@code everyMinutes} held. */
    @ConfigSerializable
    public static final class TimedReward {

        private int everyMinutes = 60;

        @Comment("How /aerie describes it.")
        private String description = "";

        private List<String> commands = List.of();

        public TimedReward() {
        }

        TimedReward(int everyMinutes, String description, List<String> commands) {
            this.everyMinutes = everyMinutes;
            this.description = description;
            this.commands = commands;
        }

        public int everyMinutes() {
            return everyMinutes;
        }

        public String description() {
            return description;
        }

        public List<String> commands() {
            return commands;
        }
    }
}
