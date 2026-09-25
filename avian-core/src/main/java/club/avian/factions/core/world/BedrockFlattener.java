package club.avian.factions.core.world;

import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.function.BooleanSupplier;

/**
 * Flat bedrock for chunks generated before Paper's {@code generate-flat-bedrock} was on. The first
 * time such a chunk loads (the tick after, never inside the load event), bedrock above the bottom
 * layer becomes deepslate (overworld) or netherrack (Nether floor and roof), and the chunk is marked
 * so it is never scanned again. New chunks are generated flat by Paper and only get the mark.
 */
public final class BedrockFlattener implements Listener {

    static final NamespacedKey DONE = new NamespacedKey("avian", "flat_bedrock");
    /** Vanilla scatters bedrock over the five layers at each edge. */
    static final int NOISE_LAYERS = 5;

    private final Plugin plugin;
    private final BooleanSupplier enabled;

    public BedrockFlattener(Plugin plugin, BooleanSupplier enabled) {
        this.plugin = plugin;
        this.enabled = enabled;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onLoad(ChunkLoadEvent event) {
        var chunk = event.getChunk();
        if (!enabled.getAsBoolean() || chunk.getPersistentDataContainer().has(DONE)
                || chunk.getWorld().getEnvironment() == World.Environment.THE_END) {
            return;
        }
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (chunk.isLoaded() && !chunk.getPersistentDataContainer().has(DONE)) {
                flatten(chunk);
            }
        });
    }

    /** Flattens one chunk and marks it. Returns how many bedrock blocks were replaced. */
    public static int flatten(Chunk chunk) {
        var world = chunk.getWorld();
        int changed = 0;
        switch (world.getEnvironment()) {
            case NORMAL -> changed += floor(chunk, world.getMinHeight(), Material.DEEPSLATE);
            case NETHER -> {
                changed += floor(chunk, world.getMinHeight(), Material.NETHERRACK);
                changed += roof(chunk, world.getLogicalHeight() - 1, Material.NETHERRACK);
            }
            default -> { }
        }
        chunk.getPersistentDataContainer().set(DONE, PersistentDataType.BYTE, (byte) 1);
        return changed;
    }

    /** Bedrock only on {@code bottom}; stray bedrock in the layers above becomes {@code fill}. */
    private static int floor(Chunk chunk, int bottom, Material fill) {
        int changed = 0;
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                var base = chunk.getBlock(x, bottom, z);
                if (base.getType() != Material.BEDROCK) {
                    base.setType(Material.BEDROCK, false);
                }
                for (int y = bottom + 1; y < bottom + NOISE_LAYERS; y++) {
                    var b = chunk.getBlock(x, y, z);
                    if (b.getType() == Material.BEDROCK) {
                        b.setType(fill, false);
                        changed++;
                    }
                }
            }
        }
        return changed;
    }

    /** Bedrock only on {@code top} (the Nether ceiling); stray bedrock below it becomes {@code fill}. */
    private static int roof(Chunk chunk, int top, Material fill) {
        int changed = 0;
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                var ceiling = chunk.getBlock(x, top, z);
                if (ceiling.getType() != Material.BEDROCK) {
                    ceiling.setType(Material.BEDROCK, false);
                }
                for (int y = top - 1; y > top - NOISE_LAYERS; y--) {
                    var b = chunk.getBlock(x, y, z);
                    if (b.getType() == Material.BEDROCK) {
                        b.setType(fill, false);
                        changed++;
                    }
                }
            }
        }
        return changed;
    }
}
