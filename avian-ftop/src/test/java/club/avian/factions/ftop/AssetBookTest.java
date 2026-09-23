package club.avian.factions.ftop;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AssetBookTest {

    private static final long GRACE = 5 * 60_000L;
    private static final AssetKey KEY = new AssetKey(UUID.randomUUID(), 10, 64, 10);
    private final AssetBook book = new AssetBook(GRACE);

    private static Observed zombies(int n) {
        return new Observed(AssetKind.SPAWNER, "ZOMBIE", n);
    }

    @Test
    void naturalSpawnersAreNeverPickedUpWithoutAPlayerPlacingThem() {
        var change = book.observe(KEY, zombies(1), false, 0);
        assertFalse(change.changed());
        assertFalse(book.isTracked(KEY));
    }

    @Test
    void placementStartsAgingAndGraceNow() {
        book.observe(KEY, zombies(3), true, 1_000);
        var a = book.get(KEY);
        assertEquals(3, a.count());
        assertEquals(1_000, a.placedAt());
        assertEquals(3, a.freeUnits(1_000 + GRACE - 1));
        assertEquals(0, a.freeUnits(1_000 + GRACE));
    }

    @Test
    void stackingMovesThePlacedTimeToTheWeightedMean() {
        book.observe(KEY, zombies(1), true, 0);
        book.observe(KEY, zombies(4), true, 4_000);   // 3 more at t=4000
        assertEquals(3_000, book.get(KEY).placedAt()); // (1*0 + 3*4000) / 4
    }

    @Test
    void graceCoversOnlyUnitsAddedInTheCurrentWindow() {
        book.observe(KEY, zombies(10), true, 0);
        long later = GRACE + 1;                        // first window over
        book.observe(KEY, zombies(12), true, later);   // 2 more
        assertEquals(2, book.get(KEY).freeUnits(later));
    }

    @Test
    void explosionThatDestroysTheWholeStackStopsTrackingIt() {
        book.observe(KEY, zombies(100), true, 0);
        var change = book.observe(KEY, null, false, 1);
        assertEquals(100, change.removedUnits());
        assertNull(book.get(KEY));
    }

    @Test
    void partialLossKeepsAgeAndReportsTheUnitsLost() {
        book.observe(KEY, zombies(8), true, 0);
        var change = book.observe(KEY, zombies(3), false, 50);
        assertEquals(5, change.removedUnits());
        assertEquals(0, book.get(KEY).placedAt());
    }

    @Test
    void aSpawnEggChangingTheTypeRestartsAging() {
        book.observe(KEY, zombies(5), true, 0);
        book.observe(KEY, new Observed(AssetKind.SPAWNER, "IRON_GOLEM", 5), false, 999_999);
        var a = book.get(KEY);
        assertEquals("IRON_GOLEM", a.type());
        assertEquals(999_999, a.placedAt());
    }

    @Test
    void spentGraceIsNotFreeTwice() {
        book.observe(KEY, zombies(4), true, 0);
        book.spendGrace(KEY, 3);
        assertEquals(1, book.get(KEY).freeUnits(1));
    }

    @Test
    void pistonMovesKeepAgeAndDoNotOverwriteARowShiftingByOne() {
        var a = KEY;
        var b = KEY.offset(1, 0, 0);
        var c = KEY.offset(2, 0, 0);
        var diamond = new Observed(AssetKind.BLOCK, "DIAMOND_BLOCK", 1);
        book.observe(a, diamond, true, 100);
        book.observe(b, diamond, true, 200);
        book.move(Map.of(a, b, b, c));
        assertFalse(book.isTracked(a));
        assertEquals(100, book.get(b).placedAt());
        assertEquals(200, book.get(c).placedAt());
        assertTrue(book.isTracked(c));
    }
}
