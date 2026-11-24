import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** Entity: one line in an account's history. Everything is final, so it can never be edited. */
public final class LedgerEntry {
    private final String transactionId;
    private final TransactionType transactionType;
    private final Money amount;          // signed: + for money in, - for money out
    private final Money balanceAfter;    // balance right after this entry
    private final String reference;      // e.g. transfer id, or a short note
    private final String timestamp;

    LedgerEntry(String transactionId, TransactionType type, Money amount, Money balanceAfter, String reference) {
        this.transactionId = transactionId;
        this.transactionType = type;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
        this.reference = reference;
        this.timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    public String getTransactionId()          { return transactionId; }
    public TransactionType getTransactionType() { return transactionType; }
    public Money getAmount()                  { return amount; }
    public Money getBalanceAfter()            { return balanceAfter; }
    public String getReference()              { return reference; }
    public String getTimestamp()              { return timestamp; }

    @Override public String toString() {
        return String.format("%s | %-15s | %14s | balance %14s | %s",
                transactionId, transactionType, amount, balanceAfter, reference);
    }
}
