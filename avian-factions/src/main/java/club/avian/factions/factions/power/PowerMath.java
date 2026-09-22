package club.avian.factions.factions.power;

import java.time.Duration;
import java.time.Instant;

/**
 * Lazy power arithmetic: what a stored {@link PlayerPower} is worth <em>now</em> (spec §8).
 *
 * <p>Pure, so it is unit-testable and so nothing has to run on a timer. A player who has been
 * offline for a month costs exactly one subtraction to evaluate, the same as one who logged off a
 * second ago.
 */
public final class PowerMath {

    private PowerMath() {
    }

    /** The player's power at {@code now}, regenerated or decayed from what was stored. */
    public static double currentValue(PlayerPower power, PowerConfig config, Instant now) {
        double elapsedHours = hoursBetween(power.updatedAt(), now);
        if (elapsedHours <= 0) {
            return clamp(power.value(), power, config);
        }
        double value = power.value();
        if (power.online()) {
            value += elapsedHours * config.regenPerHour();
        } else if (config.offlineDecayPerHour() > 0) {
            double decaying = Math.max(0, elapsedHours - config.offlineDecayGraceHours());
            value -= decaying * config.offlineDecayPerHour();
        }
        return clamp(value, power, config);
    }

    /** The most this player may hold, including any admin boost. */
    public static double maximumFor(PlayerPower power, PowerConfig config) {
        return config.maximum() + power.boost();
    }

    /** Applies the death penalty to the value current at {@code now}. */
    public static PlayerPower onDeath(PlayerPower power, PowerConfig config, Instant now) {
        double after = currentValue(power, config, now) - config.deathLoss();
        return power.withValue(clamp(after, power, config), now);
    }

    /** Freezes the current value into the record — done on login, logout and before any write. */
    public static PlayerPower settle(PlayerPower power, PowerConfig config, Instant now) {
        return power.withValue(currentValue(power, config, now), now);
    }

    /** Claim capacity for a faction holding {@code factionPower} (spec §8). */
    public static int claimCapacity(double factionPower, PowerConfig config) {
        if (factionPower <= 0) {
            return 0;
        }
        return (int) Math.floor(factionPower / config.perClaim());
    }

    private static double clamp(double value, PlayerPower power, PowerConfig config) {
        return Math.clamp(value, config.minimum(), maximumFor(power, config));
    }

    private static double hoursBetween(Instant from, Instant to) {
        return Duration.between(from, to).toMillis() / 3_600_000.0;
    }
}
