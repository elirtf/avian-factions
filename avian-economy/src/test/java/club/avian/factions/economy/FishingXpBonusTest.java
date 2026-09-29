package club.avian.factions.economy;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** The fishing skill-XP bonus: catch XP 10, Seasoned Angler +10 % a level. */
class FishingXpBonusTest {

    @Test
    void aDoubledCatchEarnsItsXpAgain() {
        assertEquals(10, FishingXpBonus.of(10, true, 0, true, 0.10), 1e-9);
    }

    @Test
    void seasonedAnglerAddsItsShareEachLevel() {
        assertEquals(3, FishingXpBonus.of(10, false, 3, true, 0.10), 1e-9);
    }

    @Test
    void bothStackMultiplicatively() {
        // 10 × 2 × 1.3 = 26, so 16 on top of the 10 already paid.
        assertEquals(16, FishingXpBonus.of(10, true, 3, true, 0.10), 1e-9);
    }

    @Test
    void doubleCatchCanBeSwitchedOff() {
        assertEquals(0, FishingXpBonus.of(10, true, 0, false, 0.10), 1e-9);
    }
}
