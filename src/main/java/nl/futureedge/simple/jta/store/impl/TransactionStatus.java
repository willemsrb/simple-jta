package nl.futureedge.simple.jta.store.impl;

/**
 * Transaction status.
 */
public enum TransactionStatus {

    ACTIVE("ACTIVE"),
    PREPARING("PREPARING"),
    PREPARED("PREPARED"),
    COMMITTING("COMMITING"),
    COMMITTED("COMMITTED"),
    COMMIT_FAILED("COMMIT_FAILED"),
    ROLLINGBACK("ROLLINGBACK"),
    ROLLED_BACK("ROLLEDBACK"),
    ROLLBACK_FAILED("ROLLBACK_FAILED");

    private final String text;

    /**
     * Constructor.
     * @param text text
     */
    TransactionStatus(final String text) {
        this.text = text;
    }

    /**
     * Get the text.
     * @return text
     */
    public String getText() {
        return text;
    }

    /**
     * Get the transaction status from the text.
     * @param text text
     * @return transaction status
     */
    public static TransactionStatus fromText(final String text) {
        for (final TransactionStatus status : values()) {
            if (status.getText().equals(text)) {
                return status;
            }
        }
        return null;
    }
}
