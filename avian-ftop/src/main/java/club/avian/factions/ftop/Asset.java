package club.avian.factions.ftop;

/**
 * One tracked spawner stack or valuable block.
 *
 * @param placedAt   epoch ms the stack was placed, averaged over its units: adding {@code n} units
 *                   at {@code t} moves it to the count-weighted mean. Aging is linear, so this gives
 *                   the same total as tracking every unit, until units reach full value
 * @param graceUntil epoch ms until which {@code graceUnits} can be picked up without the cost
 * @param graceUnits units added in the current grace window
 */
public record Asset(AssetKey key, AssetKind kind, String type, int count, long placedAt,
                    long graceUntil, int graceUnits) {

    /** Units that can be picked up free at {@code now}. */
    public int freeUnits(long now) {
        return now < graceUntil ? Math.min(graceUnits, count) : 0;
    }
}
