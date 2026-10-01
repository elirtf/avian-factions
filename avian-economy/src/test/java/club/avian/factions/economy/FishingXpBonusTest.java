package club.avian.factions.economy;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** The fishing skill-XP bonus: catch XP 10, Seasoned Angler +10 % a level. */
class FishingXpBonusTest {

    @Test
    void aSingleCatchEarnsNothingExtra() {
        assertEquals(0, FishingXpBonus.of(10, 1, 0, true, 0.10), 1e-9);
    }

    @Test
    void aDoubledCatchEarnsItsXpAgain() {
        // The owner's case: two Common fish at 60 each should come to 120.
        assertEquals(60, FishingXpBonus.of(60, 2, 0, true, 0.10), 1e-9);
    }

    @Test
    void everyExtraItemCounts() {
        assertEquals(20, FishingXpBonus.of(10, 3, 0, true, 0.10), 1e-9);
    }

    @Test
    void seasonedAnglerAddsItsShareEachLevel() {
        assertEquals(3, FishingXpBonus.of(10, 1, 3, true, 0.10), 1e-9);
    }

    @Test
    void bothStackMultiplicatively() {
        // 10 × 2 × 1.3 = 26, so 16 on top of the 10 already paid.
        assertEquals(16, FishingXpBonus.of(10, 2, 3, true, 0.10), 1e-9);
    }

    @Test
    void multipleItemsCanBeSwitchedOff() {
        assertEquals(0, FishingXpBonus.of(10, 2, 0, false, 0.10), 1e-9);
    }
}
