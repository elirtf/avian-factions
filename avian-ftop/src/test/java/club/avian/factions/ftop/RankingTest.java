package club.avian.factions.ftop;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RankingTest {

    private static final long FULL = 72 * 3_600_000L;
    private static final UUID WORLD = UUID.randomUUID();
    private final Valuation valuation = new Valuation(new FTopConfig());

    private static Asset at(int x, AssetKind kind, String type, int count) {
        return new Asset(new AssetKey(WORLD, x, 64, 0), kind, type, count, 0, 0, 0);
    }

    @Test
    void assetsCountForTheFactionWhoseClaimTheySitInAndWildernessCountsForNobody() {
        var ravens = new Ranking.Owner(1, "Ravens");
        var hawks = new Ranking.Owner(2, "Hawks");
        // x 0-9 is the Ravens' claim, 10-19 the Hawks', anything else wilderness.
        var claims = Map.of(0, ravens, 10, hawks);
        var ranking = Ranking.calculate(List.of(
                        at(1, AssetKind.SPAWNER, "ZOMBIE", 2),        // Ravens 500k
                        at(11, AssetKind.SPAWNER, "IRON_GOLEM", 1),   // Hawks 1M
                        at(12, AssetKind.BLOCK, "DIAMOND_BLOCK", 1),  // Hawks 500
                        at(99, AssetKind.SPAWNER, "IRON_GOLEM", 9)),  // wilderness
                key -> claims.get(key.x() / 10 * 10), valuation, FULL);

        assertEquals(2, ranking.entries().size());
        var first = ranking.entries().get(0);
        assertEquals("Hawks", first.owner().tag());
        assertEquals(1_000_500, first.total());
        assertEquals(1_000_000, first.spawnerValue());
        assertEquals(500, first.blockValue());
        assertEquals("DIAMOND_BLOCK x1, IRON_GOLEM x1", first.breakdown());
        assertEquals(2, ranking.entries().get(1).rank());
        assertEquals(500_000, ranking.entries().get(1).total());
    }
}
