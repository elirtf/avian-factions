package club.avian.factions.ftop;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Every tracked asset, in memory, keyed by position. It never guesses at deltas from events:
 * callers read what is really at a position and {@link #observe} it, so double-fired events,
 * RoseStacker's explosion rounding and "destroy the rest of the stack" all come out right.
 * Main thread only.
 */
public final class AssetBook {

    /** What an observation did. {@code before} and {@code after} are null when absent. */
    public record Change(@Nullable Asset before, @Nullable Asset after) {
        public boolean changed() {
            return before != after;
        }

        /** Units that disappeared from this position (0 if it grew or was untouched). */
        public int removedUnits() {
            int was = before == null ? 0 : before.count();
            int now = after == null || before == null || !after.type().equals(before.type()) ? 0 : after.count();
            return Math.max(0, was - now);
        }
    }

    private final Map<AssetKey, Asset> assets = new HashMap<>();
    private final long graceMillis;

    public AssetBook(long graceMillis) {
        this.graceMillis = graceMillis;
    }

    public void load(Collection<Asset> loaded) {
        loaded.forEach(a -> assets.put(a.key(), a));
    }

    public @Nullable Asset get(AssetKey key) {
        return assets.get(key);
    }

    public boolean isTracked(AssetKey key) {
        return assets.containsKey(key);
    }

    public Collection<Asset> all() {
        return List.copyOf(assets.values());
    }

    public int size() {
        return assets.size();
    }

    /**
     * Brings one position in line with the world.
     *
     * @param seen      what is there now, or null if nothing we value
     * @param mayCreate whether an untracked position may start being tracked. Only true for
     *                  player placement and stacking, so naturally generated spawners never count
     */
    public Change observe(AssetKey key, @Nullable Observed seen, boolean mayCreate, long now) {
        Asset before = assets.get(key);
        if (seen == null || seen.count() < 1) {
            if (before == null) {
                return new Change(null, null);
            }
            assets.remove(key);
            return new Change(before, null);
        }
        Asset after;
        if (before == null) {
            if (!mayCreate) {
                return new Change(null, null);
            }
            after = fresh(key, seen, now);
        } else if (before.kind() != seen.kind() || !before.type().equals(seen.type())) {
            // A spawn egg changed the spawner's type: aging and grace start over, so a cheap
            // spawner aged for days cannot become an aged expensive one.
            after = fresh(key, seen, now);
        } else if (seen.count() > before.count()) {
            int added = seen.count() - before.count();
            long placedAt = Math.round(((double) before.placedAt() * before.count() + (double) now * added)
                    / seen.count());
            int graceUnits = now < before.graceUntil() ? before.graceUnits() + added : added;
            after = new Asset(key, before.kind(), before.type(), seen.count(), placedAt, now + graceMillis, graceUnits);
        } else if (seen.count() < before.count()) {
            after = new Asset(key, before.kind(), before.type(), seen.count(), before.placedAt(),
                    before.graceUntil(), Math.min(before.graceUnits(), seen.count()));
        } else {
            return new Change(before, before);
        }
        assets.put(key, after);
        return new Change(before, after);
    }

    /** Spends grace on units a player just picked up free, so the same grace is not used twice. */
    public void spendGrace(AssetKey key, int units) {
        Asset a = assets.get(key);
        if (a != null && units > 0) {
            assets.put(key, new Asset(key, a.kind(), a.type(), a.count(), a.placedAt(), a.graceUntil(),
                    Math.max(0, a.graceUnits() - units)));
        }
    }

    /**
     * A piston moved valuable blocks. Assets travel with their block and keep their age. All are
     * lifted before any is set down, so a row of blocks shifting by one never overwrites itself.
     */
    public List<Change> move(Map<AssetKey, AssetKey> fromTo) {
        var lifted = new HashMap<AssetKey, Asset>();
        fromTo.forEach((from, to) -> {
            Asset a = assets.remove(from);
            if (a != null) {
                lifted.put(to, a);
            }
        });
        var changes = new ArrayList<Change>();
        fromTo.forEach((from, to) -> {
            Asset a = lifted.get(to);
            if (a != null) {
                changes.add(new Change(a, null));
            }
        });
        lifted.forEach((to, a) -> {
            var moved = new Asset(to, a.kind(), a.type(), a.count(), a.placedAt(), a.graceUntil(), a.graceUnits());
            assets.put(to, moved);
            changes.add(new Change(null, moved));
        });
        return changes;
    }

    private Asset fresh(AssetKey key, Observed seen, long now) {
        return new Asset(key, seen.kind(), seen.type(), seen.count(), now, now + graceMillis, seen.count());
    }
}
