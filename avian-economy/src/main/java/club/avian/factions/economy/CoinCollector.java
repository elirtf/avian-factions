package club.avian.factions.economy;

import club.avian.factions.api.config.ConfigHandle;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.logging.Logger;

/**
 * Coin Collector (owner, 2026-10-16): our enchantment ({@code avian:coin_collector}, datapack avian-enchants) that
 * makes a kill's mob coins pay more, {@code economy.conf}'s {@code coin-collector-percent-per-level} a level.
 * UltimateMobCoins' {@code MobCoinDropEvent} carries the amount; we hook it by name, so the build doesn't need
 * their jar, and do nothing if the plugin or the enchantment is missing.
 */
final class CoinCollector {

    static final NamespacedKey ENCHANTMENT = new NamespacedKey("avian", "coin_collector");
    private static final String EVENT = "nl.chimpgamer.ultimatemobcoins.paper.events.MobCoinDropEvent";

    private CoinCollector() {
    }

    /** {@code amount} raised by {@code percent}% for each of {@code level} levels, kept to cents. */
    static BigDecimal boosted(BigDecimal amount, int level, int percent) {
        if (level <= 0 || percent <= 0) {
            return amount;
        }
        return amount.multiply(BigDecimal.valueOf(100L + (long) level * percent))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    static void register(Plugin plugin, ConfigHandle<EconomyConfig> config, Logger log) {
        var mobCoins = Bukkit.getPluginManager().getPlugin("UltimateMobCoins");
        var enchantment = RegistryAccess.registryAccess().getRegistry(RegistryKey.ENCHANTMENT).get(ENCHANTMENT);
        if (mobCoins == null || enchantment == null) {
            log.info("Coin Collector is off: " + (mobCoins == null ? "UltimateMobCoins" : "the avian:coin_collector enchantment")
                    + " isn't loaded");
            return;
        }
        try {
            Class<? extends Event> type = Class.forName(EVENT, true, mobCoins.getClass().getClassLoader()).asSubclass(Event.class);
            Method player = type.getMethod("getPlayer");
            Method getAmount = type.getMethod("getAmount");
            Method setAmount = type.getMethod("setAmount", BigDecimal.class);
            Bukkit.getPluginManager().registerEvent(type, new Listener() { }, EventPriority.HIGH, (listener, event) -> {
                if (!type.isInstance(event)) {
                    return;
                }
                try {
                    ItemStack hand = ((Player) player.invoke(event)).getInventory().getItemInMainHand();
                    int level = hand.getEnchantmentLevel(enchantment);
                    if (level > 0) {
                        var amount = (BigDecimal) getAmount.invoke(event);
                        setAmount.invoke(event, boosted(amount, level, config.get().coinCollectorPercentPerLevel()));
                    }
                } catch (ReflectiveOperationException e) {
                    log.warning("Coin Collector: " + e);
                }
            }, plugin, true);
            log.info("Coin Collector: mob coin drops pay " + config.get().coinCollectorPercentPerLevel() + "% more a level");
        } catch (ReflectiveOperationException e) {
            log.warning("Coin Collector is off: UltimateMobCoins' drop event changed (" + e + ")");
        }
    }
}
