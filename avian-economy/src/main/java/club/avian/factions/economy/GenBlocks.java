package club.avian.factions.economy;

import club.avian.factions.api.config.ConfigHandle;
import club.avian.factions.api.economy.Currency;
import club.avian.factions.api.economy.Economy;
import club.avian.factions.api.text.Brand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Gen blocks (owner, 2026-10-16): a block item that, placed, lays its block in a line, one at a time, each at its
 * /shop price. Horizontal ones run the way the player faces, vertical ones straight down. The item is never used
 * up: the real placement is cancelled and the run lays the first block itself. Every block laid goes through a
 * BlockPlaceEvent as the player, so faction land, WorldGuard regions and CoreProtect treat it as theirs. The rules
 * are {@link GenRun}.
 */
final class GenBlocks implements Listener {

    static final NamespacedKey KEY = new NamespacedKey("avian", "gen_block");

    private final Plugin plugin;
    private final ConfigHandle<GenBlocksConfig> config;
    private final Economy economy;
    private final Logger log;
    private final Map<UUID, Integer> running = new HashMap<>();

    GenBlocks(Plugin plugin, ConfigHandle<GenBlocksConfig> config, Economy economy, Logger log) {
        this.plugin = plugin;
        this.config = config;
        this.economy = economy;
        this.log = log;
    }

    /** {@code amount} gen blocks of type {@code id}, or null if there is no such type. */
    ItemStack create(String id, int amount) {
        var type = config.get().type(id);
        if (type == null) {
            return null;
        }
        var item = new ItemStack(type.material(), amount);
        item.editMeta(meta -> {
            meta.displayName(Brand.mm("<bold>" + type.name() + "</bold>").decoration(TextDecoration.ITALIC, false));
            List<Component> lore = new ArrayList<>();
            lore.add(line("<soft>Lays " + blockName(type.material()) + " in a line, "
                    + (type.vertical() ? "straight down to the ground." : "the way you face.") + "</soft>"));
            lore.add(line("<soft>Fills air only, and runs on past " + blockName(type.material()) + " already there.</soft>"));
            lore.add(Component.empty());
            lore.add(line("<dim>◆</dim> <soft>Each block</soft>  <money>" + economy.format(Currency.MONEY, type.cost()) + "</money>"));
            lore.add(line("<dim>◆</dim> <soft>Used up</soft>  <cane>never</cane>"));
            lore.add(Component.empty());
            lore.add(line("<cane>▶ Place</cane> <dim>·</dim> <soft>start laying</soft>"));
            meta.lore(lore);
            meta.setEnchantmentGlintOverride(true);
            meta.getPersistentDataContainer().set(KEY, PersistentDataType.STRING, id.toLowerCase(java.util.Locale.ROOT));
        });
        return item;
    }

    static String typeOf(ItemStack item) {
        return item == null || !item.hasItemMeta() ? null
                : item.getItemMeta().getPersistentDataContainer().get(KEY, PersistentDataType.STRING);
    }

    private static Component line(String mm) {
        return Brand.mm(mm).decoration(TextDecoration.ITALIC, false);
    }

    private static String blockName(Material material) {
        return material.name().toLowerCase(java.util.Locale.ROOT).replace('_', ' ');
    }

    // Our own BlockPlaceEvents carry a plain block as the item, so they never reach this.
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        var id = typeOf(event.getItemInHand());
        if (id == null) {
            return;
        }
        event.setCancelled(true);   // the item stays; the run lays the first block itself
        var player = event.getPlayer();
        var cfg = config.get();
        var type = cfg.type(id);
        if (!cfg.enabled() || type == null) {
            player.sendMessage(Brand.mm("<bad>Gen blocks are switched off right now.</bad>"));
            return;
        }
        if (running.getOrDefault(player.getUniqueId(), 0) >= cfg.maxRunning()) {
            player.sendMessage(Brand.mm("<bad>You already have " + cfg.maxRunning() + " gens running. Let one finish.</bad>"));
            return;
        }
        BlockFace dir = type.vertical() ? BlockFace.DOWN : player.getFacing();
        Block start = event.getBlockPlaced();
        int length = type.vertical() ? start.getY() - start.getWorld().getMinHeight() + 1 : cfg.maxLength();
        start(player, id, type, start, dir, length, cfg);
    }

    private void start(Player player, String id, GenBlocksConfig.Type type, Block start, BlockFace dir, int length,
                       GenBlocksConfig cfg) {
        var uuid = player.getUniqueId();
        running.merge(uuid, 1, Integer::sum);
        var run = new GenRun(type.cost(), length);
        var world = start.getWorld();
        var view = new GenRun.World() {
            @Override
            public GenRun.Spot spot(int step) {
                var b = start.getRelative(dir, step);
                if (b.getY() < world.getMinHeight() || b.getY() >= world.getMaxHeight()
                        || !world.getWorldBorder().isInside(b.getLocation())
                        || !world.isChunkLoaded(b.getX() >> 4, b.getZ() >> 4)) {
                    return GenRun.Spot.OUTSIDE;
                }
                var t = b.getType();
                return t == type.material() ? GenRun.Spot.SAME : t.isAir() ? GenRun.Spot.AIR : GenRun.Spot.OTHER;
            }

            @Override
            public boolean place(int step) {
                var p = Bukkit.getPlayer(uuid);
                if (p == null) {
                    return false;
                }
                var b = start.getRelative(dir, step);
                var replaced = b.getState();
                // As vanilla does: lay it, ask everyone, take it back if anyone says no.
                b.setType(type.material(), false);
                var ev = new BlockPlaceEvent(b, replaced, b.getRelative(dir.getOppositeFace()),
                        new ItemStack(type.material()), p, true, EquipmentSlot.HAND);
                Bukkit.getPluginManager().callEvent(ev);
                if (ev.isCancelled() || !ev.canBuild()) {
                    replaced.update(true, false);
                    return false;
                }
                return true;
            }

            @Override
            public long balance() {
                return economy.balance(uuid, Currency.MONEY);
            }
        };
        player.sendActionBar(Brand.mm("<cane>⛏ " + type.name() + " started</cane>"));
        player.playSound(player.getLocation(), Sound.BLOCK_STONE_PLACE, 0.8f, 0.8f);
        new BukkitRunnable() {
            @Override
            public void run() {
                var p = Bukkit.getPlayer(uuid);
                if (p == null) {
                    run.stop(GenRun.End.GONE);
                }
                var end = run.end() != null ? run.end() : run.advance(view, cfg.blocksPerTick());
                if (run.owed() >= type.cost() * cfg.chargeEvery() || end != null) {
                    charge(uuid, id, run.takeOwed());
                }
                if (end != null) {
                    cancel();
                    running.computeIfPresent(uuid, (k, n) -> n <= 1 ? null : n - 1);
                    if (p != null) {
                        p.sendMessage(Brand.mm("<cane>⛏</cane> " + type.name() + " <soft>laid</soft> <white>" + run.placed()
                                + "</white> <soft>blocks for</soft> <money>" + economy.format(Currency.MONEY, run.placed() * type.cost())
                                + "</money><soft>, and stopped: " + end.why + ".</soft>"));
                    }
                }
            }
        }.runTaskTimer(plugin, 1, 1);
    }

    private void charge(UUID player, String id, long amount) {
        if (amount <= 0) {
            return;
        }
        economy.withdraw(player, Currency.MONEY, amount, "genblock:" + id).thenAccept(r -> {
            if (!r.ok()) {
                log.warning("genblock: could not charge " + player + " $" + amount + " (" + r.status() + ")");
            }
        });
    }
}
