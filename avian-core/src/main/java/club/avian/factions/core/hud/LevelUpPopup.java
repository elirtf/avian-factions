package club.avian.factions.core.hud;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLevelChangeEvent;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Plays the HUD's level-up burst: BetterHud has no level-up trigger, so on each level gained this runs
 * {@code hud popup show <player> <popup>}. The popup itself (art, timing) lives in BetterHud's config;
 * a blank popup name turns this off.
 */
public final class LevelUpPopup implements Listener {

    private final Supplier<String> popup;
    private final Consumer<String> console;

    /** {@code console} runs a command as the console, without its feedback (one line per level-up otherwise). */
    public LevelUpPopup(Supplier<String> popup, Consumer<String> console) {
        this.popup = popup;
        this.console = console;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onLevel(PlayerLevelChangeEvent event) {
        String name = popup.get();
        if (event.getNewLevel() > event.getOldLevel() && !name.isBlank()) {
            show(event.getPlayer(), name);
        }
    }

    private void show(Player player, String name) {
        console.accept("hud popup show " + player.getName() + " " + name);
    }
}
