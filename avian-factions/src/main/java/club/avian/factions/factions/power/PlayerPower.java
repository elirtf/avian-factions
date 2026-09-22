package club.avian.factions.factions.power;

import java.time.Instant;
import java.util.UUID;

/**
 * A player's stored power (spec §8). Three fields, and regeneration is derived from the third on
 * read — there is no scheduled task that walks players (#4: every plugin that scans lags).
 *
 * @param value      power as of {@code updatedAt}
 * @param boost      admin adjustment added to the maximum, may be negative
 * @param updatedAt  when {@code value} was last recomputed and stored
 * @param onlineSince when the player logged in, or null when offline — regeneration only accrues
 *                    while online, decay only while offline
 */
public record PlayerPower(UUID player, double value, double boost, Instant updatedAt, Instant onlineSince) {

    public static PlayerPower starting(UUID player, PowerConfig config, Instant now) {
        return new PlayerPower(player, config.starting(), 0, now, null);
    }

    public PlayerPower withValue(double value, Instant at) {
        return new PlayerPower(player, value, boost, at, onlineSince);
    }

    public PlayerPower withOnlineSince(Instant onlineSince) {
        return new PlayerPower(player, value, boost, updatedAt, onlineSince);
    }

    public PlayerPower withBoost(double boost, Instant at) {
        return new PlayerPower(player, value, boost, at, onlineSince);
    }

    public boolean online() {
        return onlineSince != null;
    }
}
