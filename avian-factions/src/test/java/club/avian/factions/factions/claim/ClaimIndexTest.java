package club.avian.factions.factions.claim;

import club.avian.factions.api.faction.FactionRank;
import club.avian.factions.api.faction.Territory;
import club.avian.factions.factions.FactionIndex;
import club.avian.factions.factions.FactionRecord;
import club.avian.factions.factions.FactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Spec §9 plus the #4 invariants: the two indexes must agree after every operation. */
class ClaimIndexTest {

    static final Instant T0 = Instant.parse("2026-09-22T12:00:00Z");
    static final String WORLD = "world";
    static final int CAPACITY = 3;

    static final class MemoryClaims implements ClaimRepository {
        final List<String> calls = new ArrayList<>();
        List<Claim> season = List.of();

        @Override public List<Claim> loadSeason(int seasonId) {
            return season;
        }

        @Override public CompletableFuture<Void> save(Claim claim, int seasonId) {
            calls.add("save " + claim.chunkX() + "," + claim.chunkZ());
            return CompletableFuture.completedFuture(null);
        }

        @Override public CompletableFuture<Void> delete(String world, int x, int z, int seasonId) {
            calls.add("delete " + x + "," + z);
            return CompletableFuture.completedFuture(null);
        }

        @Override public CompletableFuture<Void> deleteByFaction(UUID factionId, int seasonId) {
            calls.add("deleteByFaction");
            return CompletableFuture.completedFuture(null);
        }
    }

    static final class NoopFactions implements FactionRepository {
        @Override public List<FactionRecord> loadSeason(int seasonId) { return List.of(); }
        @Override public CompletableFuture<Void> create(FactionRecord f) { return done(); }
        @Override public CompletableFuture<Void> addMember(UUID f, UUID p, FactionRank r, Instant at) { return done(); }
        @Override public CompletableFuture<Void> removeMember(UUID f, UUID p) { return done(); }
        @Override public CompletableFuture<Void> delete(UUID factionId) { return done(); }
        private static CompletableFuture<Void> done() { return CompletableFuture.completedFuture(null); }
    }

