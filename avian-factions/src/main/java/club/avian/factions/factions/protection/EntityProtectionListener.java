package club.avian.factions.factions.protection;

import club.avian.factions.api.faction.Interaction;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityCombustByEntityEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityPlaceEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.event.vehicle.VehicleDamageEvent;
import org.bukkit.event.vehicle.VehicleDestroyEvent;

/**
 * Entity-borne protection (spec §9): the "blocks that are technically entities" — item frames,
 * paintings, armour stands, vehicles, end crystals — plus PvP gating and damage to animals.
 */
public final class EntityProtectionListener implements Listener {

    private final ProtectionGuard guard;

    public EntityProtectionListener(ProtectionGuard guard) {
        this.guard = guard;
    }

    /** End crystals, boats, minecarts and armour stands placed in a claim (IF #181). */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onEntityPlace(EntityPlaceEvent event) {
        var player = event.getPlayer();
        if (player != null) {
            guard.deny(event, player, event.getBlock().getLocation(), Interaction.BUILD);
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onHangingPlace(HangingPlaceEvent event) {
        var player = event.getPlayer();
        if (player != null) {
            guard.deny(event, player, event.getEntity().getLocation(), Interaction.ENTITY);
        }
    }

    /** Item frames and paintings broken by a player or their projectile (FUUID #1375). */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onHangingBreak(HangingBreakByEntityEvent event) {
        ProtectionGuard.playerBehind(event.getRemover()).ifPresent(player ->
                guard.deny(event, player, event.getEntity().getLocation(), Interaction.ENTITY));
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onVehicleDamage(VehicleDamageEvent event) {
        ProtectionGuard.playerBehind(event.getAttacker()).ifPresent(player ->
                guard.deny(event, player, event.getVehicle().getLocation(), Interaction.ENTITY));
    }

    /** Chest minecarts are storage on wheels; destroying one is a container theft. */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onVehicleDestroy(VehicleDestroyEvent event) {
        ProtectionGuard.playerBehind(event.getAttacker()).ifPresent(player ->
                guard.deny(event, player, event.getVehicle().getLocation(), Interaction.ENTITY));
    }

    /**
     * Endermen lifting blocks, silverfish infesting, withers breaking, falling blocks landing
     * across a border (FUUID #1443). Only player-caused changes are judged by membership; the
     * rest are left to vanilla, since a mob is not an actor with a faction.
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onEntityChangeBlock(EntityChangeBlockEvent event) {
        ProtectionGuard.playerBehind(event.getEntity()).ifPresent(player ->
                guard.deny(event, player, event.getBlock(), Interaction.BUILD));
    }

    /** Flame arrows and fire aspect igniting a protected player. */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onCombust(EntityCombustByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) {
            return;
        }
        ProtectionGuard.playerBehind(event.getCombuster()).ifPresent(attacker -> {
            var decision = guard.policy().canDamagePlayer(attacker.getUniqueId(), victim.getUniqueId(),
                    guard.claims().at(attacker.getLocation()), guard.claims().at(victim.getLocation()));
            if (decision.denied()) {
                event.setCancelled(true);
            }
        });
    }

    /**
     * Damage by an entity, split two ways: player-versus-player goes through the PvP rule, and
     * damage to anything else (animals, villagers, golems, item frames) is a claim interaction.
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        var attacker = ProtectionGuard.playerBehind(event.getDamager()).orElse(null);
        if (attacker == null) {
            return;
        }
        if (event.getEntity() instanceof Player victim) {
            var decision = guard.policy().canDamagePlayer(attacker.getUniqueId(), victim.getUniqueId(),
                    guard.claims().at(attacker.getLocation()), guard.claims().at(victim.getLocation()));
            if (decision.denied()) {
                event.setCancelled(true);
                guard.deny(event, attacker, guard.claims().at(victim.getLocation()), Interaction.DAMAGE_ENTITY);
            }
            return;
        }
        guard.deny(event, attacker, event.getEntity().getLocation(), Interaction.DAMAGE_ENTITY);
    }
}
