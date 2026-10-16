package club.avian.factions.ftop;

import club.avian.factions.api.config.ConfigErrors;
import club.avian.factions.api.config.ConfigSpec;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

/**
 * {@code seasons.conf}: seasons and F-Top payouts (spec §50-53; owner, 2026-10-16). Nothing pays until a season is
 * started with {@code /season start}: the dev server never pays out. While one runs, the top factions are paid every
 * week and, much more, when it ends; each faction's prize is split between all its members.
 */
@ConfigSerializable
public final class SeasonConfig {

    public static final ConfigSpec<SeasonConfig> SPEC = ConfigSpec.of("seasons.conf", SeasonConfig.class)
            .version(1)
            .validate(SeasonConfig::validate)
            .build();

    @Comment("How long a season started with /season start runs, in weeks (spec §53: 2 hardcore, 4, 8 long).")
    private int lengthWeeks = 4;

    @Comment("When the weekly payout happens: day, time and time zone.")
    private String weeklyDay = "SUNDAY";
    private String weeklyTime = "21:00";
    private String timeZone = "America/Chicago";

    @Comment("""
            The weekly payout, by F-Top place (first entry = #1). A faction's money and tokens are split equally
            between its members, online or not; every command runs once per member ({player} is the member,
            {season} the season number), which is how keys are handed out.""")
    private List<Prize> weekly = List.of(
            new Prize(1_000_000, 2_500, List.of("crazycrates give virtual Mythic 1 {player} -s")),
            new Prize(500_000, 1_500, List.of("crazycrates give virtual Epic 1 {player} -s")),
            new Prize(250_000, 750, List.of("crazycrates give virtual Rare 1 {player} -s")));

    @Comment("""
            The season-end payout (about 5x the weekly one), the same way. The champions' tag is
            deluxetags.tag.season<N>champion (plugins/DeluxeTags/config.yml has seasons 1-10).""")
    private List<Prize> seasonEnd = List.of(
            new Prize(5_000_000, 12_500, List.of("crazycrates give virtual Mythic 3 {player} -s",
                    "lp user {player} permission set deluxetags.tag.season{season}champion true")),
            new Prize(2_500_000, 7_500, List.of("crazycrates give virtual Mythic 1 {player} -s",
                    "crazycrates give virtual Epic 2 {player} -s")),
            new Prize(1_250_000, 3_750, List.of("crazycrates give virtual Epic 2 {player} -s")));

    public int lengthWeeks() {
        return lengthWeeks;
    }

    public DayOfWeek weeklyDay() {
        return DayOfWeek.valueOf(weeklyDay.toUpperCase(java.util.Locale.ROOT));
    }

    public LocalTime weeklyTime() {
        return LocalTime.parse(weeklyTime);
    }

    public ZoneId timeZone() {
        return ZoneId.of(timeZone);
    }

    public List<Prize> weekly() {
        return weekly;
    }

    public List<Prize> seasonEnd() {
        return seasonEnd;
    }

    static void validate(SeasonConfig cfg, ConfigErrors e) {
        e.check(cfg.lengthWeeks >= 1 && cfg.lengthWeeks <= 52, "length-weeks", "must be 1-52");
        try {
            cfg.weeklyDay();
        } catch (IllegalArgumentException ex) {
            e.check(false, "weekly-day", "must be MONDAY…SUNDAY");
        }
        try {
            cfg.weeklyTime();
        } catch (RuntimeException ex) {
            e.check(false, "weekly-time", "must be HH:MM");
        }
        try {
            cfg.timeZone();
        } catch (RuntimeException ex) {
            e.check(false, "time-zone", "must be a zone like America/Chicago");
        }
        for (var list : List.of(cfg.weekly, cfg.seasonEnd)) {
            for (var p : list) {
                e.check(p.money >= 0 && p.tokens >= 0, "prizes", "money and tokens must be >= 0");
            }
        }
    }

    /** One place's prize: money and tokens split across the faction, commands run for each member. */
    @ConfigSerializable
    public static final class Prize {

        private long money;
        private long tokens;
        private List<String> commands = List.of();

        public Prize() {
        }

        Prize(long money, long tokens, List<String> commands) {
            this.money = money;
            this.tokens = tokens;
            this.commands = commands;
        }

        public long money() {
            return money;
        }

        public long tokens() {
            return tokens;
        }

        public List<String> commands() {
            return commands;
        }
    }
}
