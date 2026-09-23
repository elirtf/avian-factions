package club.avian.factions.ftop;

import org.jspecify.annotations.Nullable;

import java.time.Clock;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Holds the latest ranking and rebuilds it. A calculation reads only the in-memory book and the
 * claim index, never chunks (spec §11), so it runs on the main thread in well under a tick.
 */
public final class FTopService {

    private final AssetBook book;
    private final AssetTracker tracker;
    private final AssetRepository repository;
    private final Supplier<Valuation> valuation;
    private final Function<AssetKey, Ranking.@Nullable Owner> ownerOf;
    private final Clock clock;
    private final Logger log;
    private volatile Ranking latest = Ranking.EMPTY;

    public FTopService(AssetBook book, AssetTracker tracker, AssetRepository repository, Supplier<Valuation> valuation,
                       Function<AssetKey, Ranking.@Nullable Owner> ownerOf, Clock clock, Logger log) {
        this.book = book;
        this.tracker = tracker;
        this.repository = repository;
        this.valuation = valuation;
        this.ownerOf = ownerOf;
        this.clock = clock;
        this.log = log;
    }

    public Ranking latest() {
        return latest;
    }

    /** Heals the book against loaded chunks, ranks, and stores the result. Main thread. */
    public Ranking recalculate() {
        tracker.heal();
        var ranking = Ranking.calculate(book.all(), ownerOf, valuation.get(), clock.millis());
        latest = ranking;
        repository.saveResults(ranking).exceptionally(t -> {
            log.log(Level.WARNING, "Could not store the F-Top ranking", t);
            return null;
        });
        return ranking;
    }
}
