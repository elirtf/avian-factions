package club.avian.factions.factions.claim;

import org.junit.jupiter.api.Test;

import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class ChunkKeyTest {

    @Test
    void roundTripsEveryQuadrantIncludingExtremes() {
        int[] coords = {0, 1, -1, 313, -313, Integer.MAX_VALUE, Integer.MIN_VALUE};
        for (int x : coords) {
            for (int z : coords) {
                long key = ChunkKey.of(x, z);
                assertEquals(x, ChunkKey.x(key), "x round trip for " + x + "," + z);
                assertEquals(z, ChunkKey.z(key), "z round trip for " + x + "," + z);
            }
        }
    }

    @Test
    void swappedCoordinatesAreDifferentKeys() {
        assertNotEquals(ChunkKey.of(1, 2), ChunkKey.of(2, 1));
        assertNotEquals(ChunkKey.of(-1, 1), ChunkKey.of(1, -1));
    }

    @Test
    void keysAreUniqueAcrossANeighbourhood() {
        var seen = new HashSet<Long>();
        for (int x = -50; x <= 50; x++) {
            for (int z = -50; z <= 50; z++) {
                assertEquals(true, seen.add(ChunkKey.of(x, z)), "collision at " + x + "," + z);
            }
        }
        assertEquals(101 * 101, seen.size());
    }
}