    final MemoryClaims repo = new MemoryClaims();
    final FactionIndex factions = new FactionIndex(new NoopFactions(), Clock.fixed(T0, ZoneOffset.UTC), 1);
    ClaimIndex claims;
    UUID ravens;
    UUID hawks;
    final UUID leader = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        claims = new ClaimIndex(repo, factions, Clock.fixed(T0, ZoneOffset.UTC), 1, WORLD::equals);
        ravens = factions.create("Ravens", leader).faction().id();
        hawks = factions.create("Hawks", UUID.randomUUID()).faction().id();
    }

    private ClaimIndex.Result claim(int x, int z, UUID faction) {
        return claims.claim(WORLD, x, z, faction, leader, CAPACITY);
    }

    @Test
    void unclaimedChunksReadAsTheSharedWildernessInstance() {
        var territory = claims.at(WORLD, 5, 5);
        assertTrue(territory.isWilderness());
        assertSame(Territory.wilderness(), territory, "one wilderness instance, so callers never null-check");
        assertSame(claims.at(WORLD, 9, 9), claims.at("other", 1, 1));
    }

    @Test
    void claimRegistersBothIndexesAndPersistsOnce() {
        assertTrue(claim(0, 0, ravens).ok());
        assertEquals(1, claims.countOf(ravens));
        assertEquals(1, claims.total());
        assertEquals(ravens, claims.at(WORLD, 0, 0).faction().orElseThrow().id());
        assertEquals(List.of("save 0,0"), repo.calls);
        assertTrue(claims.indexesAgree());
    }

    @Test
    void chunksAreDistinctPerCoordinateIncludingNegatives() {
        assertTrue(claim(-1, -1, ravens).ok());
        assertTrue(claim(-1, 0, ravens).ok());
        assertTrue(claim(0, -1, ravens).ok());
        assertEquals(3, claims.countOf(ravens));
        assertTrue(claims.at(WORLD, -1, -1).faction().isPresent());
        assertTrue(claims.at(WORLD, 1, 1).isWilderness(), "a mirrored coordinate is a different chunk");
        assertTrue(claims.indexesAgree());
    }

    @Test
    void claimingIsRefusedOverCapacityAndNothingIsWritten() {
        for (int i = 0; i < CAPACITY; i++) {
            assertTrue(claim(i, 0, ravens).ok());
        }
        var refused = claim(99, 0, ravens);
        assertFalse(refused.ok());
        assertEquals(ClaimIndex.Refusal.OVER_CAPACITY, refused.refusal());
        assertEquals(CAPACITY, claims.countOf(ravens), "a refused claim must not enter the index");
        assertEquals(CAPACITY, repo.calls.size(), "and must not be written");
        assertTrue(claims.at(WORLD, 99, 0).isWilderness());
        assertTrue(claims.indexesAgree());
    }

    @Test
    void anOccupiedChunkIsRefusedWithTheRightReason() {
        claim(0, 0, ravens);
        assertEquals(ClaimIndex.Refusal.ALREADY_OWNED_BY_YOU, claim(0, 0, ravens).refusal());
        assertEquals(ClaimIndex.Refusal.OWNED_BY_OTHER, claim(0, 0, hawks).refusal());
        assertEquals(ravens, claims.ownerOf(WORLD, 0, 0).orElseThrow(), "the owner is unchanged");
    }

    @Test
    void claimingIsRefusedInADisabledWorld() {
        var refused = claims.claim("nether", 0, 0, ravens, leader, CAPACITY);
        assertEquals(ClaimIndex.Refusal.WORLD_DISABLED, refused.refusal());
        assertEquals(0, claims.total());
        assertTrue(claims.at("nether", 0, 0).isWilderness(), "a disabled world is never protected");
    }

    @Test
    void squareRadiusFollowsTheFactionsUuidConvention() {
        assertEquals(1, claims.claimSquare(WORLD, 0, 0, 1, ravens, leader, 100).claimed(), "1 = the chunk you stand on");
        assertEquals(8, claims.claimSquare(WORLD, 0, 0, 2, ravens, leader, 100).claimed(), "2 = 3x3, centre already held");
        var fiveByFive = claims.claimSquare(WORLD, 0, 0, 3, ravens, leader, 100);
        assertEquals(16, fiveByFive.claimed());
        assertEquals(9, fiveByFive.alreadyYours());
        assertEquals(25, claims.countOf(ravens));
        assertTrue(claims.at(WORLD, -2, 2).faction().isPresent());
        assertTrue(claims.at(WORLD, 3, 0).isWilderness());
        assertTrue(claims.indexesAgree());
    }

    @Test
    void squareClaimStopsAtCapacityKeepingTheNearestChunks() {
        var result = claims.claimSquare(WORLD, 10, 10, 3, ravens, leader, 5);
        assertEquals(5, result.claimed());
        assertEquals(20, result.overCapacity());
        assertEquals(ravens, claims.ownerOf(WORLD, 10, 10).orElseThrow(), "the centre is claimed first");
        for (var key : List.of(new int[]{8, 8}, new int[]{12, 12}, new int[]{8, 12})) {
            assertTrue(claims.at(WORLD, key[0], key[1]).isWilderness(), "outer ring is not reached");
        }
        assertTrue(claims.indexesAgree());
    }

    @Test
    void squareClaimSkipsOtherFactionsLand() {
        claims.claim(WORLD, 1, 0, hawks, leader, 100);
        var result = claims.claimSquare(WORLD, 0, 0, 2, ravens, leader, 100);
        assertEquals(8, result.claimed());
        assertEquals(1, result.ownedByOther());
        assertEquals(hawks, claims.ownerOf(WORLD, 1, 0).orElseThrow());
    }

    @Test
    void squareClaimInADisabledWorldClaimsNothing() {
        var result = claims.claimSquare("nether", 0, 0, 3, ravens, leader, 100);
        assertTrue(result.worldDisabled());
        assertEquals(0, claims.total());
        assertTrue(repo.calls.isEmpty());
    }

    @Test
    void unclaimClearsBothIndexes() {
        claim(0, 0, ravens);
        claim(1, 0, ravens);
        assertTrue(claims.unclaim(WORLD, 0, 0, ravens).isPresent());
        assertEquals(1, claims.countOf(ravens));
        assertTrue(claims.at(WORLD, 0, 0).isWilderness());
        assertTrue(claims.indexesAgree());
        assertEquals(List.of("save 0,0", "save 1,0", "delete 0,0"), repo.calls);
    }

    @Test
    void unclaimingSomeoneElsesChunkIsRefusedAndWritesNothing() {
        claim(0, 0, ravens);
        assertTrue(claims.unclaim(WORLD, 0, 0, hawks).isEmpty());
        assertEquals(ravens, claims.ownerOf(WORLD, 0, 0).orElseThrow());
        assertEquals(1, repo.calls.size());
    }

    @Test
    void unclaimAllReleasesEveryChunkAcrossWorlds() {
        var everywhere = new ClaimIndex(repo, factions, Clock.fixed(T0, ZoneOffset.UTC), 1, w -> true);
        everywhere.claim(WORLD, 0, 0, ravens, leader, 10);
        everywhere.claim(WORLD, 1, 0, ravens, leader, 10);
        everywhere.claim("nether", 0, 0, ravens, leader, 10);
        everywhere.claim(WORLD, 5, 5, hawks, leader, 10);

        everywhere.unclaimAll(ravens).join();
        assertEquals(0, everywhere.countOf(ravens));
        assertEquals(1, everywhere.total(), "other factions keep theirs");
        assertTrue(everywhere.at(WORLD, 0, 0).isWilderness());
        assertTrue(everywhere.at("nether", 0, 0).isWilderness());
        assertTrue(everywhere.indexesAgree());
    }

    @Test
    void disbandingAFactionReleasesItsClaims() {
        factions.onDisband(id -> claims.unclaimAll(id));
        claim(0, 0, ravens);
        claim(1, 0, ravens);
        factions.disband(ravens).join();
        assertEquals(0, claims.total(), "a disbanded faction leaves no claims behind");
        assertTrue(claims.at(WORLD, 0, 0).isWilderness());
        assertTrue(claims.indexesAgree());
    }

    @Test
    void loadRebuildsBothIndexesFromStorage() {
        repo.season = List.of(
                new Claim(WORLD, 3, 4, ravens, leader, T0),
                new Claim(WORLD, 3, 5, ravens, leader, T0),
                new Claim(WORLD, -7, 2, hawks, leader, T0));
        assertEquals(3, claims.load());
        assertEquals(2, claims.countOf(ravens));
        assertEquals(1, claims.countOf(hawks));
        assertEquals(ravens, claims.ownerOf(WORLD, 3, 4).orElseThrow());
        assertEquals(hawks, claims.ownerOf(WORLD, -7, 2).orElseThrow());
        assertTrue(claims.indexesAgree());
    }

    @Test
    void countOfAnUnknownFactionIsZero() {
        assertEquals(0, claims.countOf(UUID.randomUUID()));
    }

    @Test
    void claimOfADeletedFactionReadsAsWildernessRatherThanCrashing() {
        // The faction index is the source of truth for who a faction is; a claim pointing at a
        // faction that no longer exists must degrade to wilderness, never throw at a listener.
        claim(0, 0, ravens);
        factions.disband(ravens).join();
        assertTrue(claims.at(WORLD, 0, 0).isWilderness());
    }
}
