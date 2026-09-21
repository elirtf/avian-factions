package club.avian.factions.core.player;

import io.papermc.paper.connection.PlayerLoginConnection;
import io.papermc.paper.event.connection.PlayerConnectionValidateLoginEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Bridges Bukkit's login lifecycle to {@link PlayerService}. Pre-login runs on an async thread,
 * so blocking on the repository future there is correct and keeps the main thread clean.
 */
public final class PlayerListener implements Listener {

    private final PlayerService players;
    private final Logger log;

    public PlayerListener(PlayerService players, Logger log) {
        this.players = players;
        this.log = log;
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onPreLogin(AsyncPlayerPreLoginEvent event) {
        if (event.getLoginResult() != AsyncPlayerPreLoginEvent.Result.ALLOWED) {
            return;
        }
        try {
            players.handleLogin(event.getUniqueId(), event.getName()).get();
        } catch (ExecutionException e) {
            log.log(Level.SEVERE, "Could not load profile for " + event.getName(), e.getCause());
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER,
                    Component.text("Profile could not be loaded. Please try again.", NamedTextColor.RED));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER,
                    Component.text("Server is shutting down.", NamedTextColor.RED));
        }
    }

    /** If anything else refused the login after pre-login cached a profile, drop it again. */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onValidateLogin(PlayerConnectionValidateLoginEvent event) {
        if (!event.isAllowed() && event.getConnection() instanceof PlayerLoginConnection login) {
            var profile = login.getAuthenticatedProfile();
            if (profile != null && profile.getId() != null) {
                players.discard(profile.getId());
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        var uuid = event.getPlayer().getUniqueId();
        players.handleQuit(uuid).exceptionally(t -> {
            log.log(Level.WARNING, "Could not record last-seen for " + event.getPlayer().getName(), t);
            return null;
        });
    }
}
