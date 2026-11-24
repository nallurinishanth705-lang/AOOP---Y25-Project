/**
 * Entity: one transfer of money between two accounts. It has its own id and a status.
 * Steps: check everything first, take money out, put money in. If putting money in
 * fails, the money is returned to the source (compensation), so no money is lost.
 */
public class FundTransfer {
    public enum Status { PENDING, COMPLETED, FAILED, ROLLED_BACK }

    private final String transferId;
    private final BankAccount source;
    private final BankAccount destination;
    private final Money amount;
    private Status status = Status.PENDING;
    private final Runnable crashPoint;   // only for testing: lets us pretend the system crashes mid-transfer

    public FundTransfer(String transferId, BankAccount source, BankAccount destination, Money amount) {
        this(transferId, source, destination, amount, null);
    }

    FundTransfer(String transferId, BankAccount source, BankAccount destination, Money amount, Runnable crashPoint) {
        this.transferId = transferId;
        this.source = source;
        this.destination = destination;
        this.amount = amount;
        this.crashPoint = crashPoint;
    }

    public void transfer() {
        if (status != Status.PENDING) {
            throw new BankingException("Transfer " + transferId + " was already processed (" + status + ")");
        }
        if (source == destination || source.getAccountNumber().equals(destination.getAccountNumber())) {
            status = Status.FAILED;
            throw new BankingException("Source and destination must be different accounts");
        }

        // Always lock the accounts in the same order, so two opposite transfers cannot block each other forever.
        BankAccount first  = source.getAccountNumber().compareTo(destination.getAccountNumber()) < 0 ? source : destination;
        BankAccount second = first == source ? destination : source;

        synchronized (first) {
            synchronized (second) {
                // Step 1: check both sides BEFORE touching any money
                try {
                    source.checkCanWithdraw(amount);
                    destination.checkCanDeposit(amount);
                } catch (BankingException e) {
                    status = Status.FAILED;
                    throw e;
                }

                // Step 2: take money out of the source
                source.applyTransfer(TransactionType.TRANSFER_OUT, amount.negate(), transferId);

                try {
                    if (crashPoint != null) crashPoint.run();   // simulated failure happens here
                    // Step 3: put money into the destination
                    destination.applyTransfer(TransactionType.TRANSFER_IN, amount, transferId);
                    status = Status.COMPLETED;
                } catch (RuntimeException e) {
                    // Step 4: something broke after money left the source, so give it back
                    source.applyTransfer(TransactionType.REVERSAL, amount, transferId + " (rollback)");
                    status = Status.ROLLED_BACK;
                    throw new BankingException("Transfer " + transferId + " failed and was rolled back: " + e.getMessage());
                }
            }
        }
    }

    public String getTransferId() { return transferId; }
    public Status getStatus()     { return status; }
}
