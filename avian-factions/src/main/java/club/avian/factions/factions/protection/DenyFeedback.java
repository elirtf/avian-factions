package club.avian.factions.factions.protection;

import club.avian.factions.api.config.ConfigHandle;
import club.avian.factions.api.faction.ProtectionDecision;
import club.avian.factions.api.faction.Territory;
import club.avian.factions.factions.FactionsConfig;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tells a player why they were stopped, at most once per reason per cooldown.
 *
 * <p>Throttling is not cosmetic: {@code PlayerInteractEvent} with {@code Action.PHYSICAL} fires
 * every tick a player stands on a pressure plate, and hopper events fire every transfer, so an
 * unthrottled message would be a chat flood and a performance problem (#4).
 */
public final class DenyFeedback {

    private record Key(UUID player, ProtectionDecision reason) {
    }

    private final Map<Key, Long> lastSent = new ConcurrentHashMap<>();
    private final ConfigHandle<FactionsConfig> config;

    public DenyFeedback(ConfigHandle<FactionsConfig> config) {
        this.config = config;
    }

    public void send(Player player, ProtectionDecision decision, Territory territory) {
        int cooldown = config.get().protection().denyMessageCooldownSeconds();
        var key = new Key(player.getUniqueId(), decision);
        long now = System.currentTimeMillis();
        var previous = lastSent.get(key);
        if (previous != null && now - previous < cooldown * 1000L) {
            return;
        }
        lastSent.put(key, now);
        player.sendActionBar(message(decision, territory));
    }

    /** Forgets a player's throttle state; called on quit so the map cannot grow unbounded. */
    public void forget(UUID player) {
        lastSent.keySet().removeIf(key -> key.player().equals(player));
    }

    private static Component message(ProtectionDecision decision, Territory territory) {
        return switch (decision) {
            case DENY_SAFEZONE -> Component.text("This is protected land.", NamedTextColor.RED);
            case DENY_FOREIGN_TERRITORY -> Component.text("This land belongs to "
                    + territory.faction().map(club.avian.factions.api.faction.Faction::name).orElse("another faction")
                    + ".", NamedTextColor.RED);
            default -> Component.empty();
        };
    }
}
