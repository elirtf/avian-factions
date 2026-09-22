package club.avian.factions.factions.claim;

/**
 * Packs a chunk coordinate pair into one {@code long} so the claim index can be a primitive map
 * with no key objects allocated per lookup (#4 prior art).
 */
public final class ChunkKey {

    private ChunkKey() {
    }

    public static long of(int chunkX, int chunkZ) {
        return ((long) chunkX << 32) | (chunkZ & 0xFFFFFFFFL);
    }

    public static int x(long key) {
        return (int) (key >> 32);
    }

    public static int z(long key) {
        return (int) key;
    }

    public static String describe(long key) {
        return x(key) + ", " + z(key);
    }
}
