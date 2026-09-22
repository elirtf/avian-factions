package club.avian.factions.factions.protection;

import club.avian.factions.api.faction.Claims;
import club.avian.factions.api.faction.Faction;
import club.avian.factions.api.faction.Territory;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryMoveItemEvent;

import java.util.Optional;
import java.util.UUID;

/**
 * Hoppers and droppers pulling items across a claim border (Medieval #1868; spec §9 lists
 * "enemy hopper interaction denied" explicitly).
 *
 * <p>There is no player here, so membership cannot be asked — the rule is ownership equality:
 * a container may feed another container only within the same faction's land, or in wilderness.
 */
public final class InventoryProtectionListener implements Listener {

    private final Claims claims;

    public InventoryProtectionListener(Claims claims) {
        this.claims = claims;
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onMoveItem(InventoryMoveItemEvent event) {
        var source = locationOf(event.getSource());
        var destination = locationOf(event.getDestination());
        if (source == null || destination == null) {
            return;
        }
        // Same chunk means same owner; skip before touching the index at all. Hoppers fire
        // constantly, so this guard carries most of the load.
        if (!ProtectionGuard.differentChunk(source, destination)) {
            return;
        }
        if (!sameOwner(claims.at(source), claims.at(destination))) {
            event.setCancelled(true);
        }
    }

    private static Location locationOf(org.bukkit.inventory.Inventory inventory) {
        var holder = inventory.getLocation();
        return holder == null ? null : holder;
    }

    /** True when both chunks are unowned, or owned by the same Faction. */
    private static boolean sameOwner(Territory a, Territory b) {
        Optional<UUID> left = a.faction().map(Faction::id);
        Optional<UUID> right = b.faction().map(Faction::id);
        return left.equals(right);
    }
}
