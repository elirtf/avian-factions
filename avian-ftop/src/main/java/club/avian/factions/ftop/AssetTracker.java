package club.avian.factions.ftop;

import club.avian.factions.api.economy.Currency;
import club.avian.factions.api.economy.Economy;
import dev.rosewood.rosestacker.event.SpawnerStackEvent;
import dev.rosewood.rosestacker.event.SpawnerUnstackEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.plugin.Plugin;
import org.jspecify.annotations.Nullable;

import java.time.Clock;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Keeps the {@link AssetBook} in line with the world. Events only say <em>which positions</em> to
 * look at; one tick later, when RoseStacker and the explosion have finished, the tracker reads
 * what is really there. That makes every path the same: placing, stacking, breaking, unstacking,
 * TNT (RoseStacker drops 75% and destroys the rest), creepers, pistons.
 *
 * <p>The pickup cost is decided when RoseStacker announces a player unstack, and taken only once
 * the next tick shows the spawners actually left. Explosions have no player, so never pay.
 */
public final class AssetTracker implements Listener {

    /** A player's pickup, waiting for the next tick to confirm it happened. */
    private record PendingPickup(UUID player, int freeUnits, Valuation.Cost cost) {
    }

    private final Plugin plugin;
    private final AssetBook book;
    private final AssetRepository repository;
    private final SpawnerStacks stacks;
    private final Supplier<Valuation> valuation;
    private final Supplier<FTopConfig.PickupCost> pickupConfig;
    private final Function<AssetKey, Ranking.@Nullable Owner> ownerOf;
    private final @Nullable Economy economy;
    private final Clock clock;
    private final Logger log;

    /** Positions to read next tick; true when the position may start being tracked. */
    private final Map<AssetKey, Boolean> queued = new LinkedHashMap<>();
    private final Map<AssetKey, PendingPickup> pickups = new HashMap<>();
    private boolean flushScheduled;

    public AssetTracker(Plugin plugin, AssetBook book, AssetRepository repository, SpawnerStacks stacks,
                        Supplier<Valuation> valuation, Supplier<FTopConfig.PickupCost> pickupConfig,
                        Function<AssetKey, Ranking.@Nullable Owner> ownerOf, @Nullable Economy economy,
                        Clock clock, Logger log) {
        this.plugin = plugin;
        this.book = book;
        this.repository = repository;
        this.stacks = stacks;
        this.valuation = valuation;
        this.pickupConfig = pickupConfig;
        this.ownerOf = ownerOf;
        this.economy = economy;
        this.clock = clock;
        this.log = log;
    }

