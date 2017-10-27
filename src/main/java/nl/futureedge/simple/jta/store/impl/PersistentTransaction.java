package nl.futureedge.simple.jta.store.impl;

import nl.futureedge.simple.jta.store.JtaTransactionStoreException;

/**
 * Transaction store delegate to stably store information for one transaction.
 */
public interface PersistentTransaction {

    /**
     * Save the status for the (global) transaction.
     * @param status status
     * @throws JtaTransactionStoreException Thrown if the transaction store encounters an unexpected error condition
     */
    void save(TransactionStatus status) throws JtaTransactionStoreException;

    /**
     * Save the status for a branch of the transaction.
     * @param status status
     * @param branchId branch xid
     * @param resourceManager resource manager
     * @throws JtaTransactionStoreException Thrown if the transaction store encounters an unexpected error condition
     */
    void save(TransactionStatus status, long branchId, String resourceManager) throws JtaTransactionStoreException;

    /**
     * Save the status for a branch of the transaction.
     * @param status status
     * @param branchId branch xid
     * @param resourceManager resource manager
     * @param cause exception information
     * @throws JtaTransactionStoreException Thrown if the transaction store encounters an unexpected error condition
     */
    void save(TransactionStatus status, long branchId, String resourceManager, Exception cause) throws JtaTransactionStoreException;

    /**
     * Remove the information for this transaction.
     * @throws JtaTransactionStoreException Thrown if the transaction store encounters an unexpected error condition
     */
    void remove() throws JtaTransactionStoreException;

    /**
     * Retrieves the current (global) status for the transaction
     * @return status
     * @throws JtaTransactionStoreException Thrown if the transaction store encounters an unexpected error condition
     */
    TransactionStatus getStatus() throws JtaTransactionStoreException;
}
