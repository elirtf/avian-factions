package club.avian.factions.combat;

import club.avian.factions.api.config.ConfigErrors;
import club.avian.factions.api.config.ConfigSpec;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;

/**
 * {@code combat.conf} — 1.8-style combat feel (spec §44).
 *
 * <p>Values and their meaning come from `docs/research/combat-mechanics-on-paper.md`. The
 * knockback model is 1.8's additive one: a base push plus a per-bonus-level extra along the
 * attacker's yaw, where bonus = Knockback enchant level + 1 while sprinting.
 */
@ConfigSerializable
public final class CombatConfig {

    public static final ConfigSpec<CombatConfig> SPEC = ConfigSpec.of("combat.conf", CombatConfig.class)
            .version(1)
            .validate(CombatConfig::validate)
            .build();

    /** Named value sets; `custom` uses whatever is written in the file. */
    public enum Preset {
        CLASSIC, COMPETITIVE, CUSTOM
    }

    @Comment("""
            CLASSIC     — vanilla 1.8 numbers (0.4 / 0.4, extra 0.5 / 0.1).
            COMPETITIVE — the "practice server" feel: lower base, slightly lower extra.
            CUSTOM      — use the knockback values written below, unchanged.
            CLASSIC and COMPETITIVE overwrite the knockback block at load; edit it under CUSTOM.""")
    private Preset preset = Preset.CLASSIC;

    @Comment("Remove the 1.9 attack cooldown by raising the attack-speed attribute.")
    private boolean disableAttackCooldown = true;

    @Comment("Attack-speed attribute applied to players. 40 is the plugin-ecosystem convention\n"
            + "(effectively instant); the vanilla value is 4.")
    private double attackSpeed = 40.0;

    @Comment("Cancel sweep-attack damage to bystanders (1.9+ mechanic, absent from 1.8 PvP).")
    private boolean disableSweepAttack = true;

    @Comment("""
            How often a hit can land on the same player. A new hit counts once HALF of this many
            ticks have passed (20 ticks = 1 second):
              20 = every 10 ticks (vanilla)   18 = every 9   16 = every 8   14 = every 7
            Lower = faster combos; much below 14 starts to feel spammy. 0 = vanilla (20).""")
    private int hitDelayTicks = 0;

    private Knockback knockback = new Knockback();

    public Preset preset() {
        return preset;
    }

    public boolean disableAttackCooldown() {
        return disableAttackCooldown;
    }

    public double attackSpeed() {
        return attackSpeed;
    }

    public boolean disableSweepAttack() {
        return disableSweepAttack;
    }

    public int hitDelayTicks() {
        return hitDelayTicks;
    }

    /** The knockback values in effect: the preset's, unless the preset is {@code CUSTOM}. */
    public Knockback knockback() {
        return switch (preset) {
            case CLASSIC -> Knockback.CLASSIC;
            case COMPETITIVE -> Knockback.COMPETITIVE;
            case CUSTOM -> knockback;
        };
    }

    static void validate(CombatConfig cfg, ConfigErrors e) {
        e.check(cfg.attackSpeed > 0, "attack-speed", "must be > 0 (got %s)", cfg.attackSpeed);
        e.check(cfg.hitDelayTicks >= 0 && cfg.hitDelayTicks <= 60, "hit-delay-ticks", "must be 0-60");
        var kb = cfg.knockback;
        e.check(kb.horizontal >= 0, "knockback.horizontal", "must be >= 0");
        e.check(kb.vertical >= 0, "knockback.vertical", "must be >= 0");
        e.check(kb.verticalLimit >= kb.vertical, "knockback.vertical-limit",
                "must be >= knockback.vertical (%s)", kb.vertical);
        e.check(kb.extraHorizontal >= 0, "knockback.extra-horizontal", "must be >= 0");
        e.check(kb.extraVertical >= 0, "knockback.extra-vertical", "must be >= 0");
    }

    @ConfigSerializable
    public static final class Knockback {

        /** Vanilla 1.8. */
        static final Knockback CLASSIC = new Knockback(0.4, 0.4, 0.4, 0.5, 0.1, true);
        /** OCM's "practice server" numbers: a little tighter than 1.8. */
        static final Knockback COMPETITIVE = new Knockback(0.35, 0.35, 0.4, 0.425, 0.085, true);

        @Comment("Base horizontal push, away from the attacker.")
        private double horizontal = 0.4;

        @Comment("Base upward push.")
        private double vertical = 0.4;

        @Comment("Upward push is clamped to this. Stops stacked hits launching a player.")
        private double verticalLimit = 0.4;

        @Comment("Extra horizontal per bonus level, along the attacker's yaw.\n"
                + "Bonus = Knockback enchant level + 1 while sprinting.")
        private double extraHorizontal = 0.5;

        @Comment("Extra upward per bonus level.")
        private double extraVertical = 0.1;

        @Comment("Whether the knockback-resistance attribute (netherite) still reduces knockback.\n"
                + "false makes armour irrelevant to how far a player is pushed.")
        private boolean respectKnockbackResistance = true;

        public Knockback() {
        }

        Knockback(double horizontal, double vertical, double verticalLimit,
                  double extraHorizontal, double extraVertical, boolean respectKnockbackResistance) {
            this.horizontal = horizontal;
            this.vertical = vertical;
            this.verticalLimit = verticalLimit;
            this.extraHorizontal = extraHorizontal;
            this.extraVertical = extraVertical;
            this.respectKnockbackResistance = respectKnockbackResistance;
        }

        public double horizontal() {
            return horizontal;
        }

        public double vertical() {
            return vertical;
        }

        public double verticalLimit() {
            return verticalLimit;
        }

        public double extraHorizontal() {
            return extraHorizontal;
        }

        public double extraVertical() {
            return extraVertical;
        }

        public boolean respectKnockbackResistance() {
            return respectKnockbackResistance;
        }
    }
}
