import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Entity: a bank account. It protects its own rules, so no other class can break them.
 * - Fields are private, nobody outside can change the balance directly.
 * - Every change goes through a method that checks the rules first.
 * - Every change also writes a ledger entry, so balance and history always agree.
 */
public class BankAccount {
    private static final AtomicInteger txnCounter = new AtomicInteger();   // used to make simple transaction ids

    private final String accountNumber;
    private Money balance;
    private final Money overdraftLimit;  // how far below zero the balance may go (never negative)
    private AccountStatus status;
    private final List<LedgerEntry> ledger = new ArrayList<>();

    public BankAccount(String accountNumber, Money openingBalance, Money overdraftLimit) {
        if (accountNumber == null || accountNumber.isBlank()) {
            throw new BankingException("Account number is required");
        }
        if (openingBalance.isNegative()) {
            throw new BankingException("Opening balance cannot be negative");
        }
        if (overdraftLimit.isNegative()) {
            throw new BankingException("Overdraft limit cannot be negative");
        }
        this.accountNumber = accountNumber;
        this.overdraftLimit = overdraftLimit;
        this.balance = Money.zero();
        this.status = AccountStatus.ACTIVE;
        if (openingBalance.isPositive()) {
            record(TransactionType.OPENING_DEPOSIT, openingBalance, "Account opened");
        }
    }

    // ---------- public operations ----------

    public synchronized void deposit(Money amount) {
        checkCanDeposit(amount);
        record(TransactionType.DEPOSIT, amount, "Cash deposit");
    }

    public synchronized void withdraw(Money amount) {
        checkCanWithdraw(amount);
        record(TransactionType.WITHDRAWAL, amount.negate(), "Cash withdrawal");
    }

    public synchronized void freeze() {
        if (status != AccountStatus.ACTIVE) {
            throw new BankingException("Only an ACTIVE account can be frozen (status: " + status + ")");
        }
        status = AccountStatus.FROZEN;
    }

    public synchronized void unfreeze() {
        if (status != AccountStatus.FROZEN) {
            throw new BankingException("Only a FROZEN account can be unfrozen (status: " + status + ")");
        }
        status = AccountStatus.ACTIVE;
    }

    public synchronized void close() {
        if (status == AccountStatus.CLOSED) {
            throw new BankingException("Account is already closed");
        }
        if (!balance.isZero()) {
            throw new BankingException("Balance must be zero before closing (balance: " + balance + ")");
        }
        status = AccountStatus.CLOSED;
    }

    // ---------- read-only views ----------

    public synchronized Money getBalance()          { return balance; }
    public String getAccountNumber()                { return accountNumber; }
    public Money getOverdraftLimit()                { return overdraftLimit; }
    public synchronized AccountStatus getStatus()   { return status; }

    /** Returns a read-only copy of the history. Callers cannot add, edit or remove entries. */
    public synchronized List<LedgerEntry> getLedger() {
        return Collections.unmodifiableList(new ArrayList<>(ledger));
    }

    /** Re-adds every ledger entry and checks the total equals the stored balance. */
    public synchronized boolean isLedgerConsistent() {
        Money total = Money.zero();
        for (LedgerEntry e : ledger) {
            total = total.add(e.getAmount());
        }
        return total.equals(balance);
    }

    // ---------- package-private helpers used by FundTransfer ----------

    synchronized void checkCanDeposit(Money amount) {
        checkAmount(amount);
        if (status == AccountStatus.CLOSED) {
            throw new BankingException("Account " + accountNumber + " is closed");
        }
    }

    synchronized void checkCanWithdraw(Money amount) {
        checkAmount(amount);
        if (status != AccountStatus.ACTIVE) {
            throw new BankingException("Account " + accountNumber + " is " + status + ", withdrawals are not allowed");
        }
        Money after = balance.subtract(amount);
        if (after.isLessThan(overdraftLimit.negate())) {
            throw new BankingException("Overdraft limit exceeded on " + accountNumber
                    + " (balance " + balance + ", limit " + overdraftLimit + ", asked " + amount + ")");
        }
    }

    synchronized void applyTransfer(TransactionType type, Money signedAmount, String reference) {
        record(type, signedAmount, reference);
    }

    // ---------- private helpers ----------

    private void checkAmount(Money amount) {
        if (amount == null || !amount.isPositive()) {
            throw new BankingException("Amount must be greater than zero");
        }
    }

    /** The only place where balance changes. It updates balance and ledger together. */
    private void record(TransactionType type, Money signedAmount, String reference) {
        balance = balance.add(signedAmount);
        ledger.add(new LedgerEntry(String.format("TXN-%04d", txnCounter.incrementAndGet()), type, signedAmount, balance, reference));
    }
}
