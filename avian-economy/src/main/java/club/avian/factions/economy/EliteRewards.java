package club.avian.factions.economy;

import club.avian.factions.api.config.ConfigHandle;
import club.avian.factions.api.economy.Currency;
import club.avian.factions.api.text.Brand;
import com.magmaguy.elitemobs.api.EliteMobDeathEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.logging.Logger;

/**
 * Pays for EliteMobs kills in our economy ({@code economy.conf} {@code elite-rewards}; owner, 2026-10-02):
 * elites pay money by level, custom bosses pay tokens and run commands (crate keys) by level tier. Only
 * registered when EliteMobs is installed; its own coins, gear and shops are switched off in its config.
 */
final class EliteRewards implements Listener {

    /** What one kill pays. */
    record Reward(long money, long tokens, List<String> commands) {
        boolean isEmpty() {
            return money <= 0 && tokens <= 0 && commands.isEmpty();
        }
    }

    private final ConfigHandle<EconomyConfig> config;
    private final EconomyService economy;
    private final Executor mainThread;
    private final Logger log;

    EliteRewards(ConfigHandle<EconomyConfig> config, EconomyService economy, Executor mainThread, Logger log) {
        this.config = config;
        this.economy = economy;
        this.mainThread = mainThread;
        this.log = log;
    }

    /** An elite pays money by level; a custom boss pays its highest reached tier (none below the first). */
    static Reward rewardFor(EconomyConfig.EliteRewards rules, int level, boolean boss) {
        if (!boss) {
            return new Reward(Math.max(0, level) * rules.moneyPerLevel(), 0, List.of());
        }
        EconomyConfig.BossTier best = null;
        for (var tier : rules.bossTiers()) {
            if (level >= tier.minLevel() && (best == null || tier.minLevel() > best.minLevel())) {
                best = tier;
            }
        }
        return best == null ? new Reward(0, 0, List.of()) : new Reward(0, best.tokens(), best.commands());
    }

    /** The player who dealt the most damage, if any player did. */
    static <P> Optional<P> topDamager(Map<P, Double> damage) {
        return damage.entrySet().stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(EliteMobDeathEvent event) {
        var elite = event.getEliteEntity();
        if (elite == null) {
            return;
        }
        var player = topDamager(elite.getDamagers()).filter(Player::isOnline);
        if (player.isEmpty()) {
            return;
        }
        boolean boss = elite.isCustomBossEntity();
        var reward = rewardFor(config.get().eliteRewards(), elite.getLevel(), boss);
        if (reward.isEmpty()) {
            return;
        }
        pay(player.get(), reward, boss ? "Boss" : "Level " + elite.getLevel() + " Elite");
    }

    private void pay(Player player, Reward reward, String what) {
        var id = player.getUniqueId();
        if (reward.money() > 0) {
            economy.deposit(id, Currency.MONEY, reward.money(), "elitemobs:kill").thenAcceptAsync(r ->
                    player.sendActionBar(Brand.mm("<money>+" + economy.format(Currency.MONEY, reward.money())
                            + "</money> <soft>for the " + what + "</soft>")), mainThread);
        }
        if (reward.tokens() > 0) {
            economy.deposit(id, Currency.TOKENS, reward.tokens(), "elitemobs:boss").thenAcceptAsync(r ->
                    player.sendMessage(Brand.mm("<sun>✦</sun> <soft>You slew the " + what + ": <token>+"
                            + economy.format(Currency.TOKENS, reward.tokens()) + "</token></soft>")), mainThread);
        }
        for (var command : reward.commands()) {
            var line = command.replace("%player%", player.getName());
            if (!Bukkit.dispatchCommand(Bukkit.getConsoleSender(), line)) {
                log.warning("elite-rewards: command failed: " + line);
            }
        }
    }
}
