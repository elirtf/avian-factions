package club.avian.factions.api.player;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/** Access to Avian Players: a synchronous cache for online players, async lookup otherwise. */
public interface Players {

    /** The cached profile of an online player; empty if they are not online. Main thread safe. */
    Optional<AvianPlayer> online(UUID uuid);

    /** Loads a profile from the database; empty if the player has never joined. */
    CompletableFuture<Optional<AvianPlayer>> load(UUID uuid);
}
