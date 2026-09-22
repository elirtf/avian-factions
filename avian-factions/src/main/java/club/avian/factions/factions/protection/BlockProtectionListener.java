package club.avian.factions.factions.protection;

import club.avian.factions.api.config.ConfigHandle;
import club.avian.factions.api.faction.Interaction;
import club.avian.factions.factions.FactionsConfig;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockMultiPlaceEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.block.EntityBlockFormEvent;
import org.bukkit.event.block.SignChangeEvent;

/**
 * Block-level protection (spec §9). Each handler cites the threat it closes; the list and its
 * bug-report provenance come from `docs/research/factions-prior-art.md`.
 */
public final class BlockProtectionListener implements Listener {

    private final ProtectionGuard guard;
    private final ConfigHandle<FactionsConfig> config;

    public BlockProtectionListener(ProtectionGuard guard, ConfigHandle<FactionsConfig> config) {
        this.guard = guard;
        this.config = config;
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        guard.deny(event, event.getPlayer(), event.getBlock(), Interaction.BUILD);
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        guard.deny(event, event.getPlayer(), event.getBlock(), Interaction.BUILD);
    }

    /**
     * Beds, doors and tall plants occupy two blocks, and the second half can land across a claim
     * border. Every replaced state must be checked, not just the clicked block.
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onMultiPlace(BlockMultiPlaceEvent event) {
        for (var state : event.getReplacedBlockStates()) {
            if (guard.deny(event, event.getPlayer(), state.getLocation(), Interaction.BUILD)) {
                return;
            }
        }
    }

    /**
     * A piston can push blocks out of its own chunk into a neighbour's claim, so the piston's
     * chunk is not enough — every destination must be checked (FUUID #1293 was the retract case).
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        var origin = guard.claims().at(event.getBlock());
        for (Block moved : event.getBlocks()) {
            var destination = moved.getRelative(event.getDirection());
            if (crossesIntoProtection(origin, destination)) {
                event.setCancelled(true);
                return;
            }
        }
    }

    /** Sticky pistons and slime/honey blocks pull blocks *out* of a neighbouring claim. */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        var origin = guard.claims().at(event.getBlock());
        for (Block moved : event.getBlocks()) {
            if (crossesIntoProtection(origin, moved)) {
                event.setCancelled(true);
                return;
            }
        }
    }

    /**
     * True when {@code target} belongs to a different faction than {@code origin}. Machinery may
     * move blocks freely within one faction's land, and anywhere in unclaimed land.
     */
    private boolean crossesIntoProtection(club.avian.factions.api.faction.Territory origin, Block target) {
        var destination = guard.claims().at(target);
        if (destination.isWilderness()) {
            return false;
        }
        return !sameOwner(origin, destination);
    }

    private static boolean sameOwner(club.avian.factions.api.faction.Territory a,
                                     club.avian.factions.api.faction.Territory b) {
        var left = a.faction().map(club.avian.factions.api.faction.Faction::id);
        var right = b.faction().map(club.avian.factions.api.faction.Faction::id);
        return left.isPresent() && left.equals(right);
    }

    /** Lava and water flowing across a border — the classic cobble-generator and grief vector. */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onFlow(BlockFromToEvent event) {
        if (config.get().protection().fluidFlowIntoClaims()) {
            return;
        }
        var from = event.getBlock();
        var to = event.getToBlock();
        if (!ProtectionGuard.differentChunk(from.getLocation(), to.getLocation())) {
            return;   // same chunk: same owner by definition, nothing to decide
        }
        var destination = guard.claims().at(to);
        if (destination.isWilderness()) {
            return;
        }
        if (!sameOwner(guard.claims().at(from), destination)) {
            event.setCancelled(true);
        }
    }

    /** Frost walker forming ice in a foreign claim (Saber #122). */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onEntityBlockForm(EntityBlockFormEvent event) {
        ProtectionGuard.playerBehind(event.getEntity()).ifPresent(player ->
                guard.deny(event, player, event.getBlock(), Interaction.BUILD));
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onIgnite(BlockIgniteEvent event) {
        var player = event.getPlayer();
        if (player != null) {
            guard.deny(event, player, event.getBlock(), Interaction.ITEM_USE);
            return;
        }
        // Spread and lava ignition have no player behind them; the territory flag decides.
        if (!config.get().protection().fireSpreadInClaims() && isProtected(event.getBlock())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onBurn(BlockBurnEvent event) {
        if (!config.get().protection().fireSpreadInClaims() && isProtected(event.getBlock())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onSpread(BlockSpreadEvent event) {
        if (event.getNewState().getType().name().contains("FIRE")
                && !config.get().protection().fireSpreadInClaims()
                && isProtected(event.getBlock())) {
            event.setCancelled(true);
        }
    }

    /** Editing signs in someone else's claim (UID #42). */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onSignChange(SignChangeEvent event) {
        guard.deny(event, event.getPlayer(), event.getBlock(), Interaction.BUILD);
    }

    /** A claim that is neither wilderness nor raidable — used by the no-player flag checks. */
    private boolean isProtected(Block block) {
        var territory = guard.claims().at(block);
        return !territory.isWilderness() && guard.policy().denyExplosion(territory);
    }
}
