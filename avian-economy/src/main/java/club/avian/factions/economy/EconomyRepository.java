package club.avian.factions.economy;

import club.avian.factions.api.economy.Currency;
import club.avian.factions.api.economy.TransactionResult;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/** Persistence for balances and the audit trail (ADR-0002, spec §66). */
public interface EconomyRepository {

    /** Every balance in the Season, for the read cache. Boot-time, main thread. */
    Map<UUID, Map<Currency, Long>> loadSeason(int seasonId);

    /**
     * Applies {@code delta} to a balance and writes the audit row in one database transaction.
     *
     * <p>A negative delta is refused rather than applied when the balance is too small — checked
     * inside the same statement, so two concurrent withdrawals cannot both succeed.
     */
    CompletableFuture<TransactionResult> apply(UUID player, Currency currency, long delta,
                                               String action, String reason, UUID referenceId, int seasonId);

    /**
     * Moves {@code amount} from one player to another, both sides and both audit rows in one
     * transaction, so money can never exist in neither place or both.
     */
    CompletableFuture<TransactionResult> transfer(UUID from, UUID to, Currency currency, long amount,
                                                  String reason, int seasonId);
}
