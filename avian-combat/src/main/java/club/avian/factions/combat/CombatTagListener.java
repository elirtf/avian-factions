package club.avian.factions.combat;

import club.avian.factions.api.config.ConfigHandle;
import club.avian.factions.api.text.Brand;
import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * The combat tag (spec §43). A player-vs-player hit puts both players in combat for
 * {@code combat-tag-seconds}, shown as a draining boss bar. In combat a player cannot teleport by
 * command, fly, or run the blocked commands, and logging out leaves a {@link LogoutBodies body}.
 *
 * <p>Hits that another plugin cancelled (faction-mates, allies, safe zones) never tag: the tag
 * listens at MONITOR and ignores cancelled events.
 */
final class CombatTagListener implements Listener {

    private final ConfigHandle<CombatConfig> config;
    private final CombatTags tags;
    private final LogoutBodies bodies;
    private final Map<UUID, BossBar> bars = new HashMap<>();

    CombatTagListener(ConfigHandle<CombatConfig> config, CombatTags tags, LogoutBodies bodies) {
        this.config = config;
        this.tags = tags;
        this.bodies = bodies;
    }

    // --- tagging -----------------------------------------------------------------------------------

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onHit(EntityDamageByEntityEvent event) {
        Player attacker = attacker(event.getDamager());
        if (attacker == null) {
            return;
        }
        if (event.getEntity() instanceof Player victim && victim != attacker) {
            tag(attacker);
            tag(victim);
        } else if (bodies != null && bodies.isBody(event.getEntity())) {
            tag(attacker);                      // hitting a logged-out body is a fight too
        }
    }

    void tag(Player player) {
        int seconds = config.get().combatTagSeconds();
        if (seconds <= 0 || !taggable(player)) {
            return;
        }
        if (tags.tag(player.getUniqueId(), seconds)) {
            player.sendMessage(Brand.mm("<bad>⚔</bad> <hot>You're in combat.</hot> <soft>Logging out now leaves"
                    + " your body behind with your loot.</soft>"));
        }
        if (player.isFlying()) {
            player.setFlying(false);
        }
        showBar(player);
    }

    boolean isTagged(Player player) {
        return tags.isTagged(player.getUniqueId());
    }

    private static boolean taggable(Player player) {
        return player.getGameMode() == GameMode.SURVIVAL || player.getGameMode() == GameMode.ADVENTURE;
    }

    private static Player attacker(Entity damager) {
        if (damager instanceof Player player) {
            return player;
        }
        if (damager instanceof Projectile projectile && projectile.getShooter() instanceof Player shooter) {
            return shooter;
        }
        return null;
    }

    // --- the bar, and running out ------------------------------------------------------------------

    /** Every few ticks: drain the bars, and release players whose tag ran out. */
    void tick() {
        for (UUID id : tags.tagged()) {
            var player = Bukkit.getPlayer(id);
            if (player != null) {
                showBar(player);
            }
        }
        for (UUID id : tags.expire()) {
            var player = Bukkit.getPlayer(id);
            hideBar(id, player);
            if (player != null) {
                player.sendMessage(Brand.mm("<cane>✔</cane> <soft>You're out of combat.</soft>"));
            }
        }
    }

    private void showBar(Player player) {
        long left = tags.remainingMillis(player.getUniqueId());
        float progress = Math.clamp(left / (config.get().combatTagSeconds() * 1000f), 0f, 1f);
        var name = Brand.mm("<bad>⚔</bad> <hot>Combat</hot> <sun>" + (left + 999) / 1000 + "s</sun>");
        var bar = bars.get(player.getUniqueId());
        if (bar == null) {
            bar = BossBar.bossBar(name, progress, BossBar.Color.RED, BossBar.Overlay.NOTCHED_20);
            bars.put(player.getUniqueId(), bar);
            player.showBossBar(bar);
        } else {
            bar.name(name).progress(progress);
        }
    }

    private void hideBar(UUID id, Player player) {
        var bar = bars.remove(id);
        if (bar != null && player != null) {
            player.hideBossBar(bar);
        }
    }

    private void release(Player player) {
        tags.clear(player.getUniqueId());
        hideBar(player.getUniqueId(), player);
    }

    // --- what combat forbids -----------------------------------------------------------------------

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        var player = event.getPlayer();
        if (isTagged(player) && blocked(event.getMessage())) {
            event.setCancelled(true);
            player.sendMessage(Brand.mm("<bad>✖</bad> <soft>You can't do that in combat.</soft> <dim>(<hot>"
                    + (tags.remainingMillis(player.getUniqueId()) + 999) / 1000 + "s</hot> left)</dim>"));
        }
    }

    /** "/Essentials:Home base" → "home base", matched against "home" and "f home" style entries. */
    boolean blocked(String message) {
        var command = message.startsWith("/") ? message.substring(1) : message;
        command = command.toLowerCase(Locale.ROOT).trim().replaceAll("\\s+", " ");
        int space = command.indexOf(' ');
        var label = space < 0 ? command : command.substring(0, space);
        int colon = label.indexOf(':');
        if (colon >= 0) {
            command = command.substring(colon + 1);
        }
        for (var entry : config.get().blockedCommands()) {
            var blocked = entry.toLowerCase(Locale.ROOT).trim();
            if (command.equals(blocked) || command.startsWith(blocked + " ")) {
                return true;
            }
        }
        return false;
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        if (event.getCause() == PlayerTeleportEvent.TeleportCause.COMMAND && isTagged(event.getPlayer())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(Brand.mm("<bad>✖</bad> <soft>You can't teleport in combat.</soft>"));
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onFly(PlayerToggleFlightEvent event) {
        if (event.isFlying() && isTagged(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    // --- leaving, dying, coming back ---------------------------------------------------------------

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        var player = event.getPlayer();
        boolean wasTagged = isTagged(player);
        release(player);
        // A shutdown kicks everyone; that is not running from a fight.
        if (wasTagged && bodies != null && !Bukkit.isStopping() && !player.isDead()) {
            bodies.spawn(player);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(PlayerDeathEvent event) {
        release(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        var player = event.getPlayer();
        if (bodies == null) {
            return;
        }
        if (bodies.owesDeath(player.getUniqueId())) {
            bodies.collect(player);
        } else if (bodies.returned(player)) {
            tag(player);
            player.sendMessage(Brand.mm("<hot>You're back in your body</hot> <soft>— and still in combat.</soft>"));
        }
    }
}
