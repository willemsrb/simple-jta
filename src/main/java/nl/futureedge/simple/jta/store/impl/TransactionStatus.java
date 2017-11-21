package nl.futureedge.simple.jta.store.impl;


/**
 * Transaction status.
 */
public enum TransactionStatus {

    /** Active. */
    ACTIVE,

    /** Preparing. */
    PREPARING,

    /** Prepared. */
    PREPARED,

    /** Committing. */
    COMMITTING,

    /** Committed. */
    COMMITTED,

    /** Commit failed. */
    COMMIT_FAILED,

    /** Rolling back. */
    ROLLING_BACK,

    /** Rolled back. */
    ROLLED_BACK,

    /** Rollback failed. */
    ROLLBACK_FAILED;
}
