/** Kinds of entries that can appear in an account's ledger. */
public enum TransactionType {
    OPENING_DEPOSIT,
    DEPOSIT,
    WITHDRAWAL,
    TRANSFER_OUT,
    TRANSFER_IN,
    REVERSAL
}
