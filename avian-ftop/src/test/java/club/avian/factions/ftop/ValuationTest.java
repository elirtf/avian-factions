package club.avian.factions.ftop;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ValuationTest {

    private static final long HOUR = 3_600_000L;
    private final Valuation valuation = new Valuation(new FTopConfig());   // defaults: 10% → 100% over 72h, 50% pickup
    private static final AssetKey KEY = new AssetKey(UUID.randomUUID(), 0, 0, 0);

    private static Asset zombies(int n, long placedAt, long graceUntil, int graceUnits) {
        return new Asset(KEY, AssetKind.SPAWNER, "ZOMBIE", n, placedAt, graceUntil, graceUnits);
    }

    @Test
    void valueGrowsLinearlyFromTheStartingShareToFull() {
        var a = zombies(2, 0, 0, 0);
        assertEquals(50_000, valuation.value(a, 0));              // 2 × 250k × 10%
        assertEquals(275_000, valuation.value(a, 36 * HOUR));     // halfway: 55%
        assertEquals(500_000, valuation.value(a, 72 * HOUR));
        assertEquals(500_000, valuation.value(a, 500 * HOUR));    // capped
    }

    @Test
    void unlistedTypesAreWorthNothing() {
        var a = new Asset(KEY, AssetKind.SPAWNER, "PIG", 50, 0, 0, 0);
        assertEquals(0, valuation.value(a, 100 * HOUR));
    }

    @Test
    void pickupInsideGraceIsFree() {
        var a = zombies(3, 0, 5 * 60_000L, 3);
        assertEquals(Valuation.Cost.FREE, valuation.pickupCost(a, 3, 60_000));
    }

    @Test
    void pickupAfterGraceCostsHalfTheAgedValuePerSpawner() {
        var a = zombies(3, 0, 0, 0);
        var cost = valuation.pickupCost(a, 2, 72 * HOUR);
        assertEquals(2, cost.chargedUnits());
        assertEquals(250_000, cost.amount());                     // 2 × 250k × 100% × 50%
    }

    @Test
    void onlyUnitsBeyondTheGraceAllowanceAreCharged() {
        var a = zombies(5, 0, Long.MAX_VALUE, 2);                 // 2 still in grace
        assertEquals(3, valuation.pickupCost(a, 5, 72 * HOUR).chargedUnits());
    }

    @Test
    void blocksAreFreeToPickUpByDefault() {
        var a = new Asset(KEY, AssetKind.BLOCK, "DIAMOND_BLOCK", 1, 0, 0, 0);
        assertEquals(Valuation.Cost.FREE, valuation.pickupCost(a, 1, 72 * HOUR));
    }
}
