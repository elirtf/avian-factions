package club.avian.factions.economy;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Owner, 2026-10-02: EliteMobs kills pay in our economy, to the player who did the most damage. */
class EliteRewardsTest {

    private final EconomyConfig.EliteRewards rules = new EconomyConfig().eliteRewards();

    @Test
    void anElitePaysMoneyByLevel() {
        var reward = EliteRewards.rewardFor(rules, 12, false);
        assertEquals(12 * 25, reward.money());
        assertEquals(0, reward.tokens());
        assertTrue(reward.commands().isEmpty());
    }

    @Test
    void aBossPaysTheHighestTierItReaches() {
        var low = EliteRewards.rewardFor(rules, 5, true);
        assertEquals(25, low.tokens());
        assertEquals(List.of("crates give virtual Rare 1 %player% -s"), low.commands());
        assertEquals(0, low.money());

        var high = EliteRewards.rewardFor(rules, 40, true);
        assertEquals(75, high.tokens());
        assertEquals(List.of("crates give virtual Epic 1 %player% -s"), high.commands());
    }

    @Test
    void theTopDamagerIsPaid() {
        assertEquals(Optional.of("b"), EliteRewards.topDamager(Map.of("a", 10.0, "b", 30.5, "c", 2.0)));
        assertEquals(Optional.empty(), EliteRewards.topDamager(Map.<String, Double>of()));
    }
}
