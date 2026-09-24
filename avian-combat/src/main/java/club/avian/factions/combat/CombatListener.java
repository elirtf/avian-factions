package club.avian.factions.combat;

import club.avian.factions.api.config.ConfigHandle;
import io.papermc.paper.event.entity.EntityKnockbackEvent;
import com.destroystokyo.paper.event.entity.EntityKnockbackByEntityEvent;
import org.bukkit.attribute.Attribute;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 1.8 combat feel on modern Paper (spec §44, `docs/research/combat-mechanics-on-paper.md`).
 *
 * <p>Three jobs: raise the attack-speed attribute so there is no 1.9 cooldown, replace the
 * knockback vector with the 1.8 formula, and drop sweep-attack damage.
 */
public final class CombatListener implements Listener {

    /**
     * A boosted melee hit fires the knockback event <em>twice</em> in the same tick: once for the
     * base push, once for the sprint/enchant bonus. We fold the bonus into the first call and zero
     * the second, so this remembers which pairs have already been handled this tick.
     */
    private final Map<UUID, Long> handledThisTick = new ConcurrentHashMap<>();

    private final ConfigHandle<CombatConfig> config;

    public CombatListener(ConfigHandle<CombatConfig> config) {
        this.config = config;
    }

    // --- attack cooldown -------------------------------------------------------------------

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        applyAttackSpeed(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRespawn(PlayerRespawnEvent event) {
        applyAttackSpeed(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldChange(PlayerChangedWorldEvent event) {
        applyAttackSpeed(event.getPlayer());
    }

    /** Attribute base values do not survive respawn or a world change, so we reapply on each. */
    public void applyAttackSpeed(Player player) {
        var cfg = config.get();
        // Hit delay first, and independent of the attribute below: Paper uses half of this as the
        // window before the next hit counts (LivingEntity: damageCooldownTime > invulnerableDuration
        // / 2). Always set, so turning the setting back to 0 restores vanilla instead of leaving the
        // last custom value in place.
        player.setMaximumNoDamageTicks(cfg.hitDelayTicks() > 0 ? cfg.hitDelayTicks() : VANILLA_HIT_DELAY_TICKS);

        var attribute = player.getAttribute(Attribute.ATTACK_SPEED);
        if (attribute == null) {
            return;
        }
        attribute.setBaseValue(cfg.disableAttackCooldown() ? cfg.attackSpeed() : 4.0);
    }

    /** Vanilla's invulnerability after a hit, in ticks: a new hit counts after half of it. */
    static final int VANILLA_HIT_DELAY_TICKS = 20;

    // --- knockback -------------------------------------------------------------------------

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onKnockback(EntityKnockbackByEntityEvent event) {
        if (event.getCause() != EntityKnockbackEvent.Cause.ENTITY_ATTACK
                || !(event.getEntity() instanceof Player victim)
                || !(event.getHitBy() instanceof Player attacker)) {
            return;
        }

        long tick = victim.getWorld().getFullTime();
        var previous = handledThisTick.put(victim.getUniqueId(), tick);
        if (previous != null && previous == tick) {
            // Second call of the same hit: the bonus is already in the vector we set.
            event.setKnockback(new org.bukkit.util.Vector(0, 0, 0));
            return;
        }

        int bonus = attacker.getInventory().getItemInMainHand()
                .getEnchantmentLevel(Enchantment.KNOCKBACK);
        if (attacker.isSprinting()) {
            // Vanilla clears the sprint flag *after* the knockback call, so this is still true here.
            bonus += 1;
        }

        var resistance = victim.getAttribute(Attribute.KNOCKBACK_RESISTANCE);
        var away = KnockbackMath.awayFrom(attacker.getLocation().getX(), attacker.getLocation().getZ(),
                victim.getLocation().getX(), victim.getLocation().getZ());
        event.setKnockback(KnockbackMath.knockback(config.get().knockback(), victim.getVelocity(), away,
                attacker.getLocation().getYaw(), bonus, resistance == null ? 0 : resistance.getValue()));
    }

    // --- sweep ------------------------------------------------------------------------------

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onSweepDamage(EntityDamageByEntityEvent event) {
        if (config.get().disableSweepAttack()
                && event.getCause() == EntityDamageEvent.DamageCause.ENTITY_SWEEP_ATTACK) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onSweepKnockback(EntityKnockbackByEntityEvent event) {
        if (config.get().disableSweepAttack()
                && event.getCause() == EntityKnockbackEvent.Cause.SWEEP_ATTACK) {
            event.setCancelled(true);
        }
    }
}
