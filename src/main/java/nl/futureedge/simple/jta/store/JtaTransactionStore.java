package nl.futureedge.simple.jta.store;

import javax.transaction.xa.XAException;
import nl.futureedge.simple.jta.JtaXid;

/**
 * Store to 'stably' register the state of an exception.
 *
 * Most steps to store information about a transaction are optional and can be ignored. The steps {@link #committing(JtaXid)}, {@link #committed(JtaXid)} and
 * {@link #rolledBack(JtaXid)} are required as they identify when the transaction must be recovered by committing work or the transaction data can be removed
 * from the store.
 */
public interface JtaTransactionStore {

    /**
     * Gives the next transaction id to use.
     * @return transaction id
     * @throws JtaTransactionStoreException Thrown if the transaction store encounters an unexpected error condition
     */
    long nextTransactionId() throws JtaTransactionStoreException;

    /**
     * Determine (for recovery) if the given xid was committing
     * @param xid xid
     * @return true, if the xid was committing
     * @throws JtaTransactionStoreException Thrown if the transaction store encounters an unexpected error condition
     */
    boolean isCommitting(JtaXid xid) throws JtaTransactionStoreException;

    /*
     * Execute cleanup; check
     */
    void cleanup() throws JtaTransactionStoreException;

    /**
     * Register a new transaction (optional).
     * @param xid xid
     * @throws JtaTransactionStoreException Thrown if the transaction store encounters an unexpected error condition
     */
    void active(JtaXid xid) throws JtaTransactionStoreException;

    /**
     * Register a new resource in a transaction (optional).
     * @param xid xid
     * @param resourceManager resource manager
     * @throws JtaTransactionStoreException Thrown if the transaction store encounters an unexpected error condition
     */
    void active(JtaXid xid, String resourceManager) throws JtaTransactionStoreException;

    /**
     * Register the start of preparation for a transaction (optional).
     * @param xid xid
     * @throws JtaTransactionStoreException Thrown if the transaction store encounters an unexpected error condition
     */
    void preparing(JtaXid xid) throws JtaTransactionStoreException;

    /**
     * Register the start of preparation for a resource in a transaction (optional).
     * @param xid xid
     * @param resourceManager resource manager
     * @throws JtaTransactionStoreException Thrown if the transaction store encounters an unexpected error condition
     */
    void preparing(JtaXid xid, String resourceManager) throws JtaTransactionStoreException;

    /**
     * Register the end of preparation for a transaction (optional).
     * @param xid xid
     * @throws JtaTransactionStoreException Thrown if the transaction store encounters an unexpected error condition
     */
    void prepared(JtaXid xid) throws JtaTransactionStoreException;

    /**
     * Register the end of preparation for a resource in a transaction (required, as this registers all participants in a transaction).
     * @param xid xid
     * @param resourceManager resource manager
     * @throws JtaTransactionStoreException Thrown if the transaction store encounters an unexpected error condition
     */
    void prepared(JtaXid xid, String resourceManager) throws JtaTransactionStoreException;

    /**
     * Register the start of commit for a transaction (required, as this signals the recovery that the work should be committed and not rolled back).
     * @param xid xid
     * @throws JtaTransactionStoreException Thrown if the transaction store encounters an unexpected error condition
     */
    void committing(JtaXid xid) throws JtaTransactionStoreException;

    /**
     * Register the start of commit for a resource in a transaction (optional).
     * @param xid xid
     * @param resourceManager resource manager
     * @throws JtaTransactionStoreException Thrown if the transaction store encounters an unexpected error condition
     */
    void committing(JtaXid xid, String resourceManager) throws JtaTransactionStoreException;

    /**
     * Register the end of commit for a transaction; all data about the transaction can be forgotten permanently (required).
     * @param xid xid
     * @throws JtaTransactionStoreException Thrown if the transaction store encounters an unexpected error condition
     */
    void committed(JtaXid xid) throws JtaTransactionStoreException;

    /**
     * Register the end of commit for a resource in a transaction (optional).
     * @param xid xid
     * @param resourceManager resource manager
     * @throws JtaTransactionStoreException Thrown if the transaction store encounters an unexpected error condition
     */
    void committed(JtaXid xid, String resourceManager) throws JtaTransactionStoreException;

    /**
     * Register the failure of commit for a transaction; this *should* not happen, it signifies that a participant has broken the rules for XA transactions (for
     * example failing to commit after succesfully preparing).
     * @param xid xid
     * @throws JtaTransactionStoreException Thrown if the transaction store encounters an unexpected error condition
     */
    void commitFailed(JtaXid xid) throws JtaTransactionStoreException;

    /**
     * Register the failure of commit for a resource in a transaction; this *should* not happen, it signifies that the resource has broken the rules for XA
     * transactions (for example failing to commit after succesfully preparing).
     * @param xid xid
     * @param resourceManager resource manager
     * @param cause cause
     * @throws JtaTransactionStoreException Thrown if the transaction store encounters an unexpected error condition
     */
    void commitFailed(JtaXid xid, String resourceManager, XAException cause) throws JtaTransactionStoreException;

    /**
     * Register the start of rolling back for a transaction (optional).
     * @param xid xid
     * @throws JtaTransactionStoreException Thrown if the transaction store encounters an unexpected error condition
     */
    void rollingBack(JtaXid xid) throws JtaTransactionStoreException;

    /**
     * Register the start of rolling back for a resource in a transaction (optional).
     * @param xid xid
     * @param resourceManager resource manager
     * @throws JtaTransactionStoreException Thrown if the transaction store encounters an unexpected error condition
     */
    void rollingBack(JtaXid xid, String resourceManager) throws JtaTransactionStoreException;

    /**
     * Register the end of rolling back for a transaction; all data about the transaction can be forgotten permanently (required).
     * @param xid xid
     * @throws JtaTransactionStoreException Thrown if the transaction store encounters an unexpected error condition
     */
    void rolledBack(JtaXid xid) throws JtaTransactionStoreException;

    /**
     * Register the end of rolling back for a resource in a transaction (optional).
     * @param xid xid
     * @param resourceManager resource manager
     * @throws JtaTransactionStoreException Thrown if the transaction store encounters an unexpected error condition
     */
    void rolledBack(JtaXid xid, String resourceManager) throws JtaTransactionStoreException;

    /**
     * Register the failure of rollback for a transaction; this *should* not happen.
     * @param xid xid
     * @throws JtaTransactionStoreException Thrown if the transaction store encounters an unexpected error condition
     */
    void rollbackFailed(JtaXid xid) throws JtaTransactionStoreException;

    /**
     * Register the failure of rollback for a transaction; this *should* not happen.
     * @param xid xid
     * @param resourceManager resource manager
     * @param cause cause
     * @throws JtaTransactionStoreException Thrown if the transaction store encounters an unexpected error condition
     */
    void rollbackFailed(JtaXid xid, String resourceManager, XAException cause) throws JtaTransactionStoreException;

}
