package club.avian.factions.api.faction;

import java.util.Optional;
import java.util.UUID;

/**
 * Read access to Factions for other modules. Every lookup is a synchronous cache hit: the whole
 * current Season is held in memory and the database is only written to (ADR-0002, #4 prior art).
 */
public interface Factions {

    Optional<Faction> byId(UUID id);

    /** Case-insensitive; the name a player would type. */
    Optional<Faction> byName(String name);

    /** The Faction this player belongs to, if any. */
    Optional<Faction> ofPlayer(UUID player);

    int count();
}
