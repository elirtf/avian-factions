package club.avian.factions.ftop;

import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;

/** One F-Top calculation: every faction with value, best first. Immutable. */
public record Ranking(List<Entry> entries, Instant calculatedAt) {

    public static final Ranking EMPTY = new Ranking(List.of(), Instant.EPOCH);

    /** The faction that owns a position's claim, for normal (player) factions only. */
    public record Owner(int id, String tag) {
    }

    /**
     * One faction's value.
     *
     * @param units units per type ("ZOMBIE" → 12, "DIAMOND_BLOCK" → 40), for the breakdown
     */
    public record Entry(int rank, Owner owner, long total, long spawnerValue, long blockValue,
                       Map<String, Integer> units) {

        /** "ZOMBIE x12, DIAMOND_BLOCK x40": spec §11's breakdown, largest counts first. */
        public String breakdown() {
            var parts = new ArrayList<String>();
            units.entrySet().stream()
                    .sorted(Map.Entry.<String, Integer>comparingByValue().reversed()
                            .thenComparing(Map.Entry.comparingByKey()))
                    .forEach(e -> parts.add(e.getKey() + " x" + e.getValue()));
            return String.join(", ", parts);
        }
    }

    /**
     * Totals every asset for the faction whose claim it sits in. Assets in wilderness, safezone
     * or warzone ({@code ownerOf} returns null) count for nobody. Ties break by faction id, so the
     * order is stable between runs.
     */
    public static Ranking calculate(Collection<Asset> assets, Function<AssetKey, @Nullable Owner> ownerOf,
                                    Valuation valuation, long now) {
        var totals = new HashMap<Integer, Accumulator>();
        for (Asset asset : assets) {
            Owner owner = ownerOf.apply(asset.key());
            if (owner == null) {
                continue;
            }
            long value = valuation.value(asset, now);
            var acc = totals.computeIfAbsent(owner.id(), id -> new Accumulator(owner));
            if (asset.kind() == AssetKind.SPAWNER) {
                acc.spawners += value;
            } else {
                acc.blocks += value;
            }
            acc.units.merge(asset.type(), asset.count(), Integer::sum);
        }
        var sorted = totals.values().stream()
                .sorted(Comparator.comparingLong(Accumulator::total).reversed()
                        .thenComparingInt(a -> a.owner.id()))
                .toList();
        var entries = new ArrayList<Entry>(sorted.size());
        for (int i = 0; i < sorted.size(); i++) {
            var a = sorted.get(i);
            entries.add(new Entry(i + 1, a.owner, a.total(), a.spawners, a.blocks, Map.copyOf(a.units)));
        }
        return new Ranking(List.copyOf(entries), Instant.ofEpochMilli(now));
    }

    private static final class Accumulator {
        final Owner owner;
        long spawners;
        long blocks;
        final Map<String, Integer> units = new TreeMap<>();

        Accumulator(Owner owner) {
            this.owner = owner;
        }

        long total() {
            return spawners + blocks;
        }
    }
}
