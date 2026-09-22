package club.avian.factions.api.faction;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * A Faction as other modules see it: identity and membership, no mutation (see CONTEXT.md).
 * Claims, power, relations and the bank join this interface as those systems land.
 */
public interface Faction {

    UUID id();

    /** Display name as created, e.g. {@code Ravens}. Case is preserved; uniqueness is case-insensitive. */
    String name();

    UUID leader();

    /** Every member including the leader, by UUID. */
    Map<UUID, FactionRank> members();

    /** The Season this Faction belongs to; Factions never cross Seasons. */
    int seasonId();

    Instant createdAt();

    default FactionRank rankOf(UUID player) {
        return members().get(player);
    }

    default boolean hasMember(UUID player) {
        return members().containsKey(player);
    }
}
