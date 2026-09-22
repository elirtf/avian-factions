package club.avian.factions.factions.protection;

import club.avian.factions.api.faction.Claims;
import club.avian.factions.api.faction.Interaction;
import club.avian.factions.api.faction.ProtectionPolicy;
import club.avian.factions.api.faction.Territory;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.Cancellable;
import org.bukkit.projectiles.ProjectileSource;

import java.util.Optional;

/**
 * The one thing every protection listener calls: resolve the territory, ask the policy, cancel and
 * explain if denied. Listeners contain no rules of their own — they translate an event into a
 * {@code (player, location, interaction)} triple and nothing more.
 */
public final class ProtectionGuard {

    private final Claims claims;
    private final ProtectionPolicy policy;
    private final DenyFeedback feedback;

    public ProtectionGuard(Claims claims, ProtectionPolicy policy, DenyFeedback feedback) {
        this.claims = claims;
        this.policy = policy;
        this.feedback = feedback;
    }

    public Claims claims() {
        return claims;
    }

    public ProtectionPolicy policy() {
        return policy;
    }

    /** Cancels {@code event} when {@code player} may not do {@code interaction} at {@code where}. */
    public boolean deny(Cancellable event, Player player, Location where, Interaction interaction) {
        return deny(event, player, claims.at(where), interaction);
    }

    public boolean deny(Cancellable event, Player player, Block block, Interaction interaction) {
        return deny(event, player, claims.at(block), interaction);
    }

    public boolean deny(Cancellable event, Player player, Territory territory, Interaction interaction) {
        var decision = policy.check(player.getUniqueId(), territory, interaction);
        if (decision.denied()) {
            event.setCancelled(true);
            feedback.send(player, decision, territory);
            return true;
        }
        return false;
    }

    /**
     * The player behind an entity: the entity itself if it is a player, or a projectile's shooter.
     *
     * <p>Attribution matters — the surveyed plugins' arrow and snowball griefing bugs all came from
     * treating {@code getDamager()} as the attacker without unwrapping the projectile.
     */
    public static Optional<Player> playerBehind(Entity entity) {
        if (entity instanceof Player player) {
            return Optional.of(player);
        }
        if (entity instanceof Projectile projectile) {
            ProjectileSource shooter = projectile.getShooter();
            if (shooter instanceof Player player) {
                return Optional.of(player);
            }
        }
        return Optional.empty();
    }

    /** True when two locations are in different chunks — the cheap guard before a policy call. */
    public static boolean differentChunk(Location a, Location b) {
        return (a.getBlockX() >> 4) != (b.getBlockX() >> 4)
                || (a.getBlockZ() >> 4) != (b.getBlockZ() >> 4)
                || !sameWorld(a.getWorld(), b.getWorld());
    }

    private static boolean sameWorld(World a, World b) {
        return a != null && b != null && a.getUID().equals(b.getUID());
    }
}
