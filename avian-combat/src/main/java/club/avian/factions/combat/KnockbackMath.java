package club.avian.factions.combat;

import org.bukkit.util.Vector;

/**
 * The 1.8 knockback formula, pure so it can be unit-tested without a server.
 *
 * <p>Paper's event carries a <em>delta</em> added to the victim's current velocity, and that delta
 * already contains vanilla's "halve the existing velocity" friction. Scaling vanilla's vector
 * would therefore scale the friction too, so we recompute the target velocity from scratch and
 * return {@code target - current}.
 */
public final class KnockbackMath {

    private KnockbackMath() {
    }

    /**
     * @param current    the victim's velocity right now
     * @param away       horizontal unit vector pointing from the attacker to the victim
     * @param yaw        the attacker's yaw in degrees; the sprint/enchant bonus follows their facing
     * @param bonusLevel Knockback enchantment level, +1 if the attacker is sprinting
     * @param resistance the victim's knockback-resistance attribute (0 when none, 1 immune)
     * @return the delta to hand to {@code EntityKnockbackEvent#setKnockback}
     */
    public static Vector knockback(CombatConfig.Knockback cfg, Vector current, Vector away,
                                   float yaw, int bonusLevel, double resistance) {
        // Vanilla keeps half the victim's existing velocity, then pushes them away.
        double x = current.getX() / 2 + away.getX() * cfg.horizontal();
        double z = current.getZ() / 2 + away.getZ() * cfg.horizontal();
        double y = Math.min(current.getY() / 2 + cfg.vertical(), cfg.verticalLimit());

        if (bonusLevel > 0) {
            // The sprint/Knockback bonus follows the attacker's yaw, not the attacker-victim line.
            double radians = Math.toRadians(yaw);
            x += -Math.sin(radians) * bonusLevel * cfg.extraHorizontal();
            z += Math.cos(radians) * bonusLevel * cfg.extraHorizontal();
            y += bonusLevel * cfg.extraVertical();
        }

        if (cfg.respectKnockbackResistance() && resistance > 0) {
            // Vanilla applies resistance to horizontal knockback only.
            double factor = Math.max(0, 1 - resistance);
            x *= factor;
            z *= factor;
        }
        return new Vector(x - current.getX(), y - current.getY(), z - current.getZ());
    }

    /**
     * Horizontal unit vector from attacker to victim. Vanilla falls back to an arbitrary direction
     * when the two occupy the same column, which would otherwise divide by zero.
     */
    public static Vector awayFrom(double attackerX, double attackerZ, double victimX, double victimZ) {
        double dx = victimX - attackerX;
        double dz = victimZ - attackerZ;
        double length = Math.sqrt(dx * dx + dz * dz);
        if (length < 1.0E-4) {
            return new Vector(0, 0, 1);
        }
        return new Vector(dx / length, 0, dz / length);
    }
}