    // --- Events: they only queue positions -------------------------------------------------------

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        Block block = event.getBlockPlaced();
        if (block.getType() == Material.SPAWNER || valuation.get().isValuableBlock(block.getType().name())) {
            queue(AssetKey.of(block), true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onStack(SpawnerStackEvent event) {
        queue(AssetKey.of(event.getStack().getLocation()), true);
    }

    /** Before RoseStacker drops anything: refuse the pickup if the player cannot pay. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onUnstack(SpawnerUnstackEvent event) {
        var key = AssetKey.of(event.getStack().getLocation());
        Player player = event.getPlayer();
        if (player != null && !checkPickup(player, key, event.getDecreaseAmount())) {
            event.setCancelled(true);
            return;
        }
        queue(key, false);
    }

    /** Valuable blocks broken by hand (spawners arrive through {@link #onUnstack}). */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        var key = AssetKey.of(event.getBlock());
        if (!book.isTracked(key)) {
            return;
        }
        var asset = book.get(key);
        if (asset != null && asset.kind() == AssetKind.BLOCK && !checkPickup(event.getPlayer(), key, 1)) {
            event.setCancelled(true);
            return;
        }
        queue(key, false);
    }

    /** LOWEST: RoseStacker takes spawners out of the block list before later handlers see it. */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onEntityExplode(EntityExplodeEvent event) {
        queueTracked(event.blockList());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBlockExplode(BlockExplodeEvent event) {
        queueTracked(event.blockList());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        moveWithPiston(event.getBlocks(), event.getDirection());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        moveWithPiston(event.getBlocks(), event.getDirection());
    }

    // --- The work ----------------------------------------------------------------------------------

    /**
     * Whether {@code player} may pick up {@code units} at {@code key}, recording the cost to take
     * next tick. Messages the player when refused. Free for untracked positions, positions outside
     * any faction's claim, and bypass holders.
     */
    boolean checkPickup(Player player, AssetKey key, int units) {
        var asset = book.get(key);
        var cfg = pickupConfig.get();
        if (asset == null || !cfg.enabled() || player.hasPermission(cfg.bypassPermission()) || ownerOf.apply(key) == null) {
            return true;
        }
        long now = clock.millis();
        var cost = valuation.get().pickupCost(asset, units, now);
        int free = Math.min(units, asset.freeUnits(now));
        if (cost.amount() > 0) {
            if (economy == null) {
                player.sendMessage(Component.text("Spawner pickup is unavailable: the economy is not running.", NamedTextColor.RED));
                return false;
            }
            long balance = economy.balance(player.getUniqueId(), Currency.MONEY);
            if (balance < cost.amount()) {
                player.sendMessage(Component.text("Picking this up costs " + economy.format(Currency.MONEY, cost.amount())
                        + " (" + cfg.percentOfValue() + "% of its value, after the " + cfg.graceMinutes()
                        + "-minute grace). You have " + economy.format(Currency.MONEY, balance) + ".", NamedTextColor.RED));
                return false;
            }
        }
        pickups.put(key, new PendingPickup(player.getUniqueId(), free, cost));
        return true;
    }

    void queue(AssetKey key, boolean mayCreate) {
        queued.merge(key, mayCreate, Boolean::logicalOr);
        if (!flushScheduled) {
            flushScheduled = true;
            Bukkit.getScheduler().runTask(plugin, this::flush);
        }
    }

    private void queueTracked(List<Block> blocks) {
        for (Block block : blocks) {
            var key = AssetKey.of(block);
            if (book.isTracked(key)) {
                queue(key, false);
            }
        }
    }

    private void moveWithPiston(List<Block> blocks, BlockFace direction) {
        var fromTo = new LinkedHashMap<AssetKey, AssetKey>();
        for (Block block : blocks) {
            var from = AssetKey.of(block);
            if (book.isTracked(from)) {
                fromTo.put(from, from.offset(direction.getModX(), direction.getModY(), direction.getModZ()));
            }
        }
        if (fromTo.isEmpty()) {
            return;
        }
        book.move(fromTo).forEach(this::persist);
        // Check both ends next tick, in case the move went somewhere other than expected.
        fromTo.forEach((from, to) -> {
            queue(from, false);
            queue(to, false);
        });
    }

    /** Reads every queued position and applies it. Runs one tick after the events. */
    void flush() {
        flushScheduled = false;
        long now = clock.millis();
        var batch = new LinkedHashMap<>(queued);
        queued.clear();
        batch.forEach((key, mayCreate) -> {
            var reading = read(key);
            if (reading == null) {
                return;
            }
            var change = book.observe(key, reading.seen(), mayCreate, now);
            persist(change);
            settlePickup(key, change);
        });
        pickups.clear();   // any left belong to breaks that were cancelled after we saw them
    }

    /** Re-reads every tracked asset in a loaded chunk; catches anything no event reported. */
    void heal() {
        long now = clock.millis();
        for (Asset asset : book.all()) {
            var reading = read(asset.key());
            if (reading != null) {
                persist(book.observe(asset.key(), reading.seen(), false, now));
            }
        }
    }

    private void settlePickup(AssetKey key, AssetBook.Change change) {
        var pickup = pickups.remove(key);
        if (pickup == null || change.removedUnits() == 0) {
            return;
        }
        book.spendGrace(key, pickup.freeUnits());
        if (book.get(key) != null) {
            persist(new AssetBook.Change(null, book.get(key)));
        }
        if (pickup.cost().amount() > 0 && economy != null) {
            economy.withdraw(pickup.player(), Currency.MONEY, pickup.cost().amount(), "ftop:pickup")
                    .whenComplete((result, failure) -> {
                        if (failure != null || !result.ok()) {
                            // The balance was checked a tick ago; losing a race with another
                            // purchase in that tick lets this one through. Logged, not reversed.
                            log.warning("Pickup cost of " + pickup.cost().amount() + " not collected from "
                                    + pickup.player() + " at " + key + ": "
                                    + (failure != null ? failure.getMessage() : result));
                        }
                    });
            var player = Bukkit.getPlayer(pickup.player());
            if (player != null) {
                player.sendMessage(Component.text("Paid " + economy.format(Currency.MONEY, pickup.cost().amount())
                        + " to pick up " + pickup.cost().chargedUnits() + " locked spawner"
                        + (pickup.cost().chargedUnits() == 1 ? "" : "s") + ".", NamedTextColor.GOLD));
            }
        }
    }

    private void persist(AssetBook.Change change) {
        if (!change.changed()) {
            return;
        }
        var future = change.after() != null
                ? repository.save(change.after())
                : repository.delete(change.before().key());
        future.exceptionally(t -> {
            log.log(Level.SEVERE, "Could not save F-Top asset " + (change.after() != null ? change.after() : change.before()), t);
            return null;
        });
    }

    /** What a position holds; {@code seen} is null when it holds nothing valuable. */
    private record Reading(@Nullable Observed seen) {
    }

    /** Reads {@code key}, or null when its chunk is not loaded and the asset must be left alone. */
    private @Nullable Reading read(AssetKey key) {
        var world = Bukkit.getWorld(key.world());
        if (world == null || !world.isChunkLoaded(key.x() >> 4, key.z() >> 4)) {
            return null;
        }
        Block block = world.getBlockAt(key.x(), key.y(), key.z());
        if (block.getType() == Material.SPAWNER) {
            var spawned = ((CreatureSpawner) block.getState(false)).getSpawnedType();
            String type = spawned == null ? "EMPTY" : spawned.name();
            return new Reading(new Observed(AssetKind.SPAWNER, type, stacks.size(block)));
        }
        String material = block.getType().name().toUpperCase(Locale.ROOT);
        return new Reading(valuation.get().isValuableBlock(material) ? new Observed(AssetKind.BLOCK, material, 1) : null);
    }
}
