package club.avian.factions.factions;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The capture zone covers the whole nest: the pad, the rim logs and the fences on it (owner, 2026-10-16). */
class AerieConfigTest {

    private final AerieConfig cfg = new AerieConfig();   // the default zone: 0, 190, radius 10.5, y 163-172

    @Test
    void theWholeNestCounts() {
        assertTrue(cfg.contains(0.5, 164, 190.5), "the pad's centre");
        assertTrue(cfg.contains(10.5, 165, 190.5), "standing on the rim, 10 out east");
        assertTrue(cfg.contains(0.5, 168.5, 180.3), "on a fence on the north rim");
        assertTrue(cfg.contains(7.2, 166, 197.4), "a diagonal rim spot");
    }

    @Test
    void theStairAndTheGroundDoNot() {
        assertFalse(cfg.contains(12.5, 164, 190.5), "on the stair beside the nest");
        assertFalse(cfg.contains(0.5, 150, 190.5), "below the pad");
        assertFalse(cfg.contains(0.5, 174, 190.5), "flying high above it");
    }
}
