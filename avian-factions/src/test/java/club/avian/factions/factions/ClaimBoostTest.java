package club.avian.factions.factions;

import dev.kitteh.factions.Faction;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.util.Map;
import java.util.function.IntFunction;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ClaimBoostTest {

    /** Cumulative totals, as in the default settings. */
    private static final IntFunction<BigDecimal> TOTALS =
            level -> BigDecimal.valueOf(Map.of(1, 10, 2, 30, 3, 70).get(level));

    @Test
    void buyingLevelsAddsTheDifferenceOnTopOfTheBasePower() {
        var faction = new BoostHolder(5.0);   // base power from creation
        ClaimBoost.applyLevelChange(faction.proxy(), 0, 1, TOTALS);
        assertEquals(15.0, faction.boost);
        ClaimBoost.applyLevelChange(faction.proxy(), 1, 3, TOTALS);
        assertEquals(75.0, faction.boost);
    }

    @Test
    void droppingToLevelZeroRemovesOnlyTheUpgradesShare() {
        var faction = new BoostHolder(5.0 + 30.0 + 12.0);   // base, level 2, and an admin's +12
        ClaimBoost.applyLevelChange(faction.proxy(), 2, 0, TOTALS);
        assertEquals(17.0, faction.boost);
    }

    /** A Faction whose only working methods are powerBoost() and powerBoost(double). */
    private static final class BoostHolder {
        double boost;

        BoostHolder(double boost) {
            this.boost = boost;
        }

        Faction proxy() {
            return (Faction) Proxy.newProxyInstance(Faction.class.getClassLoader(), new Class<?>[]{Faction.class},
                    (p, method, args) -> switch (method.getName()) {
                        case "powerBoost" -> {
                            if (args == null) {
                                yield boost;
                            }
                            boost = (Double) args[0];
                            yield null;
                        }
                        default -> throw new UnsupportedOperationException(method.getName());
                    });
        }
    }
}
