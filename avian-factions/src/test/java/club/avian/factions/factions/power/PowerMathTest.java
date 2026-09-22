package club.avian.factions.factions.power;

import club.avian.factions.factions.FactionsConfig;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Spec §8: power regenerates and decays lazily, clamped, with a death penalty. */
class PowerMathTest {

    static final Instant T0 = Instant.parse("2026-09-22T12:00:00Z");
    static final UUID PLAYER = UUID.randomUUID();
    final PowerConfig config = new FactionsConfig().power();   // 10 start/max, 0 min, +1/h, -2 death

    private PlayerPower online(double value, Instant updatedAt) {
        return new PlayerPower(PLAYER, value, 0, updatedAt, updatedAt);
    }

    private PlayerPower offline(double value, Instant updatedAt) {
        return new PlayerPower(PLAYER, value, 0, updatedAt, null);
    }

    private static Instant after(Duration d) {
        return T0.plus(d);
    }

    @Test
    void onlinePowerRegeneratesWithElapsedTime() {
        var power = online(5, T0);
        assertEquals(5.0, PowerMath.currentValue(power, config, T0), 1e-9);
        assertEquals(5.5, PowerMath.currentValue(power, config, after(Duration.ofMinutes(30))), 1e-9);
        assertEquals(6.0, PowerMath.currentValue(power, config, after(Duration.ofHours(1))), 1e-9);
    }

    @Test
    void regenerationStopsAtTheMaximum() {
        var power = online(5, T0);
        assertEquals(10.0, PowerMath.currentValue(power, config, after(Duration.ofDays(30))), 1e-9,
                "a month online must not exceed the cap");
    }

    @Test
    void anAdminBoostRaisesTheCeiling() {
        var boosted = new PlayerPower(PLAYER, 5, 5, T0, T0);
        assertEquals(15.0, PowerMath.maximumFor(boosted, config), 1e-9);
        assertEquals(15.0, PowerMath.currentValue(boosted, config, after(Duration.ofDays(30))), 1e-9);
    }

    @Test
    void offlinePlayersDoNotRegenerate() {
        assertEquals(5.0, PowerMath.currentValue(offline(5, T0), config, after(Duration.ofHours(10))), 1e-9);
    }

    @Test
    void offlineDecayRespectsTheGracePeriodWhenEnabled() {
        var decaying = new ConfigWithDecay(1.0, 24.0).config();
        var power = offline(10, T0);
        assertEquals(10.0, PowerMath.currentValue(power, decaying, after(Duration.ofHours(24))), 1e-9,
                "nothing decays inside the grace window");
        assertEquals(9.0, PowerMath.currentValue(power, decaying, after(Duration.ofHours(25))), 1e-9);
        assertEquals(0.0, PowerMath.currentValue(power, decaying, after(Duration.ofDays(365))), 1e-9,
                "decay stops at the minimum");
    }

    @Test
    void deathSubtractsFromTheValueCurrentAtThatMoment() {
        // Regenerated to 6.0 after an hour, then -2 for the death.
        var after = PowerMath.onDeath(online(5, T0), config, after(Duration.ofHours(1)));
        assertEquals(4.0, after.value(), 1e-9);
        assertEquals(after(Duration.ofHours(1)), after.updatedAt(), "the timestamp moves with the write");
    }

    @Test
    void deathCannotPushPowerBelowTheMinimum() {
        assertEquals(0.0, PowerMath.onDeath(online(1, T0), config, T0).value(), 1e-9);
    }

    @Test
    void settleFreezesTheCurrentValueAndTimestamp() {
        var settled = PowerMath.settle(online(5, T0), config, after(Duration.ofHours(2)));
        assertEquals(7.0, settled.value(), 1e-9);
        assertEquals(after(Duration.ofHours(2)), settled.updatedAt());
        // Settling twice must not regenerate twice.
        assertEquals(7.0, PowerMath.currentValue(settled, config, after(Duration.ofHours(2))), 1e-9);
    }

    @Test
    void claimCapacityIsFlooredAndNeverNegative() {
        assertEquals(16, PowerMath.claimCapacity(80, config));   // spec §8 worked example
        assertEquals(2, PowerMath.claimCapacity(14, config));     // 14/5 = 2.8 → 2
        assertEquals(0, PowerMath.claimCapacity(0, config));
        assertEquals(0, PowerMath.claimCapacity(-50, config), "negative power grants no claims");
    }

    @Test
    void aClockThatGoesBackwardsDoesNotGrantPower() {
        assertEquals(5.0, PowerMath.currentValue(online(5, T0), config, T0.minus(Duration.ofHours(5))), 1e-9);
    }

    /** Builds a PowerConfig with decay enabled by round-tripping through the real config class. */
    private record ConfigWithDecay(double perHour, double graceHours) {
        PowerConfig config() {
            var cfg = new PowerConfig();
            set(cfg, "offlineDecayPerHour", perHour);
            set(cfg, "offlineDecayGraceHours", graceHours);
            return cfg;
        }

        private static void set(PowerConfig cfg, String field, double value) {
            try {
                var f = PowerConfig.class.getDeclaredField(field);
                f.setAccessible(true);
                f.setDouble(cfg, value);
            } catch (ReflectiveOperationException e) {
                throw new AssertionError("PowerConfig field renamed: " + field, e);
            }
        }
    }
}
