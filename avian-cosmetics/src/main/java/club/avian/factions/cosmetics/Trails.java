package club.avian.factions.cosmetics;

import club.avian.factions.api.config.ConfigHandle;
import org.bukkit.Color;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Draws each player's chosen trail at their feet while they move. The choice is kept on the player
 * ({@link #CHOSEN}), so it survives relogs; it only shows while they still have the trail's
 * permission. Hidden for vanished, invisible and spectating players.
 */
final class Trails implements Listener {

    static final NamespacedKey CHOSEN = new NamespacedKey("avian", "trail");

    private final ConfigHandle<TrailsConfig> config;
    private final Map<UUID, Location> lastSeen = new HashMap<>();
    private long step;

    Trails(ConfigHandle<TrailsConfig> config) {
        this.config = config;
    }

    /** The trail this player picked, if it still exists and they may use it. */
    Optional<String> chosen(Player player) {
        String id = player.getPersistentDataContainer().get(CHOSEN, PersistentDataType.STRING);
        if (id == null || !config.get().trails().containsKey(id) || !owns(player, id)) {
            return Optional.empty();
        }
        return Optional.of(id);
    }

    static boolean owns(Player player, String id) {
        return player.hasPermission(TrailsConfig.PERMISSION_PREFIX + id);
    }

    void choose(Player player, String id) {
        player.getPersistentDataContainer().set(CHOSEN, PersistentDataType.STRING, id);
    }

    void clear(Player player) {
        player.getPersistentDataContainer().remove(CHOSEN);
    }

    /** One pass over the online players; the module runs it every {@code interval-ticks}. */
    void tick(Iterable<? extends Player> players) {
        var cfg = config.get();
        if (!cfg.enabled()) {
            return;
        }
        step++;
        for (Player player : players) {
            var id = chosen(player);
            Location now = player.getLocation();
            Location before = lastSeen.put(player.getUniqueId(), now);
            if (id.isEmpty() || hidden(player) || (cfg.onlyWhileMoving() && !moved(before, now))) {
                continue;
            }
            draw(player, cfg.trails().get(id.get()), now);
        }
    }

    static boolean moved(Location before, Location now) {
        return before != null && before.getWorld() == now.getWorld() && before.distanceSquared(now) > 0.0025;
    }

    // EssentialsX marks vanished players only through Bukkit metadata ("vanished"), deprecated but still its API.
    @SuppressWarnings("deprecation")
    static boolean hidden(Player player) {
        return player.getGameMode() == GameMode.SPECTATOR
                || player.hasPotionEffect(PotionEffectType.INVISIBILITY)
                || player.getMetadata("vanished").stream().anyMatch(m -> m.asBoolean());
    }

    private void draw(Player player, TrailsConfig.Trail trail, Location at) {
        Particle particle = trail.particle();
        var where = at.clone().add(0, 0.1, 0);
        Object data = data(particle, trail, step);
        player.getWorld().spawnParticle(particle, where, trail.count(), trail.spread(), 0.05, trail.spread(),
                trail.speed(), data);
    }

    /** The particle's data: none, or the trail's colours, stepping through them one puff at a time. */
    static Object data(Particle particle, TrailsConfig.Trail trail, long step) {
        var colours = trail.colours();
        if (colours.isEmpty()) {
            return null;
        }
        Color colour = colours.get((int) (step % colours.size()));
        var type = particle.getDataType();
        if (type == Particle.DustOptions.class) {
            return new Particle.DustOptions(colour, 1.2f);
        }
        if (type == Particle.DustTransition.class) {
            return new Particle.DustTransition(colour, colours.get((int) ((step + 1) % colours.size())), 1.2f);
        }
        if (type == Color.class) {
            return colour;
        }
        return null;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        lastSeen.remove(event.getPlayer().getUniqueId());
    }
}
