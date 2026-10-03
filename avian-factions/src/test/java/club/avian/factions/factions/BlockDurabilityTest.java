package club.avian.factions.factions;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Obsidian takes 3 TNT hits by default; a wall heals after 10 minutes without one. */
class BlockDurabilityTest {

    private static final long TEN_MINUTES = 600_000L;
    private long now = 0;
    private final BlockDurability durability = new BlockDurability(() -> now);
    private final BlockDurability.Key obsidian = new BlockDurability.Key(UUID.randomUUID(), 10, 64, -3);

    @Test
    void theThirdHitBreaksIt() {
        assertFalse(durability.hit(obsidian, 3, TEN_MINUTES));
        assertFalse(durability.hit(obsidian, 3, TEN_MINUTES));
        assertTrue(durability.hit(obsidian, 3, TEN_MINUTES));
        assertEquals(0, durability.hitsOn(obsidian, TEN_MINUTES), "a broken block starts over");
    }

    @Test
    void aWallHealsAfterTheResetTime() {
        durability.hit(obsidian, 3, TEN_MINUTES);
        durability.hit(obsidian, 3, TEN_MINUTES);
        now += TEN_MINUTES;
        assertEquals(0, durability.hitsOn(obsidian, TEN_MINUTES));
        assertFalse(durability.hit(obsidian, 3, TEN_MINUTES), "counting starts again from one");
    }

    @Test
    void hitsInsideTheResetTimeKeepAddingUp() {
        durability.hit(obsidian, 3, TEN_MINUTES);
        now += TEN_MINUTES - 1;
        durability.hit(obsidian, 3, TEN_MINUTES);
        now += TEN_MINUTES - 1;
        assertTrue(durability.hit(obsidian, 3, TEN_MINUTES), "each hit restarts the reset timer");
    }

    @Test
    void zeroResetMeansNeverForgotten() {
        durability.hit(obsidian, 3, 0);
        now += 10 * TEN_MINUTES;
        assertEquals(1, durability.hitsOn(obsidian, 0));
    }

    @Test
    void pruningForgetsOnlyHealedBlocks() {
        var other = new BlockDurability.Key(obsidian.world(), 11, 64, -3);
        durability.hit(obsidian, 3, TEN_MINUTES);
        now += TEN_MINUTES;
        durability.hit(other, 3, TEN_MINUTES);
        durability.prune(TEN_MINUTES);
        assertEquals(1, durability.tracked());
    }
}
