package club.avian.factions.factions.claim;

import club.avian.factions.api.faction.Claims;
import club.avian.factions.api.faction.Faction;
import club.avian.factions.api.faction.Territory;
import club.avian.factions.factions.FactionIndex;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import org.bukkit.World;

import java.time.Clock;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

/**
 * The claim index: chunk → Faction, in memory, one map per world, keyed by a packed {@code long}
 * (#4 prior art — the plugins that query storage per event are the plugins with lag tickets).
 *
 * <p>The reverse index {@code factionId → chunk keys} is maintained in {@link #claim} and
 * {@link #unclaim} <em>and nowhere else</em>, so the two can only disagree if those two methods
 * are wrong. {@code countOf} reads its size rather than scanning.
 *
 * <p>Main thread only.
 */
public final class ClaimIndex implements Claims {

    /** Per-world chunk → faction id. */
    private final Map<String, Long2ObjectMap<UUID>> byWorld = new HashMap<>();
    /** Reverse: faction id → the chunks it holds, per world. */
    private final Map<UUID, Map<String, LongSet>> byFaction = new HashMap<>();

    private final ClaimRepository repository;
    private final FactionIndex factions;
    private final Clock clock;
    private final int seasonId;
    /** Worlds where claiming is allowed at all; everything else reads as wilderness. */
    private final Predicate<String> claimableWorld;

    public ClaimIndex(ClaimRepository repository, FactionIndex factions, Clock clock, int seasonId,
                      Predicate<String> claimableWorld) {
        this.repository = repository;
        this.factions = factions;
        this.clock = clock;
        this.seasonId = seasonId;
        this.claimableWorld = claimableWorld;
    }

    /** Loads the Season's claims. Boot-time; see {@code Database#queryDuringBoot}. */
    public int load() {
        var claims = repository.loadSeason(seasonId);
        for (var claim : claims) {
            put(claim.world(), claim.key(), claim.factionId());
        }
        return claims.size();
    }

    // --- lookups ---------------------------------------------------------------------------

    @Override
    public Territory at(World world, int chunkX, int chunkZ) {
        return at(world.getName(), chunkX, chunkZ);
    }

    /** The world-name form, so tests and the map renderer do not need a Bukkit world. */
    public Territory at(String world, int chunkX, int chunkZ) {
        var chunks = byWorld.get(world);
        if (chunks == null) {
            return Territory.wilderness();
        }
        var factionId = chunks.get(ChunkKey.of(chunkX, chunkZ));
        if (factionId == null) {
            return Territory.wilderness();
        }
        return factions.byId(factionId).<Territory>map(FactionTerritory::new).orElseGet(Territory::wilderness);
    }

    /** The Faction holding this chunk, if any. */
    public Optional<UUID> ownerOf(String world, int chunkX, int chunkZ) {
        var chunks = byWorld.get(world);
        return chunks == null ? Optional.empty() : Optional.ofNullable(chunks.get(ChunkKey.of(chunkX, chunkZ)));
    }

    @Override
    public int countOf(UUID factionId) {
        var worlds = byFaction.get(factionId);
        if (worlds == null) {
            return 0;
        }
        int total = 0;
        for (var chunks : worlds.values()) {
            total += chunks.size();
        }
        return total;
    }

    @Override
    public int total() {
        int total = 0;
        for (var chunks : byWorld.values()) {
            total += chunks.size();
        }
        return total;
    }

    // --- mutations -------------------------------------------------------------------------

    /** Why a claim was refused; the command maps these to messages. */
    public enum Refusal {
        WORLD_DISABLED, ALREADY_OWNED_BY_YOU, OWNED_BY_OTHER, OVER_CAPACITY
    }

    /** The outcome of a claim attempt: a refusal, or the write that persists it. */
    public record Result(Refusal refusal, CompletableFuture<Void> persisted) {
        public boolean ok() {
            return refusal == null;
        }

        static Result refused(Refusal refusal) {
            return new Result(refusal, CompletableFuture.completedFuture(null));
        }
    }

    /**
     * Claims one chunk for {@code factionId}.
     *
     * @param capacity how many chunks this Faction may hold, from Power (spec §8)
     */
    public Result claim(String world, int chunkX, int chunkZ, UUID factionId, UUID claimedBy, int capacity) {
        if (!claimableWorld.test(world)) {
            return Result.refused(Refusal.WORLD_DISABLED);
        }
        var existing = ownerOf(world, chunkX, chunkZ);
        if (existing.isPresent()) {
            return Result.refused(existing.get().equals(factionId)
                    ? Refusal.ALREADY_OWNED_BY_YOU : Refusal.OWNED_BY_OTHER);
        }
        if (countOf(factionId) >= capacity) {
            return Result.refused(Refusal.OVER_CAPACITY);
        }
        put(world, ChunkKey.of(chunkX, chunkZ), factionId);
        var claim = new Claim(world, chunkX, chunkZ, factionId, claimedBy, clock.instant());
        return new Result(null, repository.save(claim, seasonId));
    }

