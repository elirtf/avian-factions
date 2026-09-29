package club.avian.factions.economy;

import club.avian.factions.api.economy.Currency;
import club.avian.factions.api.economy.Economy;
import club.avian.factions.api.text.Brand;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Charge, then reward: how a click menu sells something for tokens or gems.
 *
 * <p>A DeluxeMenus button can't check a balance and take it in one step, and anything it gives
 * would be handed out whether or not the payment went through. So the button runs one console
 * command, {@code /tokens charge <player> <cost> <reason> <reward command>}, and this does the rest:
 * the withdrawal is the database's atomic conditional update (a double-click can't spend the same
 * tokens twice), the reward command runs only once it succeeds, and a reward that fails (unknown
 * command, player gone) is refunded, both halves in the audit trail.
 */
final class Purchase {

    enum Outcome { BOUGHT, TOO_POOR, REFUNDED }

    private final Economy economy;
    private final Executor mainThread;
    private final Predicate<String> console;
    private final Logger log;

    /** {@code console} runs a command as the console and says whether it ran; it's called on {@code mainThread}. */
    Purchase(Economy economy, Executor mainThread, Predicate<String> console, Logger log) {
        this.economy = economy;
        this.mainThread = mainThread;
        this.console = console;
        this.log = log;
    }

    CompletableFuture<Outcome> buy(Player player, Currency currency, long cost, String reason, String reward) {
        var uuid = player.getUniqueId();
        String command = reward.startsWith("/") ? reward.substring(1) : reward;
        return economy.withdraw(uuid, currency, cost, reason).thenApplyAsync(result -> {
            if (!result.ok()) {
                player.sendMessage(Brand.mm("<bad>✖</bad> <soft>That costs " + tagged(currency, cost)
                        + "; you have " + tagged(currency, result.balanceAfter()) + ".</soft>"));
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.7f, 1.0f);
                return Outcome.TOO_POOR;
            }
            if (!player.isOnline() || !run(command)) {
                economy.deposit(uuid, currency, cost, "refund:" + reason).exceptionally(t -> {
                    log.log(Level.SEVERE, "Refund of " + cost + " " + currency + " to " + player.getName() + " failed", t);
                    return null;
                });
                player.sendMessage(Brand.mm("<bad>✖</bad> <soft>That purchase failed, so "
                        + tagged(currency, cost) + " went back to you.</soft>"));
                return Outcome.REFUNDED;
            }
            player.sendMessage(Brand.mm("<cane>✔</cane> <soft>Paid " + tagged(currency, cost) + " · "
                    + tagged(currency, result.balanceAfter()) + " left.</soft>"));
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.7f, 1.4f);
            return Outcome.BOUGHT;
        }, mainThread);
    }

    private boolean run(String command) {
        try {
            return console.test(command);
        } catch (RuntimeException e) {
            log.log(Level.WARNING, "Purchase reward failed: /" + command, e);
            return false;
        }
    }

    private String tagged(Currency currency, long amount) {
        String tag = currency == Currency.GEMS ? "gem" : "token";
        return "<" + tag + ">" + economy.format(currency, amount) + "</" + tag + ">";
    }
}
