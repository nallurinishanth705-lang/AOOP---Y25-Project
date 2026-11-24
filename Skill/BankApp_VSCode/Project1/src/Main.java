import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/** Demo: runs each rule from the case study and prints what happens. */
public class Main {
    interface Action { void run(); }

    static void attempt(String title, Action action) {
        System.out.println("\n>> " + title);
        try {
            action.run();
            System.out.println("   OK: allowed");
        } catch (RuntimeException e) {
            System.out.println("   REJECTED: " + (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName() + " (list is read-only)"));
        }
    }

    static void printState(BankAccount a) {
        System.out.println("   " + a.getAccountNumber() + " | status " + a.getStatus()
                + " | balance " + a.getBalance() + " | ledger consistent: " + a.isLedgerConsistent());
    }

    public static void main(String[] args) throws Exception {
        BankAccount a = new BankAccount("ACC-1001", Money.rupees(10000), Money.rupees(2000));
        BankAccount b = new BankAccount("ACC-2002", Money.rupees(3000),  Money.rupees(0));

        System.out.println("=== Starting state ===");
        printState(a);
        printState(b);

        System.out.println("\n=== Test 1: invalid amounts ===");
        attempt("Deposit Rs. 0",        () -> a.deposit(Money.rupees(0)));
        attempt("Deposit Rs. -500",     () -> a.deposit(Money.rupees(-500)));
        attempt("Withdraw Rs. -100",    () -> a.withdraw(Money.rupees(-100)));

        System.out.println("\n=== Test 2: deposit and withdraw ===");
        attempt("Deposit Rs. 2,500",    () -> a.deposit(Money.rupees(2500)));
        attempt("Withdraw Rs. 1,500",   () -> a.withdraw(Money.rupees(1500)));
        printState(a);

        System.out.println("\n=== Test 3: overdraft limit (A has limit Rs. 2,000) ===");
        attempt("Withdraw Rs. 12,000 (balance goes to -Rs. 1,000, inside the limit)", () -> a.withdraw(Money.rupees(12000)));
        printState(a);
        attempt("Withdraw Rs. 2,000 (would go to -Rs. 3,000, beyond the limit)", () -> a.withdraw(Money.rupees(2000)));
        attempt("Deposit Rs. 1,000 to bring balance back to zero",              () -> a.deposit(Money.rupees(1000)));
        attempt("Deposit Rs. 10,000 to make A healthy again",                    () -> a.deposit(Money.rupees(10000)));

        System.out.println("\n=== Test 4: frozen and closed accounts ===");
        attempt("Freeze A",                   a::freeze);
        attempt("Withdraw Rs. 100 from frozen A", () -> a.withdraw(Money.rupees(100)));
        attempt("Freeze A again",             a::freeze);
        attempt("Unfreeze A",                 a::unfreeze);
        attempt("Withdraw Rs. 100 after unfreeze", () -> a.withdraw(Money.rupees(100)));
        attempt("Close A while balance is not zero", a::close);
        BankAccount c = new BankAccount("ACC-3003", Money.rupees(0), Money.rupees(0));
        attempt("Close empty account C",      c::close);
        attempt("Deposit into closed C",      () -> c.deposit(Money.rupees(100)));
        attempt("Unfreeze closed C",          c::unfreeze);

        System.out.println("\n=== Test 5: normal transfer of Rs. 5,000 from A to B ===");
        System.out.println("   Before:"); printState(a); printState(b);
        FundTransfer t1 = new FundTransfer("TRF-001", a, b, Money.rupees(5000));
        attempt("Transfer TRF-001", t1::transfer);
        System.out.println("   Transfer status: " + t1.getStatus());
        System.out.println("   After:");  printState(a); printState(b);
        attempt("Run TRF-001 a second time", t1::transfer);

        System.out.println("\n=== Test 6: transfer rules ===");
        attempt("Transfer from A to A",       () -> new FundTransfer("TRF-002", a, a, Money.rupees(100)).transfer());
        attempt("Transfer Rs. 0",             () -> new FundTransfer("TRF-003", a, b, Money.rupees(0)).transfer());
        attempt("Transfer more than overdraft allows", () -> new FundTransfer("TRF-004", a, b, Money.rupees(500000)).transfer());
        b.freeze();
        attempt("Withdraw from frozen B via transfer", () -> new FundTransfer("TRF-005", b, a, Money.rupees(100)).transfer());
        b.unfreeze();
        System.out.println("   Nothing should have moved:"); printState(a); printState(b);

        System.out.println("\n=== Test 7: crash in the middle of a transfer (Rs. 5,000 from A to B) ===");
        System.out.println("   Before:"); printState(a); printState(b);
        FundTransfer t2 = new FundTransfer("TRF-006", a, b, Money.rupees(5000),
                () -> { throw new IllegalStateException("simulated system crash"); });
        attempt("Transfer TRF-006 with crash after debit", t2::transfer);
        System.out.println("   Transfer status: " + t2.getStatus());
        System.out.println("   After (no money lost):"); printState(a); printState(b);

        System.out.println("\n=== Test 8: ledger is read-only ===");
        List<LedgerEntry> history = a.getLedger();
        attempt("Try to delete an entry from A's ledger", () -> history.remove(0));
        attempt("Try to add an entry to A's ledger",      () -> history.add(history.get(0)));

        System.out.println("\n=== Test 9: exactly at the overdraft limit (balance Rs. 1,000, limit Rs. 500) ===");
        BankAccount d = new BankAccount("ACC-4004", Money.rupees(1000), Money.rupees(500));
        attempt("Withdraw Rs. 1,500 (lands exactly on -Rs. 500)", () -> d.withdraw(Money.rupees(1500)));
        attempt("Withdraw 1 more paisa (one step past the limit)", () -> d.withdraw(Money.inr(1)));
        printState(d);

        System.out.println("\n=== Test 10: many things at the same time ===");
        BankAccount g = new BankAccount("ACC-5005", Money.rupees(10000), Money.rupees(0));
        AtomicInteger worked = new AtomicInteger();
        Thread[] two = new Thread[2];
        for (int i = 0; i < 2; i++) {
            two[i] = new Thread(() -> { try { g.withdraw(Money.rupees(8000)); worked.incrementAndGet(); } catch (BankingException ex) { } });
            two[i].start();
        }
        for (Thread t : two) t.join();
        System.out.println("   Two people withdraw Rs. 8,000 at the same moment from Rs. 10,000. Successful withdrawals: " + worked.get());
        printState(g);
        BankAccount f = new BankAccount("ACC-6006", Money.rupees(0), Money.rupees(0));
        Thread[] many = new Thread[100];
        for (int i = 0; i < 100; i++) { many[i] = new Thread(() -> f.deposit(Money.rupees(10))); many[i].start(); }
        for (Thread t : many) t.join();
        System.out.println("   100 people deposit Rs. 10 at the same moment (expected Rs. 1,000.00):");
        printState(f);

        System.out.println("\n=== Ledger of ACC-1001 ===");
        for (LedgerEntry e : a.getLedger()) System.out.println("   " + e);
        System.out.println("\n=== Ledger of ACC-2002 ===");
        for (LedgerEntry e : b.getLedger()) System.out.println("   " + e);

        System.out.println("\n=== Final check ===");
        printState(a); printState(b);
        Money total = a.getBalance().add(b.getBalance());
        System.out.println("   Total money in A + B: " + total);
    }
}
