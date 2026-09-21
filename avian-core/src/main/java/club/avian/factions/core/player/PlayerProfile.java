package club.avian.factions.core.player;

import club.avian.factions.api.player.AvianPlayer;

import java.time.Instant;
import java.util.UUID;

/** Immutable {@link AvianPlayer}; the cache holds one per online player. */
public record PlayerProfile(UUID uuid, String name, Instant firstJoinAt, Instant lastSeenAt) implements AvianPlayer {

    public PlayerProfile withSeen(String name, Instant at) {
        return new PlayerProfile(uuid, name, firstJoinAt, at);
    }
}
