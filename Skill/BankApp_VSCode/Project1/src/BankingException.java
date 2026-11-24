/** One exception type for every broken banking rule, with a clear message. */
public class BankingException extends RuntimeException {
    public BankingException(String message) { super(message); }
}
