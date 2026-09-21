package club.avian.factions.core.player;

import club.avian.factions.api.player.AvianPlayer;
import club.avian.factions.api.player.Players;

import java.time.Clock;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The online cache behind {@link Players}. Profiles enter in the async pre-login phase and leave
 * on quit; everything in between is a synchronous map read (ADR-0004).
 */
public final class PlayerService implements Players {

    private final PlayerRepository repository;
    private final Clock clock;
    private final Map<UUID, PlayerProfile> online = new ConcurrentHashMap<>();

    public PlayerService(PlayerRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Override
    public Optional<AvianPlayer> online(UUID uuid) {
        return Optional.ofNullable(online.get(uuid));
    }

    @Override
    public CompletableFuture<Optional<AvianPlayer>> load(UUID uuid) {
        var cached = online.get(uuid);
        if (cached != null) {
            return CompletableFuture.completedFuture(Optional.of(cached));
        }
        return repository.find(uuid).thenApply(p -> p.map(AvianPlayer.class::cast));
    }

    /** Async pre-login: load or create, then cache. Completes exceptionally if the database fails. */
    public CompletableFuture<PlayerProfile> handleLogin(UUID uuid, String name) {
        return repository.findOrCreate(uuid, name, clock.instant())
                .thenApply(profile -> {
                    online.put(uuid, profile);
                    return profile;
                });
    }

    /** Quit: drop from the cache and record last-seen asynchronously. */
    public CompletableFuture<Void> handleQuit(UUID uuid) {
        var profile = online.remove(uuid);
        if (profile == null) {
            return CompletableFuture.completedFuture(null);
        }
        return repository.touchLastSeen(uuid, clock.instant());
    }

    /** Login was cached but the player never fully joined (kicked by another plugin, etc.). */
    public void discard(UUID uuid) {
        online.remove(uuid);
    }

    public int onlineCount() {
        return online.size();
    }
}
