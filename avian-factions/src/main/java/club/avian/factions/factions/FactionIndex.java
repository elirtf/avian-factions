package club.avian.factions.factions;

import club.avian.factions.api.faction.Faction;
import club.avian.factions.api.faction.FactionRank;
import club.avian.factions.api.faction.Factions;

import java.time.Clock;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * The in-memory Faction index and the only place Factions are mutated (#4 prior art: every
 * lookup is a map read, the database is written to but never read on the hot path).
 *
 * <p>Main thread only. Each mutation updates the maps first, then queues the single-row write;
 * a failed write is logged by the caller, never rolled back into a half-updated index — the
 * next boot reloads from the database, which is the source of truth.
 */
public final class FactionIndex implements Factions {

    private final Map<UUID, FactionRecord> byId = new HashMap<>();
    private final Map<String, UUID> byNameKey = new HashMap<>();
    private final Map<UUID, UUID> factionOfPlayer = new HashMap<>();
    private final FactionRepository repository;
    private final Clock clock;
    private final int seasonId;

    public FactionIndex(FactionRepository repository, Clock clock, int seasonId) {
        this.repository = repository;
        this.clock = clock;
        this.seasonId = seasonId;
    }

    /** Loads the Season into memory. Called once at enable, before the command is registered. */
    public int load() {
        var factions = repository.loadSeason(seasonId);
        factions.forEach(this::put);
        return factions.size();
    }

    @Override
    public Optional<Faction> byId(UUID id) {
        return Optional.ofNullable(byId.get(id));
    }

    @Override
    public Optional<Faction> byName(String name) {
        var id = byNameKey.get(FactionName.key(name));
        return id == null ? Optional.empty() : byId(id);
    }

    @Override
    public Optional<Faction> ofPlayer(UUID player) {
        var id = factionOfPlayer.get(player);
        return id == null ? Optional.empty() : byId(id);
    }

    @Override
    public int count() {
        return byId.size();
    }

    public boolean nameTaken(String name) {
        return byNameKey.containsKey(FactionName.key(name));
    }

    /**
     * Creates a Faction with {@code leader} as its Leader.
     *
     * @return the new Faction, and the write that persists it
     */
    public Created create(String name, UUID leader) {
        if (nameTaken(name)) {
            throw new IllegalStateException("faction name already taken: " + name);
        }
        if (factionOfPlayer.containsKey(leader)) {
            throw new IllegalStateException("player is already in a faction: " + leader);
        }
        var faction = new FactionRecord(UUID.randomUUID(), name, leader,
                Map.of(leader, FactionRank.LEADER), seasonId, clock.instant());
        put(faction);
        return new Created(faction, repository.create(faction));
    }

    /** Disbands the Faction, dropping every membership. */
    public CompletableFuture<Void> disband(UUID factionId) {
        var faction = byId.remove(factionId);
        if (faction == null) {
            return CompletableFuture.completedFuture(null);
        }
        byNameKey.remove(FactionName.key(faction.name()));
        faction.members().keySet().forEach(factionOfPlayer::remove);
        return repository.delete(factionId);
    }

    private void put(FactionRecord faction) {
        byId.put(faction.id(), faction);
        byNameKey.put(FactionName.key(faction.name()), faction.id());
        faction.members().keySet().forEach(player -> factionOfPlayer.put(player, faction.id()));
    }

    /** A created Faction plus the future of its persistence, so callers can report write failures. */
    public record Created(FactionRecord faction, CompletableFuture<Void> persisted) {
    }
}
