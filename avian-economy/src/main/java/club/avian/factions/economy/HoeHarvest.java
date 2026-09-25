package club.avian.factions.economy;

import club.avian.factions.api.config.ConfigHandle;
import club.avian.factions.api.economy.Currency;
import club.avian.factions.api.economy.Economy;
import club.avian.factions.api.economy.SellValues;
import club.avian.factions.economy.HarvesterHoe.Track;
import club.avian.factions.api.text.Brand;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.function.DoubleSupplier;
import java.util.function.ObjDoubleConsumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Breaking sugar cane with a Harvester Hoe harvests every block above the root, in the hoe's
 * radius, and pays for it: money (auto-sell, times the multiplier), tokens (the hand-harvest chance,
 * boosted) and random drops.
 *
 * <p>The real break is cancelled and the hoe does the harvest itself, so the plain token listener
 * never also pays for it. Neighbouring columns are only harvested where the player could break them:
 * each is checked with a break event of its own, the way claim and region plugins expect.
 */
final class HoeHarvest implements Listener {

    /** True while this class is asking other plugins whether a neighbouring column may be broken. */
    static final ThreadLocal<Boolean> PROBING = ThreadLocal.withInitial(() -> false);


    private final Economy economy;
    private final SellValues sellValues;
    private final ConfigHandle<HoeConfig> hoe;
    private final ConfigHandle<EconomyConfig> economyConfig;
    private final DoubleSupplier random;
    private final ObjDoubleConsumer<Player> farmingXp;
    private final Logger log;
    /** Whether the last probe was allowed by every other plugin; read right after the probe. */
    private boolean probeAllowed;

    HoeHarvest(Economy economy, SellValues sellValues, ConfigHandle<HoeConfig> hoe,
               ConfigHandle<EconomyConfig> economyConfig, DoubleSupplier random,
               ObjDoubleConsumer<Player> farmingXp, Logger log) {
        this.economy = economy;
        this.sellValues = sellValues;
        this.hoe = hoe;
        this.economyConfig = economyConfig;
        this.random = random;
        this.farmingXp = farmingXp;
        this.log = log;
    }

    /** What one harvest took. */
    record Harvest(int cane, int grown) {
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        if (PROBING.get() || event.getBlock().getType() != Material.SUGAR_CANE) {
            return;
        }
        var player = event.getPlayer();
        var tool = player.getInventory().getItemInMainHand();
        if (!HarvesterHoe.is(tool) || player.getGameMode() == GameMode.CREATIVE) {
            return;
        }
        event.setCancelled(true);   // the hoe harvests; the root stays, and no vanilla drop happens

