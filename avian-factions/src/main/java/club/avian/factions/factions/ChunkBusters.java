package club.avian.factions.factions;

import club.avian.factions.api.config.ConfigHandle;
import club.avian.factions.api.text.Brand;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Waterlogged;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.LongSupplier;

/**
 * Chunk busters: a faction tool that clears a chunk of its own land from the placement level down,
 * so a base or cannon has room. Placing one asks for a click-to-confirm (no accidental craters), then
 * the chunk is cleared one layer per tick. Bedrock, spawners, containers and the configured blocks
 * stay; the chunk is held loaded until it is done.
 *
 * <p>Identified by persistent data, never the item's name.
 */
final class ChunkBusters implements Listener {

    static final NamespacedKey ID = new NamespacedKey("avian", "chunk_buster");

    /** A placement waiting for the player's click. */
    record Pending(Location at, long expiresAt) {
    }

    private final Plugin plugin;
    private final ConfigHandle<FactionsConfig> config;
    /** Whether this player may bust this chunk: their own faction's claim. */
    private final BiPredicate<Player, Chunk> ownsChunk;
    private final LongSupplier clock;
    /** Holds a chunk loaded (true) or lets it go (false) while it is being busted. */
    private final BiConsumer<Chunk, Boolean> keepLoaded;
    private final Map<UUID, Pending> pending = new HashMap<>();
    private final Set<Long> running = new HashSet<>();

    ChunkBusters(Plugin plugin, ConfigHandle<FactionsConfig> config, BiPredicate<Player, Chunk> ownsChunk,
                 LongSupplier clock, BiConsumer<Chunk, Boolean> keepLoaded) {
        this.plugin = plugin;
        this.config = config;
        this.ownsChunk = ownsChunk;
        this.clock = clock;
        this.keepLoaded = keepLoaded;
    }

    /** The real hold: a plugin chunk ticket. */
    static BiConsumer<Chunk, Boolean> tickets(Plugin plugin) {
        return (chunk, hold) -> {
            if (hold) {
                chunk.addPluginChunkTicket(plugin);
            } else {
                chunk.removePluginChunkTicket(plugin);
            }
        };
    }

    static ItemStack create(int amount) {
        var item = new ItemStack(Material.END_PORTAL_FRAME, Math.max(1, Math.min(64, amount)));
        item.editPersistentDataContainer(pdc -> pdc.set(ID, PersistentDataType.BYTE, (byte) 1));
        item.editMeta(meta -> {
            meta.itemName(Brand.mm("<bold><gradient:#FF7A45:#FF5C5C:#C58CFF>Chunk Buster</gradient></bold>"));
            meta.setEnchantmentGlintOverride(true);
            meta.lore(List.of(
                    Brand.mm("<soft>Clears the chunk from where it is"),
                    Brand.mm("<soft>placed <hot>all the way down</hot> to bedrock."),
                    net.kyori.adventure.text.Component.empty(),
                    Brand.mm("<soft>Keeps <cane>spawners</cane>, <token>chests</token> and bedrock."),
                    Brand.mm("<soft>Only works in <sun>your faction's land</sun>,"),
                    Brand.mm("<soft>far from spawn."),
                    net.kyori.adventure.text.Component.empty(),
                    Brand.mm("<cane>➜ Place it, then click [BUST CHUNK]</cane>")));
        });
        return item;
    }

