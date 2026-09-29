package club.avian.factions.combat;

import club.avian.factions.api.config.ConfigHandle;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import com.destroystokyo.paper.event.player.PlayerLaunchProjectileEvent;
import io.papermc.paper.event.player.PlayerItemCooldownEvent;
import org.bukkit.Material;
import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityResurrectEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.LongSupplier;

/**
 * Cooldowns on the strongest PvP consumables (#40): golden apples, enchanted golden apples and the
 * totem of undying; and on ender pearls, the HCF way (16 s instead of vanilla's 1 s).
 *
 * <p>The vanilla item cooldown only draws the sweep on the item and is forgotten on relog, so the
 * real timer lives here, keyed by player: it survives relogging and dying (not a restart), is
 * re-drawn on join and respawn, and is what the server checks.
 */
final class ConsumableCooldowns implements Listener {

    private final ConfigHandle<CombatConfig> config;
    private final LongSupplier clock;
    private final Map<UUID, Map<Material, Long>> readyAt = new HashMap<>();

    ConsumableCooldowns(ConfigHandle<CombatConfig> config, LongSupplier clockMillis) {
        this.config = config;
        this.clock = clockMillis;
    }

    int cooldownSeconds(Material material) {
        var cfg = config.get();
        return switch (material) {
            case ENCHANTED_GOLDEN_APPLE -> cfg.enchantedGoldenAppleCooldownSeconds();
            case GOLDEN_APPLE -> cfg.goldenAppleCooldownSeconds();
            case TOTEM_OF_UNDYING -> cfg.totemCooldownSeconds();
            case ENDER_PEARL -> cfg.enderPearlCooldownSeconds();
            default -> 0;
        };
    }

    /** Milliseconds until this player may use this item again; 0 when ready. */
    long remainingMillis(UUID player, Material material) {
        var until = readyAt.getOrDefault(player, Map.of()).get(material);
        return until == null ? 0 : Math.max(0, until - clock.getAsLong());
    }

    void start(Player player, Material material) {
        int seconds = cooldownSeconds(material);
        if (seconds <= 0) {
            return;
        }
        readyAt.computeIfAbsent(player.getUniqueId(), p -> new EnumMap<>(Material.class))
                .put(material, clock.getAsLong() + seconds * 1000L);
        player.setCooldown(new ItemStack(material), seconds * 20);
    }

    // --- golden apples ----------------------------------------------------------------------------

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEat(PlayerItemConsumeEvent event) {
        var material = event.getItem().getType();
        long remaining = remainingMillis(event.getPlayer().getUniqueId(), material);
        if (cooldownSeconds(material) > 0 && remaining > 0) {
            event.setCancelled(true);
            tell(event.getPlayer(), material, remaining);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onAte(PlayerItemConsumeEvent event) {
        start(event.getPlayer(), event.getItem().getType());
    }

    // --- totem: EntityResurrectEvent arrives cancelled when there is no totem to use ------------

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onTotem(EntityResurrectEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        long remaining = remainingMillis(player.getUniqueId(), Material.TOTEM_OF_UNDYING);
        if (cooldownSeconds(Material.TOTEM_OF_UNDYING) > 0 && remaining > 0) {
            event.setCancelled(true);   // the totem stays in hand and does not save them
            tell(player, Material.TOTEM_OF_UNDYING, remaining);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTotemUsed(EntityResurrectEvent event) {
        if (event.getEntity() instanceof Player player) {
            start(player, Material.TOTEM_OF_UNDYING);
        }
    }

    // --- ender pearls: vanilla puts a 1 s cooldown on the pearl after each throw -----------------

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPearl(PlayerLaunchProjectileEvent event) {
        if (!(event.getProjectile() instanceof EnderPearl)) {
            return;
        }
        long remaining = remainingMillis(event.getPlayer().getUniqueId(), Material.ENDER_PEARL);
        if (cooldownSeconds(Material.ENDER_PEARL) > 0 && remaining > 0) {
            event.setCancelled(true);   // the pearl stays in hand
            tell(event.getPlayer(), Material.ENDER_PEARL, remaining);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPearlThrown(PlayerLaunchProjectileEvent event) {
        if (event.getProjectile() instanceof EnderPearl) {
            start(event.getPlayer(), Material.ENDER_PEARL);
        }
    }

    /** Vanilla sets its own 1 s pearl cooldown right after the throw; stretch it to ours. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onVanillaCooldown(PlayerItemCooldownEvent event) {
        if (event.getType() != Material.ENDER_PEARL) {
            return;
        }
        long remaining = remainingMillis(event.getPlayer().getUniqueId(), Material.ENDER_PEARL);
        if (remaining > 0) {
            event.setCooldown(Math.max(event.getCooldown(), (int) Math.ceil(remaining / 50.0)));
        }
    }

    // --- keep the item sweep in step after a relog or death ---------------------------------------

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        redraw(event.getPlayer());
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        redraw(event.getPlayer());
    }

    private void redraw(Player player) {
        var mine = readyAt.get(player.getUniqueId());
        if (mine == null) {
            return;
        }
        mine.forEach((material, until) -> {
            long remaining = until - clock.getAsLong();
            if (remaining > 0) {
                player.setCooldown(new ItemStack(material), (int) Math.ceil(remaining / 50.0));
            }
        });
        mine.values().removeIf(until -> until <= clock.getAsLong());
        if (mine.isEmpty()) {
            readyAt.remove(player.getUniqueId());
        }
    }

    private static void tell(Player player, Material material, long remainingMillis) {
        String what = switch (material) {
            case ENCHANTED_GOLDEN_APPLE -> "Enchanted golden apple";
            case GOLDEN_APPLE -> "Golden apple";
            case ENDER_PEARL -> "Ender pearl";
            default -> "Totem";
        };
        long seconds = (remainingMillis + 999) / 1000;
        player.sendActionBar(Component.text(what + " on cooldown: " + seconds + "s", NamedTextColor.RED));
    }
}