        var cfg = hoe.get();
        int radius = Math.min(HarvesterHoe.level(tool, Track.RADIUS) * Math.max(1, cfg.radius().perLevel()), 4);
        var hit = root(event.getBlock());
        int cane = 0;
        int grown = 0;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                var r = dx == 0 && dz == 0 ? hit : root(hit.getRelative(dx, 0, dz));
                if (r == null || r.getType() != Material.SUGAR_CANE) {
                    continue;
                }
                if (!(dx == 0 && dz == 0) && !mayBreak(player, r.getRelative(0, 1, 0))) {
                    continue;
                }
                var h = harvestColumn(r);
                cane += h.cane();
                grown += h.grown();
            }
        }
        if (cane > 0) {
            pay(player, tool, new Harvest(cane, grown));
        }
    }

    /** The lowest cane block of the column containing {@code block}, or null if it is not cane. */
    static Block root(Block block) {
        if (block.getType() != Material.SUGAR_CANE) {
            return null;
        }
        var b = block;
        while (b.getRelative(0, -1, 0).getType() == Material.SUGAR_CANE) {
            b = b.getRelative(0, -1, 0);
        }
        return b;
    }

    /** Removes every cane block above {@code root}, top down so nothing pops and drops. */
    static Harvest harvestColumn(Block root) {
        var above = new ArrayList<Block>();
        for (var b = root.getRelative(0, 1, 0); b.getType() == Material.SUGAR_CANE; b = b.getRelative(0, 1, 0)) {
            above.add(b);
        }
        int grown = 0;
        for (int i = above.size() - 1; i >= 0; i--) {
            var b = above.get(i);
            if (!SugarCaneTokens.unmark(b)) {
                grown++;   // placed cane still sells, but never earns tokens
            }
            b.setType(Material.AIR, false);
        }
        return new Harvest(above.size(), grown);
    }

    private boolean mayBreak(Player player, Block block) {
        if (block.getType() != Material.SUGAR_CANE) {
            return false;
        }
        PROBING.set(true);
        probeAllowed = false;
        try {
            Bukkit.getPluginManager().callEvent(new BlockBreakEvent(block, player));
            return probeAllowed;
        } finally {
            PROBING.set(false);
        }
    }

    /**
     * Ends a probe once every protection plugin has had its say: note the verdict, then cancel it so
     * MONITOR listeners (skills XP, block logging) never treat the probe as a real break. The hoe pays
     * XP for the whole harvest itself.
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onProbe(BlockBreakEvent event) {
        if (PROBING.get()) {
            probeAllowed = !event.isCancelled();
            event.setCancelled(true);
        }
    }

    private void pay(Player player, ItemStack tool, Harvest h) {
        var cfg = hoe.get();
        var summary = new ArrayList<String>();

        // Money, or the cane itself.
        long money = HarvesterHoe.autoSell(tool, cfg) ? SellValues.applyMultiplier(priceOf(player, h.cane()),
                10_000 + 100 * cfg.moneyMultiplier().perLevel() * HarvesterHoe.level(tool, Track.MONEY_MULTIPLIER)) : 0;
        if (money > 0) {
            deposit(player, Currency.MONEY, money, "hoe:sugar_cane");
            summary.add("<money>+" + economy.format(Currency.MONEY, money) + "</money>");
        } else {
            give(player, new ItemStack(Material.SUGAR_CANE, h.cane()));
            summary.add("<cane>+" + h.cane() + " cane</cane>");
        }

        // Tokens: the hand-harvest chance, boosted.
        double chance = economyConfig.get().sugarCaneTokenChance()
                * (1 + cfg.tokenBoost().perLevel() / 100.0 * HarvesterHoe.level(tool, Track.TOKEN_BOOST));
        long tokens = 0;
        for (int i = 0; i < h.grown(); i++) {
            if (random.getAsDouble() < chance) {
                tokens += economyConfig.get().sugarCaneTokens();
            }
        }

        // Random drops.
        int dropLevel = HarvesterHoe.level(tool, Track.RANDOM_DROPS);
        var lucky = new ArrayList<String>();
        if (dropLevel > 0) {
            double scale = dropLevel * Math.max(1, cfg.randomDrops().perLevel());
            for (int i = 0; i < h.grown(); i++) {
                for (var drop : cfg.drops()) {
                    if (random.getAsDouble() < drop.chance() * scale) {
                        tokens += drop.tokens();
                        if (!drop.command().isBlank()) {
                            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), drop.command().replace("{player}", player.getName()));
                        }
                        if (!drop.message().isBlank()) {
                            lucky.add(drop.message());
                        }
                    }
                }
            }
        }
        // Farming XP: what a hand harvest would give, plus Cultivation.
        double xp = h.grown() * cfg.farmingXpPerCane()
                * (1 + cfg.cultivation().perLevel() / 100.0 * HarvesterHoe.level(tool, Track.CULTIVATION));
        if (xp > 0) {
            farmingXp.accept(player, xp);
            summary.add("<xp>+" + new DecimalFormat("#,##0.#").format(xp) + " XP</xp>");
        }

        if (tokens > 0) {
            deposit(player, Currency.TOKENS, tokens, "hoe:sugar_cane");
            summary.add("<token>+" + new DecimalFormat("#,##0").format(tokens) + (tokens == 1 ? " token" : " tokens") + "</token>");
        }
        if (!summary.isEmpty()) {
            player.sendActionBar(Brand.mm(String.join("  <dim>·</dim>  ", summary)));
        }
        for (var message : lucky) {
            player.sendMessage(Brand.mm("<gem><bold>LUCKY HARVEST!</bold></gem> <soft>You found <sun>" + message + "</sun>."));
        }
    }

    /** Priced in stacks of 64: the shop prices real item stacks, which cannot be larger. */
    private long priceOf(Player player, int cane) {
        long total = 0;
        for (int left = cane; left > 0; left -= 64) {
            total += sellValues.priceFor(player, new ItemStack(Material.SUGAR_CANE, Math.min(64, left)));
        }
        return total;
    }

    private void give(Player player, ItemStack items) {
        List<ItemStack> overflow = new ArrayList<>(player.getInventory().addItem(items).values());
        overflow.forEach(i -> player.getWorld().dropItemNaturally(player.getLocation(), i));
    }

    private void deposit(Player player, Currency currency, long amount, String reason) {
        economy.deposit(player.getUniqueId(), currency, amount, reason).exceptionally(t -> {
            log.log(Level.WARNING, "Could not pay " + player.getName() + " " + amount + " " + currency + " for a harvest", t);
            return null;
        });
    }
}
