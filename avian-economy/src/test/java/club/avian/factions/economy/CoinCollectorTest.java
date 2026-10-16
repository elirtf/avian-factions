package club.avian.factions.economy;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Coin Collector: 20% more mob coins a level, so level V doubles a drop. */
class CoinCollectorTest {

    @Test
    void eachLevelAddsTwentyPercent() {
        assertEquals(new BigDecimal("1.20"), CoinCollector.boosted(BigDecimal.ONE, 1, 20));
        assertEquals(new BigDecimal("2.00"), CoinCollector.boosted(BigDecimal.ONE, 5, 20));
        assertEquals(new BigDecimal("3.20"), CoinCollector.boosted(new BigDecimal("2"), 3, 20));
    }

    @Test
    void noEnchantNoChange() {
        assertEquals(new BigDecimal("2"), CoinCollector.boosted(new BigDecimal("2"), 0, 20));
    }
}
