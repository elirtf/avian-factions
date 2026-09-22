package club.avian.factions.factions.claim;

import club.avian.factions.api.faction.FactionRank;
import club.avian.factions.factions.FactionRecord;
import club.avian.factions.factions.JdbcFactionRepository;
import club.avian.factions.testing.MariaDbExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MariaDbExtension.class)
class JdbcClaimRepositoryIT {

    static final Instant T0 = Instant.parse("2026-09-22T12:00:00.125Z");

    final ClaimRepository repo = new JdbcClaimRepository(MariaDbExtension.database());
    final JdbcFactionRepository factions = new JdbcFactionRepository(MariaDbExtension.database());

    /** Claims are FK-bound to a faction row, so every test needs a real one. */
    private UUID faction() {
        var leader = UUID.randomUUID();
        var record = new FactionRecord(UUID.randomUUID(), "F" + System.nanoTime() % 1000000, leader,
                Map.of(leader, FactionRank.LEADER), 1, T0);
        factions.create(record).join();
        return record.id();
    }

    private Claim claim(String world, int x, int z, UUID factionId) {
        return new Claim(world, x, z, factionId, UUID.randomUUID(), T0);
    }

    @Test
    void savedClaimsRoundTripIncludingNegativeCoordinates() {
        var id = faction();
        var world = "w" + System.nanoTime() % 1000000;
        repo.save(claim(world, 3, 4, id), 1).join();
        repo.save(claim(world, -7, -2, id), 1).join();

        var loaded = repo.loadSeason(1).stream().filter(c -> c.world().equals(world)).toList();
        assertEquals(2, loaded.size());
        assertTrue(loaded.stream().anyMatch(c -> c.chunkX() == -7 && c.chunkZ() == -2));
        assertEquals(T0, loaded.getFirst().claimedAt());
    }

    @Test
    void savingTheSameChunkTwiceTransfersItRatherThanDuplicating() {
        var first = faction();
        var second = faction();
        var world = "w" + System.nanoTime() % 1000000;
        repo.save(claim(world, 0, 0, first), 1).join();
        repo.save(claim(world, 0, 0, second), 1).join();

        var loaded = repo.loadSeason(1).stream().filter(c -> c.world().equals(world)).toList();
        assertEquals(1, loaded.size(), "the chunk key is unique per season");
        assertEquals(second, loaded.getFirst().factionId(), "the later owner wins");
    }

    @Test
    void deleteRemovesOnlyThatChunk() {
        var id = faction();
        var world = "w" + System.nanoTime() % 1000000;
        repo.save(claim(world, 0, 0, id), 1).join();
        repo.save(claim(world, 1, 0, id), 1).join();
        repo.delete(world, 0, 0, 1).join();

        var loaded = repo.loadSeason(1).stream().filter(c -> c.world().equals(world)).toList();
        assertEquals(1, loaded.size());
        assertEquals(1, loaded.getFirst().chunkX());
    }

    @Test
    void deleteByFactionClearsEveryWorld() {
        var id = faction();
        var a = "a" + System.nanoTime() % 1000000;
        var b = "b" + System.nanoTime() % 1000000;
        repo.save(claim(a, 0, 0, id), 1).join();
        repo.save(claim(b, 0, 0, id), 1).join();
        repo.deleteByFaction(id, 1).join();
        assertEquals(0, repo.loadSeason(1).stream().filter(c -> c.factionId().equals(id)).count());
    }

    @Test
    void claimsCascadeWhenTheirFactionIsDeleted() {
        var id = faction();
        var world = "w" + System.nanoTime() % 1000000;
        repo.save(claim(world, 2, 2, id), 1).join();
        factions.delete(id).join();
        assertEquals(0, repo.loadSeason(1).stream().filter(c -> c.factionId().equals(id)).count(),
                "the foreign key must cascade, or the next boot loads orphan claims");
    }

    @Test
    void theSameChunkIsIndependentInAnotherSeason() {
        var id = faction();
        var world = "w" + System.nanoTime() % 1000000;
        repo.save(claim(world, 0, 0, id), 1).join();
        repo.save(claim(world, 0, 0, id), 2).join();
        assertEquals(1, repo.loadSeason(1).stream().filter(c -> c.world().equals(world)).count());
        assertEquals(1, repo.loadSeason(2).stream().filter(c -> c.world().equals(world)).count());
    }
}
