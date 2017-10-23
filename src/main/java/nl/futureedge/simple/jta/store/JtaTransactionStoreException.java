package nl.futureedge.simple.jta.store;


/**
 * Exception thrown if the tranaction store encounters an error.
 */
public final class JtaTransactionStoreException extends Exception {
    private static final long serialVersionUID = 1L;

    /**
     * Constructor.
     * @param message message
     */
    public JtaTransactionStoreException(final String message) {
        super(message);
    }

    /**
     * Constructor.
     * @param message message
     * @param cause underlying cause
     */
    public JtaTransactionStoreException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