    /** What a square claim did: counts per outcome, plus every write it made. */
    public record SquareResult(int claimed, int alreadyYours, int ownedByOther, int overCapacity,
                               boolean worldDisabled, CompletableFuture<Void> persisted) {
    }

    /**
     * Claims the square of side {@code 2 * radius - 1} centred on a chunk ({@code /f claim <radius>},
     * the FactionsUUID convention: radius 1 is the chunk you stand on, 2 is 3x3, 3 is 5x5).
     * Chunks are tried nearest-ring first, so when power runs out the land kept is the land around
     * the player. Chunks another Faction holds are skipped, never taken.
     */
    public SquareResult claimSquare(String world, int centreX, int centreZ, int radius,
                                    UUID factionId, UUID claimedBy, int capacity) {
        if (!claimableWorld.test(world)) {
            return new SquareResult(0, 0, 0, 0, true, CompletableFuture.completedFuture(null));
        }
        int claimed = 0;
        int alreadyYours = 0;
        int ownedByOther = 0;
        int overCapacity = 0;
        var writes = new ArrayList<CompletableFuture<Void>>();
        for (int ring = 0; ring < radius; ring++) {
            for (int dx = -ring; dx <= ring; dx++) {
                for (int dz = -ring; dz <= ring; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != ring) {
                        continue;
                    }
                    var result = claim(world, centreX + dx, centreZ + dz, factionId, claimedBy, capacity);
                    if (result.ok()) {
                        claimed++;
                        writes.add(result.persisted());
                    } else {
                        switch (result.refusal()) {
                            case ALREADY_OWNED_BY_YOU -> alreadyYours++;
                            case OWNED_BY_OTHER -> ownedByOther++;
                            case OVER_CAPACITY -> overCapacity++;
                            case WORLD_DISABLED -> throw new IllegalStateException("world checked above");
                        }
                    }
                }
            }
        }
        return new SquareResult(claimed, alreadyYours, ownedByOther, overCapacity, false,
                CompletableFuture.allOf(writes.toArray(CompletableFuture[]::new)));
    }

    /** Releases one chunk. Returns empty when the Faction did not hold it. */
    public Optional<CompletableFuture<Void>> unclaim(String world, int chunkX, int chunkZ, UUID factionId) {
        var owner = ownerOf(world, chunkX, chunkZ);
        if (owner.isEmpty() || !owner.get().equals(factionId)) {
            return Optional.empty();
        }
        remove(world, ChunkKey.of(chunkX, chunkZ), factionId);
        return Optional.of(repository.delete(world, chunkX, chunkZ, seasonId));
    }

    /** Releases every chunk a Faction holds — {@code /f unclaim all}, and disband. */
    public CompletableFuture<Void> unclaimAll(UUID factionId) {
        var worlds = byFaction.remove(factionId);
        if (worlds == null) {
            return CompletableFuture.completedFuture(null);
        }
        for (var entry : worlds.entrySet()) {
            var chunks = byWorld.get(entry.getKey());
            if (chunks != null) {
                entry.getValue().forEach(chunks::remove);
            }
        }
        return repository.deleteByFaction(factionId, seasonId);
    }

    // --- index maintenance: the only two places the maps change ------------------------------

    private void put(String world, long key, UUID factionId) {
        byWorld.computeIfAbsent(world, w -> new Long2ObjectOpenHashMap<>()).put(key, factionId);
        byFaction.computeIfAbsent(factionId, f -> new HashMap<>())
                .computeIfAbsent(world, w -> new LongOpenHashSet()).add(key);
    }

    private void remove(String world, long key, UUID factionId) {
        var chunks = byWorld.get(world);
        if (chunks != null) {
            chunks.remove(key);
        }
        var worlds = byFaction.get(factionId);
        if (worlds != null) {
            var keys = worlds.get(world);
            if (keys != null) {
                keys.remove(key);
                if (keys.isEmpty()) {
                    worlds.remove(world);
                }
            }
            if (worlds.isEmpty()) {
                byFaction.remove(factionId);
            }
        }
    }

    /** Test seam: true when the forward and reverse indexes describe the same set of chunks. */
    public boolean indexesAgree() {
        int forward = total();
        int reverse = 0;
        for (var worlds : byFaction.values()) {
            for (var entry : worlds.entrySet()) {
                reverse += entry.getValue().size();
                var chunks = byWorld.get(entry.getKey());
                if (chunks == null) {
                    return false;
                }
                for (var key : entry.getValue()) {
                    if (!chunks.containsKey((long) key)) {
                        return false;
                    }
                }
            }
        }
        return forward == reverse;
    }

    /** Not a record: Territory#faction() returns Optional, which a record accessor cannot. */
    private static final class FactionTerritory implements Territory {
        private final Faction owner;

        FactionTerritory(Faction owner) {
            this.owner = owner;
        }

        @Override
        public Kind kind() {
            return Kind.FACTION;
        }

        @Override
        public Optional<Faction> faction() {
            return Optional.of(owner);
        }

        @Override
        public String toString() {
            return "Territory(" + owner.name() + ")";
        }
    }
}
