package club.avian.factions.api.economy;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Balances and the transactions that change them (spec §12, §66, §74).
 *
 * <p>Every mutation takes a {@code reason} and writes an audit row alongside the balance change,
 * in one database transaction: "bank cleared for no reason" is unanswerable without it (#4,
 * FUUID #1363). Amounts are whole units — there are no cents.
 *
 * <p>Reads are synchronous cache hits and safe on the main thread; writes return futures.
 */
public interface Economy {

    /** This player's balance in whole units. Zero for a player with no row yet. */
    long balance(UUID player, Currency currency);

    /** True when the player holds at least {@code amount} whole units. */
    default boolean has(UUID player, Currency currency, long amount) {
        return balance(player, currency) >= amount;
    }

    CompletableFuture<TransactionResult> deposit(UUID player, Currency currency, long amount, String reason);

    /**
     * Takes {@code amount} if the player has it. The check and the deduction are one atomic
     * database operation, so two concurrent withdrawals cannot both succeed against one balance.
     */
    CompletableFuture<TransactionResult> withdraw(UUID player, Currency currency, long amount, String reason);

    /** Moves {@code amount} between players, or fails leaving both untouched. */
    CompletableFuture<TransactionResult> transfer(UUID from, UUID to, Currency currency, long amount, String reason);

    /** Formats an amount for display: {@code 1640} → {@code "$1,640"}. */
    String format(Currency currency, long amount);
}
