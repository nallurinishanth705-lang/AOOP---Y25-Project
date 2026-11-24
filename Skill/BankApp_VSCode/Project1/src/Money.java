import java.util.Objects;

/** Value object: an amount of money in paise (1 rupee = 100 paise). Immutable. */
public final class Money {
    private final long amountInPaise;
    private final String currency;

    public Money(long amountInPaise, String currency) {
        if (currency == null || currency.isBlank()) {
            throw new IllegalArgumentException("Currency is required");
        }
        this.amountInPaise = amountInPaise;
        this.currency = currency;
    }

    public static Money inr(long amountInPaise) { return new Money(amountInPaise, "INR"); }
    public static Money rupees(long rupees)     { return new Money(rupees * 100, "INR"); }
    public static Money zero()                  { return new Money(0, "INR"); }

    public Money add(Money other) {
        checkSameCurrency(other);
        return new Money(Math.addExact(amountInPaise, other.amountInPaise), currency);
    }

    public Money subtract(Money other) {
        checkSameCurrency(other);
        return new Money(Math.subtractExact(amountInPaise, other.amountInPaise), currency);
    }

    public boolean isPositive()  { return amountInPaise > 0; }
    public boolean isNegative()  { return amountInPaise < 0; }
    public boolean isZero()      { return amountInPaise == 0; }

    public boolean isLessThan(Money other) {
        checkSameCurrency(other);
        return amountInPaise < other.amountInPaise;
    }

    public Money negate() { return new Money(-amountInPaise, currency); }

    public long getAmountInPaise() { return amountInPaise; }
    public String getCurrency()    { return currency; }

    private void checkSameCurrency(Money other) {
        if (!currency.equals(other.currency)) {
            throw new IllegalArgumentException("Currency mismatch: " + currency + " vs " + other.currency);
        }
    }

    // Two Money objects are equal when amount and currency are the same (no identity).
    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Money)) return false;
        Money m = (Money) o;
        return amountInPaise == m.amountInPaise && currency.equals(m.currency);
    }

    @Override public int hashCode() { return Objects.hash(amountInPaise, currency); }

    @Override public String toString() {
        long abs = Math.abs(amountInPaise);
        return String.format("%sRs. %,d.%02d", amountInPaise < 0 ? "-" : "", abs / 100, abs % 100);
    }
}
