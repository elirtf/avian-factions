package club.avian.factions.combat;

import org.bukkit.util.Vector;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The 1.8 formula, checked against the numbers in docs/research/combat-mechanics-on-paper.md. */
class KnockbackMathTest {

    static final CombatConfig.Knockback CLASSIC = new CombatConfig().knockback();  // preset CLASSIC
    static final Vector STILL = new Vector(0, 0, 0);
    static final Vector EAST = new Vector(1, 0, 0);   // attacker west of victim

    /** The event wants a delta; with the victim at rest the delta equals the target velocity. */
    private Vector kb(Vector current, Vector away, float yaw, int bonus, double resistance) {
        return KnockbackMath.knockback(CLASSIC, current, away, yaw, bonus, resistance);
    }

    @Test
    void baseHitPushesAwayAndUpByTheConfiguredAmounts() {
        var v = kb(STILL, EAST, 0, 0, 0);
        assertEquals(0.4, v.getX(), 1e-9);
        assertEquals(0.4, v.getY(), 1e-9);
        assertEquals(0.0, v.getZ(), 1e-9);
    }

    @Test
    void existingVelocityIsHalved() {
        // Vanilla keeps half the victim's speed: target.x = 1.0/2 + 0.4, delta = target - current.
        var v = kb(new Vector(1.0, 0, 0), EAST, 0, 0, 0);
        assertEquals(0.5 + 0.4 - 1.0, v.getX(), 1e-9);
    }

    @Test
    void sprintBonusAddsAlongAttackerYawNotTheAttackerVictimLine() {
        // yaw 0 faces +Z in Minecraft, so a sprint bonus at yaw 0 pushes +Z, not +X.
        var v = kb(STILL, EAST, 0, 1, 0);
        assertEquals(0.4, v.getX(), 1e-9, "base push is unchanged");
        assertEquals(0.5, v.getZ(), 1e-9, "extra-horizontal follows the yaw");
        assertEquals(0.5, v.getY(), 1e-9, "0.4 base + 0.1 extra");
    }

    @Test
    void knockbackEnchantStacksWithSprint() {
        var one = kb(STILL, EAST, 0, 1, 0);
        var three = kb(STILL, EAST, 0, 3, 0);
        assertEquals(one.getZ() * 3, three.getZ(), 1e-9);
        assertEquals(0.4 + 0.3, three.getY(), 1e-9);
    }

    @Test
    void verticalIsClampedByTheLimitBeforeBonuses() {
        // A victim moving upward fast would otherwise be launched: 2.0/2 + 0.4 = 1.4, clamped to 0.4.
        var v = KnockbackMath.knockback(CLASSIC, new Vector(0, 2.0, 0), EAST, 0, 0, 0);
        assertEquals(0.4 - 2.0, v.getY(), 1e-9, "target is the 0.4 limit, delta accounts for current");
    }

    @Test
    void resistanceReducesHorizontalOnlyAndOneMeansImmune() {
        var half = kb(STILL, EAST, 0, 0, 0.5);
        assertEquals(0.2, half.getX(), 1e-9);
        assertEquals(0.4, half.getY(), 1e-9, "vertical is untouched by resistance");

        var immune = kb(STILL, EAST, 0, 0, 1.0);
        assertEquals(0.0, immune.getX(), 1e-9);
    }

    @Test
    void resistanceIsIgnoredWhenTheConfigSaysSo() {
        var ignoring = new CombatConfig.Knockback(0.4, 0.4, 0.4, 0.5, 0.1, false);
        var v = KnockbackMath.knockback(ignoring, STILL, EAST, 0, 0, 1.0);
        assertEquals(0.4, v.getX(), 1e-9, "armour must not matter when resistance is disabled");
    }

    @Test
    void directionIsAUnitVectorAwayFromTheAttacker() {
        var away = KnockbackMath.awayFrom(0, 0, 3, 4);
        assertEquals(0.6, away.getX(), 1e-9);
        assertEquals(0.8, away.getZ(), 1e-9);
        assertEquals(1.0, away.length(), 1e-9);
    }

    @Test
    void sameColumnFallsBackInsteadOfDividingByZero() {
        var away = KnockbackMath.awayFrom(10, 10, 10, 10);
        assertEquals(1.0, away.length(), 1e-9);
        assertTrue(Double.isFinite(away.getX()) && Double.isFinite(away.getZ()));
    }

    @Test
    void competitivePresetIsWeakerThanClassic() {
        var classic = KnockbackMath.knockback(CLASSIC, STILL, EAST, 0, 1, 0);
        var competitive = KnockbackMath.knockback(competitive(), STILL, EAST, 0, 1, 0);
        assertTrue(competitive.getX() < classic.getX(), "competitive base push is lower");
        assertTrue(competitive.getZ() < classic.getZ(), "competitive sprint bonus is lower");
    }

    private static CombatConfig.Knockback competitive() {
        return new CombatConfig.Knockback(0.35, 0.35, 0.4, 0.425, 0.085, true);
    }
}
