package club.avian.factions.factions;

import club.avian.factions.api.config.ConfigHandle;
import club.avian.factions.api.text.Brand;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.persistence.PersistentDataType;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.LongSupplier;

/**
 * Tells the whole server when someone mines ancient debris (owner, 2026-09-29), so players go looking
 * for each other: to fight over it or to team up. Debris a player placed never counts (the chunk
 * remembers where), and one player is announced at most once per cooldown, so a vein is one message.
 */
final class NetheriteAnnouncer implements Listener {

    /** Where in this chunk players placed debris ({@link #keyInChunk}). */
    static final NamespacedKey PLACED = new NamespacedKey("avian", "placed_debris");

    private final ConfigHandle<FactionsConfig> config;
    private final LongSupplier clock;
    private final Map<UUID, Long> lastAnnounced = new HashMap<>();

    NetheriteAnnouncer(ConfigHandle<FactionsConfig> config, LongSupplier clock) {
        this.config = config;
        this.clock = clock;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        if (event.getBlockPlaced().getType() == Material.ANCIENT_DEBRIS) {
            setPlaced(event.getBlockPlaced(), true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        var block = event.getBlock();
        if (block.getType() != Material.ANCIENT_DEBRIS) {
            return;
        }
        if (setPlaced(block, false)) {
            return;   // a player put it there: not a find
        }
        var cfg = config.get().netheriteAlert();
        var player = event.getPlayer();
        long now = clock.getAsLong();
        Long last = lastAnnounced.get(player.getUniqueId());
        if (!cfg.enabled() || (last != null && now - last < cfg.cooldownSeconds() * 1000L)) {
            return;
        }
        lastAnnounced.put(player.getUniqueId(), now);
        // Player names are letters, digits, _ and Floodgate's leading dot: nothing MiniMessage reads as a tag.
        Bukkit.broadcast(Brand.mm("<hot>⛏</hot> <white>" + player.getName() + "</white> <soft>just found</soft> "
                + "<bold><gradient:#8C5CFF:#FF5C5C>Ancient Debris</gradient></bold> <soft>" + where(block.getWorld()) + "!</soft>"));
        for (var online : Bukkit.getOnlinePlayers()) {
            online.playSound(online.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 0.6f, 0.7f);
        }
    }

    private static String where(World world) {
        return switch (world.getEnvironment()) {
            case NETHER -> "in the Nether";
            case THE_END -> "in the End";
            default -> "in " + world.getName();
        };
    }

    /** The block's position inside its chunk as one number: height, then X and Z within the chunk. */
    static long keyInChunk(Block block) {
        return ((long) block.getY() << 8) | ((block.getX() & 15) << 4) | (block.getZ() & 15);
    }

    /**
     * Records ({@code placed}) or forgets a placed debris block in its chunk's data. When forgetting,
     * returns whether it had been placed by a player.
     */
    static boolean setPlaced(Block block, boolean placed) {
        var data = block.getChunk().getPersistentDataContainer();
        long key = keyInChunk(block);
        long[] keys = data.getOrDefault(PLACED, PersistentDataType.LONG_ARRAY, new long[0]);
        boolean had = Arrays.stream(keys).anyMatch(k -> k == key);
        long[] next = keys;
        if (placed && !had) {
            next = Arrays.copyOf(keys, keys.length + 1);
            next[keys.length] = key;
        } else if (!placed) {
            next = Arrays.stream(keys).filter(k -> k != key).toArray();
        }
        if (next.length == 0) {
            data.remove(PLACED);
        } else {
            data.set(PLACED, PersistentDataType.LONG_ARRAY, next);
        }
        return had;
    }
}
