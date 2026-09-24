package club.avian.factions.economy;

import club.avian.factions.api.config.ConfigHandle;
import club.avian.factions.api.economy.Currency;
import club.avian.factions.api.economy.Economy;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.persistence.PersistentDataType;

import java.util.Arrays;
import java.util.function.DoubleSupplier;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Tokens from farming (spec §30): a player harvesting grown sugar cane by hand has a chance of
 * earning tokens per block. Money comes from selling the cane, including from auto-farms; tokens
 * need a player swinging.
 *
 * <p>Cane a player placed is remembered in its chunk's data and never pays, so placing and breaking
 * the same cane earns nothing. A farm's bottom block is always placed, which is fine: harvesting
 * leaves it and breaks the grown blocks above.
 */
final class SugarCaneTokens implements Listener {

    private static final NamespacedKey PLACED = new NamespacedKey("avian", "placed_cane");
    private static final TextColor TOKEN_GOLD = TextColor.color(0xF2D06B);

    private final Economy economy;
    private final ConfigHandle<EconomyConfig> config;
    private final DoubleSupplier random;
    private final Logger log;

    SugarCaneTokens(Economy economy, ConfigHandle<EconomyConfig> config, DoubleSupplier random, Logger log) {
        this.economy = economy;
        this.config = config;
        this.random = random;
        this.log = log;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        if (event.getBlockPlaced().getType() == Material.SUGAR_CANE) {
            mark(event.getBlockPlaced());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        var block = event.getBlock();
        if (block.getType() != Material.SUGAR_CANE) {
            return;
        }
        var player = event.getPlayer();
        var cfg = config.get();
        // Breaking one block pops every cane block above it, without events of their own.
        int grown = 0;
        for (var b = block; b.getType() == Material.SUGAR_CANE; b = b.getRelative(0, 1, 0)) {
            if (!unmark(b)) {
                grown++;
            }
        }
        if (player.getGameMode() == GameMode.CREATIVE || cfg.sugarCaneTokens() == 0) {
            return;
        }
        long earned = 0;
        for (int i = 0; i < grown; i++) {
            if (random.getAsDouble() < cfg.sugarCaneTokenChance()) {
                earned += cfg.sugarCaneTokens();
            }
        }
        if (earned == 0) {
            return;
        }
        long amount = earned;
        economy.deposit(player.getUniqueId(), Currency.TOKENS, amount, "farming:sugar_cane")
                .thenAccept(result -> {
                    if (result.ok()) {
                        player.sendActionBar(Component.text("+" + amount + (amount == 1 ? " token" : " tokens"), TOKEN_GOLD));
                    }
                })
                .exceptionally(t -> {
                    log.log(Level.WARNING, "Could not pay sugar cane tokens to " + player.getName(), t);
                    return null;
                });
    }

    // --- placed-cane markers, one int per block, in the chunk's persistent data ----------------

    static void mark(Block block) {
        var pdc = block.getChunk().getPersistentDataContainer();
        int[] placed = pdc.getOrDefault(PLACED, PersistentDataType.INTEGER_ARRAY, new int[0]);
        int key = key(block);
        if (Arrays.stream(placed).noneMatch(k -> k == key)) {
            int[] grown = Arrays.copyOf(placed, placed.length + 1);
            grown[placed.length] = key;
            pdc.set(PLACED, PersistentDataType.INTEGER_ARRAY, grown);
        }
    }

    /** Forgets a placed-cane marker. True if there was one, i.e. a player placed this block. */
    static boolean unmark(Block block) {
        var pdc = block.getChunk().getPersistentDataContainer();
        int[] placed = pdc.get(PLACED, PersistentDataType.INTEGER_ARRAY);
        if (placed == null) {
            return false;
        }
        int key = key(block);
        int[] rest = Arrays.stream(placed).filter(k -> k != key).toArray();
        if (rest.length == placed.length) {
            return false;
        }
        if (rest.length == 0) {
            pdc.remove(PLACED);
        } else {
            pdc.set(PLACED, PersistentDataType.INTEGER_ARRAY, rest);
        }
        return true;
    }

    /** Position within the chunk: 4 bits x, 4 bits z, the rest y (offset so it is never negative). */
    private static int key(Block block) {
        return ((block.getY() + 4096) << 8) | ((block.getX() & 15) << 4) | (block.getZ() & 15);
    }
}