    static boolean is(ItemStack item) {
        return item != null && !item.getType().isAir() && item.hasItemMeta()
                && item.getPersistentDataContainer().has(ID, PersistentDataType.BYTE);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        if (!is(event.getItemInHand())) {
            return;
        }
        event.setCancelled(true);   // the buster is never placed as a block; the confirm starts it
        var player = event.getPlayer();
        var cfg = config.get().chunkBuster();
        var at = event.getBlockPlaced().getLocation();
        if (!cfg.enabled()) {
            player.sendMessage(Brand.mm("<bad>Chunk busters are switched off.</bad>"));
            return;
        }
        if (!cfg.environments().contains(at.getWorld().getEnvironment().name())) {
            deny(player, "<bad>Chunk busters don't work in this world.</bad>");
            return;
        }
        if (at.getBlockY() > ceiling(at.getWorld())) {
            deny(player, "<bad>Chunk busters can't be used on or above the Nether roof.</bad>");
            return;
        }
        if (nearSpawn(at, at.getWorld().getSpawnLocation(), cfg.minDistanceFromSpawn())) {
            deny(player, tooCloseMessage(cfg.minDistanceFromSpawn()));
            return;
        }
        if (!ownsChunk.test(player, at.getChunk())) {
            deny(player, "<bad>Chunk busters only work in <sun>your faction's land</sun>.</bad>");
            return;
        }
        if (running.contains(at.getChunk().getChunkKey())) {
            deny(player, "<bad>This chunk is already being busted.</bad>");
            return;
        }
        pending.put(player.getUniqueId(), new Pending(at, clock.getAsLong() + cfg.confirmSeconds() * 1000L));
        player.sendMessage(Brand.mm("<hot><bold>CHUNK BUSTER</bold></hot> <soft>Clear this chunk from Y " + at.getBlockY()
                + " down? </soft>").append(Brand.mm("<cane><bold>[BUST CHUNK]</bold></cane>")
                .clickEvent(ClickEvent.runCommand("/chunkbuster confirm"))
                .hoverEvent(Brand.mm("<soft>Click to start. You have " + cfg.confirmSeconds() + " seconds."))));
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.8f, 0.8f);
    }

    /** {@code /chunkbuster confirm}: checks everything again, takes one buster, starts clearing. */
    void confirm(Player player) {
        var p = pending.remove(player.getUniqueId());
        if (p == null || clock.getAsLong() > p.expiresAt()) {
            deny(player, "<bad>Nothing to confirm. Place a chunk buster first.</bad>");
            return;
        }
        var chunk = p.at().getChunk();
        int minDistance = config.get().chunkBuster().minDistanceFromSpawn();
        if (nearSpawn(p.at(), p.at().getWorld().getSpawnLocation(), minDistance)) {
            deny(player, tooCloseMessage(minDistance));
            return;
        }
        if (!ownsChunk.test(player, chunk)) {
            deny(player, "<bad>That chunk is no longer your faction's land.</bad>");
            return;
        }
        if (!running.add(chunk.getChunkKey())) {
            deny(player, "<bad>This chunk is already being busted.</bad>");
            return;
        }
        if (!takeOne(player)) {
            running.remove(chunk.getChunkKey());
            deny(player, "<bad>You need a chunk buster in your hand.</bad>");
            return;
        }
        player.sendMessage(Brand.mm("<hot><bold>BUSTING!</bold></hot> <soft>Clearing the chunk…"));
        player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 0.6f, 1.2f);
        bust(chunk, p.at().getBlockY(), () -> player.sendMessage(Brand.mm("<cane><bold>DONE!</bold></cane> <soft>The chunk is clear.")));
    }

    /** Whether {@code at} is closer than {@code min} blocks to {@code spawn}, measured flat (X and Z only). */
    static boolean nearSpawn(Location at, Location spawn, int min) {
        double dx = at.getX() - spawn.getX();
        double dz = at.getZ() - spawn.getZ();
        return dx * dx + dz * dz < (double) min * min;
    }

    private static String tooCloseMessage(int min) {
        return "<bad>Chunk busters only work <sun>" + String.format("%,d", min) + " blocks</sun> or more from spawn.</bad>";
    }

    private boolean takeOne(Player player) {
        for (var hand : List.of(EquipmentSlot.HAND, EquipmentSlot.OFF_HAND)) {
            var item = player.getInventory().getItem(hand);
            if (is(item)) {
                item.setAmount(item.getAmount() - 1);
                player.getInventory().setItem(hand, item.getAmount() > 0 ? item : null);
                return true;
            }
        }
        return false;
    }

    /** Clears {@code chunk} from {@code topY} down to just above the world's bottom. */
    void bust(Chunk chunk, int topY, Runnable done) {
        var keep = EnumSet.noneOf(Material.class);
        for (var name : config.get().chunkBuster().keep()) {
            var m = Material.matchMaterial(name);
            if (m != null) {
                keep.add(m);
            }
        }
        int perTick = config.get().chunkBuster().layersPerTick();
        var world = chunk.getWorld();
        int bottom = world.getMinHeight();
        int top = Math.min(topY, ceiling(world));
        Material seal = world.getEnvironment() == World.Environment.NETHER ? Material.NETHERRACK : Material.STONE;
        keepLoaded.accept(chunk, true);
        var task = new BukkitTask[1];
        int[] y = {top};
        task[0] = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            for (int layer = 0; layer < perTick && y[0] > bottom; layer++, y[0]--) {
                for (int x = 0; x < 16; x++) {
                    for (int z = 0; z < 16; z++) {
                        clear(chunk.getBlock(x, y[0], z), keep, seal, y[0] == top);
                    }
                }
            }
            if (y[0] <= bottom) {
                task[0].cancel();
                keepLoaded.accept(chunk, false);
                running.remove(chunk.getChunkKey());
                done.run();
            }
        }, 1L, 1L);
    }

    /**
     * Clears one block. Where the hole would let liquid in (lava or water across the chunk border,
     * or sitting on top of the cleared area) it becomes {@code seal} instead of air, so busting never
     * floods a chunk or starts a lava flow. Liquid inside the chunk is simply removed.
     */
    static void clear(Block block, Set<Material> keep, Material seal, boolean topLayer) {
        var type = block.getType();
        if (type.isAir() || keep.contains(type) || block.getState(false) instanceof Container) {
            return;
        }
        block.setType(leaks(block, topLayer) ? seal : Material.AIR, false);
    }

    /** Whether liquid outside the cleared area touches this block. Unknown (unloaded) counts as yes. */
    static boolean leaks(Block block, boolean topLayer) {
        if (topLayer && liquid(block.getRelative(BlockFace.UP))) {
            return true;
        }
        int lx = block.getX() & 15;
        int lz = block.getZ() & 15;
        for (var face : new BlockFace[] {BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST}) {
            boolean crossesBorder = (face == BlockFace.WEST && lx == 0) || (face == BlockFace.EAST && lx == 15)
                    || (face == BlockFace.NORTH && lz == 0) || (face == BlockFace.SOUTH && lz == 15);
            if (!crossesBorder) {
                continue;
            }
            int nx = block.getX() + face.getModX();
            int nz = block.getZ() + face.getModZ();
            if (!block.getWorld().isChunkLoaded(nx >> 4, nz >> 4) || liquid(block.getWorld().getBlockAt(nx, block.getY(), nz))) {
                return true;
            }
        }
        return false;
    }

    private static boolean liquid(Block block) {
        var type = block.getType();
        return type == Material.LAVA || type == Material.WATER || type == Material.BUBBLE_COLUMN
                || (block.getBlockData() instanceof Waterlogged w && w.isWaterlogged());
    }

    /** The highest Y a buster may clear: below the Nether roof, else the build limit. */
    static int ceiling(World world) {
        return world.getEnvironment() == World.Environment.NETHER ? world.getLogicalHeight() - 2 : world.getMaxHeight() - 1;
    }

    private static void deny(Player player, String message) {
        player.sendMessage(Brand.mm(message));
        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.7f, 1.0f);
    }
}
