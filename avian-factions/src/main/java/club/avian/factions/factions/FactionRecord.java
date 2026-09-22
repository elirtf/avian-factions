package club.avian.factions.factions;

import club.avian.factions.api.faction.Faction;
import club.avian.factions.api.faction.FactionRank;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/** Immutable {@link Faction}; the in-memory index holds one per Faction in the current Season. */
public record FactionRecord(UUID id, String name, UUID leader, Map<UUID, FactionRank> members,
                            int seasonId, Instant createdAt) implements Faction {

    public FactionRecord {
        members = Map.copyOf(members);
    }

    public FactionRecord withMember(UUID player, FactionRank rank) {
        var updated = new java.util.HashMap<>(members);
        updated.put(player, rank);
        return new FactionRecord(id, name, leader, updated, seasonId, createdAt);
    }

    public FactionRecord withoutMember(UUID player) {
        var updated = new java.util.HashMap<>(members);
        updated.remove(player);
        return new FactionRecord(id, name, leader, updated, seasonId, createdAt);
    }
}
