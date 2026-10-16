package club.avian.factions.economy;

import club.avian.factions.api.config.ConfigHandle;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.persistence.PersistentDataType;

import java.util.Locale;

/**
 * Counts each player's catches in icy waters, for the Ice Fisher quest (owner, 2026-10-16: 1,000 catches in an
 * ice biome earns the tag). Quests can't filter by biome, so we count and expose the total as
 * {@code %avian_ice_fish%}. The hook's spot decides, not where the player stands. The total lives in the
 * player's own data, so it survives restarts with nothing to store.
 */
final class IceFishing implements Listener {

    static final NamespacedKey CATCHES = new NamespacedKey("avian", "ice_fish");

    private final ConfigHandle<EconomyConfig> config;

    IceFishing(ConfigHandle<EconomyConfig> config) {
        this.config = config;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFish(PlayerFishEvent event) {
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH) {
            return;
        }
        var biome = event.getHook().getLocation().getBlock().getBiome().getKey().getKey();
        if (config.get().iceFishingBiomes().contains(biome.toLowerCase(Locale.ROOT))) {
            var data = event.getPlayer().getPersistentDataContainer();
            data.set(CATCHES, PersistentDataType.INTEGER, count(event.getPlayer()) + 1);
        }
    }

    static int count(Player player) {
        return player.getPersistentDataContainer().getOrDefault(CATCHES, PersistentDataType.INTEGER, 0);
    }
}
