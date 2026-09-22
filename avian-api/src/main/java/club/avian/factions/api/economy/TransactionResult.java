package club.avian.factions.api.economy;

import java.util.UUID;

/**
 * The outcome of one balance change.
 *
 * @param status       what happened
 * @param transaction  the audit row's id, present only when {@code status} is {@link Status#OK}
 * @param balanceAfter the balance once the change applied, or the unchanged balance on failure
 */
public record TransactionResult(Status status, UUID transaction, long balanceAfter) {

    public enum Status {
        OK,
        /** The player does not have that much. The balance is untouched. */
        INSUFFICIENT_FUNDS,
        /** The amount was negative, zero where that makes no sense, or not a real number. */
        INVALID_AMOUNT,
        /** The change would take the balance past what the currency can hold. */
        WOULD_OVERFLOW,
        /** The database refused or failed; nothing was written. */
        FAILED
    }

    public boolean ok() {
        return status == Status.OK;
    }

    public static TransactionResult failure(Status status, long balance) {
        return new TransactionResult(status, null, balance);
    }
}
