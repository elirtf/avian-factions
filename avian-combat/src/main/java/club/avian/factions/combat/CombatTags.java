package club.avian.factions.combat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.LongSupplier;

/**
 * Who is in combat and until when (spec §43). A tag starts, or restarts at full length, on every
 * player-vs-player hit, for the attacker and the victim alike.
 */
final class CombatTags {

    private final LongSupplier clock;
    private final Map<UUID, Long> until = new HashMap<>();

    CombatTags(LongSupplier clockMillis) {
        this.clock = clockMillis;
    }

    /** Tags (or re-tags) a player; true when they were not already in combat. */
    boolean tag(UUID player, int seconds) {
        boolean fresh = !isTagged(player);
        until.put(player, clock.getAsLong() + seconds * 1000L);
        return fresh;
    }

    boolean isTagged(UUID player) {
        return remainingMillis(player) > 0;
    }

    long remainingMillis(UUID player) {
        var end = until.get(player);
        return end == null ? 0 : Math.max(0, end - clock.getAsLong());
    }

    void clear(UUID player) {
        until.remove(player);
    }

    /** Players whose tag is still running. */
    List<UUID> tagged() {
        return until.keySet().stream().filter(this::isTagged).toList();
    }

    /** Forgets every tag that has run out and returns whose they were. */
    List<UUID> expire() {
        long now = clock.getAsLong();
        var done = until.entrySet().stream().filter(e -> e.getValue() <= now).map(Map.Entry::getKey).toList();
        done.forEach(until::remove);
        return done;
    }
}
