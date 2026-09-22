package club.avian.factions.factions;

import club.avian.factions.api.faction.FactionRank;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FactionIndexTest {

    static final Instant T0 = Instant.parse("2026-09-22T12:00:00Z");

    /** Records calls so tests can assert what would be written, without a database. */
    static final class RecordingRepository implements FactionRepository {
        final List<String> calls = new ArrayList<>();
        final Map<UUID, FactionRecord> rows = new HashMap<>();
        List<FactionRecord> season = List.of();

        @Override public List<FactionRecord> loadSeason(int seasonId) {
            return season;
        }

        @Override public CompletableFuture<Void> create(FactionRecord faction) {
            calls.add("create " + faction.name());
            rows.put(faction.id(), faction);
            return CompletableFuture.completedFuture(null);
        }

        @Override public CompletableFuture<Void> addMember(UUID f, UUID p, FactionRank r, Instant at) {
            calls.add("addMember");
            return CompletableFuture.completedFuture(null);
        }

        @Override public CompletableFuture<Void> removeMember(UUID f, UUID p) {
            calls.add("removeMember");
            return CompletableFuture.completedFuture(null);
        }

        @Override public CompletableFuture<Void> delete(UUID factionId) {
            calls.add("delete");
            rows.remove(factionId);
            return CompletableFuture.completedFuture(null);
        }
    }

    final RecordingRepository repo = new RecordingRepository();
    final FactionIndex index = new FactionIndex(repo, Clock.fixed(T0, ZoneOffset.UTC), 1);
    final UUID leader = UUID.randomUUID();

    @Test
    void createRegistersEveryLookupAndPersistsOnce() {
        var created = index.create("Ravens", leader);
        var faction = created.faction();
        assertEquals(1, index.count());
        assertEquals(faction.id(), index.byId(faction.id()).orElseThrow().id());
        assertEquals(faction.id(), index.byName("RAVENS").orElseThrow().id(), "lookup is case-insensitive");
        assertEquals(faction.id(), index.ofPlayer(leader).orElseThrow().id());
        assertEquals(FactionRank.LEADER, faction.rankOf(leader));
        assertEquals(1, faction.seasonId());
        assertEquals(T0, faction.createdAt());
        assertEquals(List.of("create Ravens"), repo.calls);
    }

    @Test
    void duplicateNamesAndDoubleMembershipAreRefused() {
        index.create("Ravens", leader);
        assertTrue(index.nameTaken("ravens"));
        assertThrows(IllegalStateException.class, () -> index.create("ravens", UUID.randomUUID()));
        assertThrows(IllegalStateException.class, () -> index.create("Hawks", leader));
        assertEquals(1, index.count(), "a refused create must not leave a partial entry");
    }

    @Test
    void disbandClearsEveryIndexAndDeletesOnce() {
        var faction = index.create("Ravens", leader).faction();
        index.disband(faction.id()).join();
        assertEquals(0, index.count());
        assertTrue(index.byId(faction.id()).isEmpty());
        assertTrue(index.byName("Ravens").isEmpty());
        assertTrue(index.ofPlayer(leader).isEmpty());
        assertFalse(index.nameTaken("Ravens"), "the name is free again");
        assertEquals(List.of("create Ravens", "delete"), repo.calls);
    }

    @Test
    void disbandingAnUnknownFactionIsANoOp() {
        index.disband(UUID.randomUUID()).join();
        assertEquals(List.of(), repo.calls);
    }

    @Test
    void loadRebuildsEveryLookupFromTheDatabase() {
        var id = UUID.randomUUID();
        var member = UUID.randomUUID();
        repo.season = List.of(new FactionRecord(id, "Ravens", leader,
                Map.of(leader, FactionRank.LEADER, member, FactionRank.MEMBER), 1, T0));
        assertEquals(1, index.load());
        assertEquals(id, index.byName("ravens").orElseThrow().id());
        assertEquals(id, index.ofPlayer(member).orElseThrow().id());
        assertEquals(FactionRank.MEMBER, index.byId(id).orElseThrow().rankOf(member));
    }
}
