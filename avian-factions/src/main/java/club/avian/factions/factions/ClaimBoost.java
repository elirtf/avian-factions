package club.avian.factions.factions;

import dev.kitteh.factions.Faction;
import dev.kitteh.factions.Universe;
import dev.kitteh.factions.upgrade.LeveledValueProvider;
import dev.kitteh.factions.upgrade.Upgrade;
import dev.kitteh.factions.upgrade.UpgradeRegistry;
import dev.kitteh.factions.upgrade.UpgradeSettings;
import dev.kitteh.factions.upgrade.UpgradeVariable;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.IntFunction;

/**
 * Spec §10's Power/Claim Boost as a FactionsUUID upgrade. With FactionsUUID one power is one
 * chunk, and a faction's power is its members' power plus a flat {@code powerBoost}, so each level
 * adds to that boost. Its built-in {@code power_max} only raises a faction-wide cap, which we do not
 * set, so it cannot do this.
 *
 * <p>Levels, power per level and costs live in FactionsUUID's upgrade settings
 * ({@code data/universe.json}), next to every other upgrade; the defaults below apply on first run.
 * The boost moves by the difference between levels, so a boost an admin set by hand survives.
 * Changing a level's power later does not re-price factions that already bought it.
 */
public final class ClaimBoost {

    static final String NAME = "claim_boost";

    /** Total extra power at a level (cumulative, not per step). */
    static final UpgradeVariable POWER = UpgradeVariable.ofInteger("avian_claim_power", BigDecimal.ZERO,
            BigDecimal.valueOf(100_000));

    private ClaimBoost() {
    }

    /** Must run during plugin load: FactionsUUID closes the registry when it enables. */
    static void register() {
        var upgrade = new Upgrade.Reactive(
                NAME,
                Component.text("Claim Boost", NamedTextColor.GOLD),
                Component.text("More power for your faction, so it can hold more land", NamedTextColor.GRAY),
                (settings, level) -> Component.text("+" + settings.valueAt(POWER, level).intValue()
                        + " power (" + settings.valueAt(POWER, level).intValue() + " more chunks)", NamedTextColor.GRAY),
                3,
                Set.of(POWER),
                (faction, oldLevel, newLevel) -> applyLevelChange(faction, oldLevel, newLevel,
                        level -> Universe.universe().upgradeSettings(upgrade()).valueAt(POWER, level)));
        var defaults = new UpgradeSettings(
                upgrade,
                Map.of(POWER, LeveledValueProvider.LevelMap.of(Map.of(
                        1, BigDecimal.valueOf(10), 2, BigDecimal.valueOf(30), 3, BigDecimal.valueOf(70)))),
                3,
                0,
                LeveledValueProvider.LevelMap.of(Map.of(
                        1, BigDecimal.valueOf(100_000), 2, BigDecimal.valueOf(250_000), 3, BigDecimal.valueOf(500_000))));
        UpgradeRegistry.registerVariable(POWER);
        UpgradeRegistry.registerUpgrade(upgrade, defaults, false);
    }

    private static Upgrade upgrade() {
        return Objects.requireNonNull(UpgradeRegistry.getUpgrade(NAME), NAME + " is not registered");
    }

    /** Moves the faction's boost from {@code oldLevel}'s total to {@code newLevel}'s. Level 0 is zero. */
    static void applyLevelChange(Faction faction, int oldLevel, int newLevel, IntFunction<BigDecimal> totalAt) {
        double before = oldLevel <= 0 ? 0 : totalAt.apply(oldLevel).doubleValue();
        double after = newLevel <= 0 ? 0 : totalAt.apply(newLevel).doubleValue();
        faction.powerBoost(faction.powerBoost() - before + after);
    }
}
