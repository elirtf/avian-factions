package club.avian.factions.factions.power;

import club.avian.factions.api.config.ConfigErrors;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;

/** The {@code power} section of {@code factions.conf} (spec §8). */
@ConfigSerializable
public final class PowerConfig {

    @Comment("Power a player starts with on first join.")
    private double starting = 10.0;

    @Comment("Most power a player can hold, before any admin boost.")
    private double maximum = 10.0;

    @Comment("Least power a player can fall to. Negative values let a faction go deep into\n"
            + "raidable territory after a losing fight; 0 caps the damage.")
    private double minimum = 0.0;

    @Comment("Power regained per hour of online time. The spec's example is +0.5 per 30 minutes.")
    private double regenPerHour = 1.0;

    @Comment("Power lost per hour while offline. 0 disables offline decay.")
    private double offlineDecayPerHour = 0.0;

    @Comment("Offline decay does not start until a player has been away this long (hours).")
    private double offlineDecayGraceHours = 24.0;

    @Comment("Power lost on death.")
    private double deathLoss = 2.0;

    @Comment("Power a faction must hold per claimed chunk: capacity = floor(power / per-claim).")
    private double perClaim = 5.0;

    public double starting() {
        return starting;
    }

    public double maximum() {
        return maximum;
    }

    public double minimum() {
        return minimum;
    }

    public double regenPerHour() {
        return regenPerHour;
    }

    public double offlineDecayPerHour() {
        return offlineDecayPerHour;
    }

    public double offlineDecayGraceHours() {
        return offlineDecayGraceHours;
    }

    public double deathLoss() {
        return deathLoss;
    }

    public double perClaim() {
        return perClaim;
    }

    public void validate(ConfigErrors e) {
        e.check(maximum > minimum, "power.maximum", "must be > power.minimum (%s)", minimum);
        e.check(starting >= minimum && starting <= maximum, "power.starting",
                "must be between power.minimum and power.maximum");
        e.check(regenPerHour >= 0, "power.regen-per-hour", "must be >= 0");
        e.check(offlineDecayPerHour >= 0, "power.offline-decay-per-hour", "must be >= 0");
        e.check(offlineDecayGraceHours >= 0, "power.offline-decay-grace-hours", "must be >= 0");
        e.check(deathLoss >= 0, "power.death-loss", "must be >= 0 (it is subtracted)");
        e.check(perClaim > 0, "power.per-claim", "must be > 0 (got %s)", perClaim);
    }
}
