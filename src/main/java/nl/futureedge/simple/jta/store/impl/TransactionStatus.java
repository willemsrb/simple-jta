package nl.futureedge.simple.jta.store.impl;


/**
 * Transaction status.
 */
public enum TransactionStatus {

    /** Active. */
    ACTIVE("ACTIVE"),

    /** Preparing. */
    PREPARING("PREPARING"),

    /** Prepared. */
    PREPARED("PREPARED"),

    /** Committing. */
    COMMITTING("COMMITTING"),

    /** Committed. */
    COMMITTED("COMMITTED"),

    /** Commit failed. */
    COMMIT_FAILED("COMMIT_FAILED"),

    /** Rolling back. */
    ROLLING_BACK("ROLLING_BACK"),

    /** Rolled back. */
    ROLLED_BACK("ROLLED_BACK"),

    /** Rollback failed. */
    ROLLBACK_FAILED("ROLLBACK_FAILED");

    private final String text;

    /**
     * Constructor.
     * @param text the status as written in the transaction store
     */
    TransactionStatus(final String text) {
        this.text = text;
    }

    /**
     * Get the status as written in the transaction store.
     * @return text
     */
    public String getText() {
        return text;
    }

}
