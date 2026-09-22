package club.avian.factions.factions;

import club.avian.factions.api.config.ConfigErrors;
import club.avian.factions.api.config.ConfigSpec;
import club.avian.factions.api.config.RequiresRestart;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;

import java.util.List;

/** {@code factions.conf} — every gameplay value here is configurable (spec §2 rule 9). */
@ConfigSerializable
public final class FactionsConfig {

    public static final ConfigSpec<FactionsConfig> SPEC = ConfigSpec.of("factions.conf", FactionsConfig.class)
            .version(1)
            .validate(FactionsConfig::validate)
            .build();

    @RequiresRestart
    @Comment("Current Season. Factions belong to one Season and are archived, never deleted, on reset.")
    private int seasonId = 1;

    private Names names = new Names();
    private club.avian.factions.factions.power.PowerConfig power = new club.avian.factions.factions.power.PowerConfig();

    public int seasonId() {
        return seasonId;
    }

    public Names names() {
        return names;
    }

    public club.avian.factions.factions.power.PowerConfig power() {
        return power;
    }

    static void validate(FactionsConfig cfg, ConfigErrors e) {
        e.check(cfg.seasonId >= 1, "season-id", "must be >= 1 (got %d)", cfg.seasonId);
        e.check(cfg.names.minLength >= 1, "names.min-length", "must be >= 1");
        e.check(cfg.names.maxLength >= cfg.names.minLength, "names.max-length",
                "must be >= names.min-length (%d)", cfg.names.minLength);
        e.check(cfg.names.maxLength <= 16, "names.max-length",
                "must be <= 16 (the database column width)");
        cfg.power.validate(e);
    }

    @ConfigSerializable
    public static final class Names {
        @Comment("Shortest allowed faction name.")
        private int minLength = 3;

        @Comment("Longest allowed faction name. The column holds 16; raising this needs a migration.")
        private int maxLength = 16;

        @Comment("Names players may not take, case-insensitive. Matches the wilderness/zone names\n"
                + "protection messages use, so a faction cannot impersonate them.")
        private List<String> reserved = List.of("wilderness", "warzone", "safezone", "spawn", "admin", "avian");

        @Comment("Substrings that make a name invalid, case-insensitive. Keep it short;\n"
                + "this is a stopgap until a real filter (spec §7 profanity) lands.")
        private List<String> blocked = List.of();

        public int minLength() {
            return minLength;
        }

        public int maxLength() {
            return maxLength;
        }

        public List<String> reserved() {
            return reserved;
        }

        public List<String> blocked() {
            return blocked;
        }
    }
}
