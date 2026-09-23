package club.avian.factions.ftop;

import club.avian.factions.testing.MariaDbExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MariaDbExtension.class)
class AssetRepositoryIT {

    private final AssetRepository repo = new AssetRepository(MariaDbExtension.database());

    @Test
    void assetsRoundTripAndTheLastWriteForAPositionWins() {
        var key = new AssetKey(UUID.randomUUID(), 5, 70, -3);
        var first = new Asset(key, AssetKind.SPAWNER, "ZOMBIE", 4, 1_000, 2_000, 4);
        var grown = new Asset(key, AssetKind.SPAWNER, "ZOMBIE", 10, 1_600, 9_000, 6);
        repo.save(first);
        repo.save(grown).join();

        var loaded = repo.loadAllDuringBoot().stream().filter(a -> a.key().equals(key)).toList();
        assertEquals(List.of(grown), loaded);
    }

    @Test
    void aStackBlownUpAndReplacedInOneTickEndsUpPresent() {
        var key = new AssetKey(UUID.randomUUID(), 1, 64, 1);
        var asset = new Asset(key, AssetKind.SPAWNER, "BLAZE", 1, 0, 0, 0);
        repo.save(asset);
        repo.delete(key);
        repo.save(asset).join();

        assertTrue(repo.loadAllDuringBoot().contains(asset));
    }

    @Test
    void resultsAreReplacedWhole() {
        var owner = new Ranking.Owner(7, "Ravens");
        var entry = new Ranking.Entry(1, owner, 500, 0, 500, Map.of("DIAMOND_BLOCK", 1));
        repo.saveResults(new Ranking(List.of(entry, new Ranking.Entry(2, new Ranking.Owner(8, "Hawks"), 1, 0, 1, Map.of())),
                Instant.now())).join();
        repo.saveResults(new Ranking(List.of(entry), Instant.now())).join();

        var rows = MariaDbExtension.database().query(c -> {
            try (var st = c.createStatement(); var rs = st.executeQuery("SELECT faction_tag, breakdown FROM ftop_results")) {
                var out = new java.util.ArrayList<String>();
                while (rs.next()) {
                    out.add(rs.getString(1) + ": " + rs.getString(2));
                }
                return out;
            }
        }).join();
        assertEquals(List.of("Ravens: DIAMOND_BLOCK x1"), rows);
    }
}
