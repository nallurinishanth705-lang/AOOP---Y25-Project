/** The states a bank account can be in. CLOSED is final: nothing can bring it back. */
public enum AccountStatus {
    ACTIVE,
    FROZEN,
    CLOSED
}
