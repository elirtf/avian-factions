package club.avian.factions.factions.protection;

import club.avian.factions.api.faction.Interaction;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketEntityEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerShearEntityEvent;
import org.bukkit.event.player.PlayerTakeLecternBookEvent;

/**
 * Player interaction protection (spec §9). The hard part is classification: one event covers
 * chests, doors, buttons, farmland trampling and every item used on a block, and each needs a
 * different {@link Interaction} so the non-member allow list can be granular.
 */
public final class InteractProtectionListener implements Listener {

    private final ProtectionGuard guard;

    public InteractProtectionListener(ProtectionGuard guard) {
        this.guard = guard;
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        var block = event.getClickedBlock();
        if (block == null) {
            return;
        }
        // PHYSICAL is standing on a pressure plate or trampling farmland; it fires every tick,
        // which is exactly why deny messages are throttled per reason.
        if (event.getAction() == Action.PHYSICAL) {
            guard.deny(event, event.getPlayer(), block, Interaction.SWITCH);
            return;
        }
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        guard.deny(event, event.getPlayer(), block, classify(block));
    }

    /** Which interaction kind a clicked block represents. */
    private static Interaction classify(Block block) {
        var type = block.getType();
        if (block.getState() instanceof Container || type == Material.ENDER_CHEST) {
            return Interaction.CONTAINER;
        }
        if (Tag.DOORS.isTagged(type) || Tag.TRAPDOORS.isTagged(type) || Tag.FENCE_GATES.isTagged(type)) {
            return Interaction.DOOR;
        }
        if (Tag.BUTTONS.isTagged(type) || Tag.PRESSURE_PLATES.isTagged(type) || type == Material.LEVER) {
            return Interaction.SWITCH;
        }
        // Anvils, enchanting tables, beds, respawn anchors, jukeboxes, cauldrons and the rest are
        // world state a non-member should not touch by default.
        return Interaction.BUILD;
    }

    /** Villager trading, item-frame rotation, leashing, dyeing, name-tagging. */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        guard.deny(event, event.getPlayer(), event.getRightClicked().getLocation(), Interaction.ENTITY);
    }

    /** Stealing equipment off armour stands (FUUID #1277). */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onArmorStand(PlayerArmorStandManipulateEvent event) {
        guard.deny(event, event.getPlayer(), event.getRightClicked().getLocation(), Interaction.ENTITY);
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onBucketEmpty(PlayerBucketEmptyEvent event) {
        guard.deny(event, event.getPlayer(), event.getBlock(), Interaction.ITEM_USE);
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onBucketFill(PlayerBucketFillEvent event) {
        guard.deny(event, event.getPlayer(), event.getBlock(), Interaction.ITEM_USE);
    }

    /** Capturing fish and axolotls out of someone's claim. */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onBucketEntity(PlayerBucketEntityEvent event) {
        guard.deny(event, event.getPlayer(), event.getEntity().getLocation(), Interaction.ENTITY);
    }

    /** Book theft from lecterns. */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onLecternBook(PlayerTakeLecternBookEvent event) {
        guard.deny(event, event.getPlayer(), event.getLectern().getLocation(), Interaction.CONTAINER);
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onShear(PlayerShearEntityEvent event) {
        guard.deny(event, event.getPlayer(), event.getEntity().getLocation(), Interaction.ENTITY);
    }
}
