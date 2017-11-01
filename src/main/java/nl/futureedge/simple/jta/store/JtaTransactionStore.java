package nl.futureedge.simple.jta.store;

import javax.transaction.xa.XAException;
import nl.futureedge.simple.jta.JtaSystemCallback;
import nl.futureedge.simple.jta.xid.BranchJtaXid;
import nl.futureedge.simple.jta.xid.GlobalJtaXid;

/**
 * Store to 'stably' register the state of an exception.
 */
public interface JtaTransactionStore extends JtaSystemCallback {

    /**
     * Gives the next transaction id to use.
     * @return transaction id
     * @throws JtaTransactionStoreException Thrown if the transaction store encounters an unexpected error condition
     */
    long nextTransactionId() throws JtaTransactionStoreException;

    /**
     * Determine (for recovery) if the given xid was committing.
     * @param xid xid
     * @return true, if the xid was committing
     * @throws JtaTransactionStoreException Thrown if the transaction store encounters an unexpected error condition
     */
    boolean isCommitting(BranchJtaXid xid) throws JtaTransactionStoreException;

    /*
     * Execute cleanup.
     */
    void cleanup() throws JtaTransactionStoreException;

    /**
     * Register a new transaction (optional).
     * @param xid xid
     * @throws JtaTransactionStoreException Thrown if the transaction store encounters an unexpected error condition
     */
    void active(GlobalJtaXid xid) throws JtaTransactionStoreException;

    /**
     * Register a new resource in a transaction (optional).
     * @param xid xid
     * @param resourceManager resource manager
     * @throws JtaTransactionStoreException Thrown if the transaction store encounters an unexpected error condition
     */
    void active(BranchJtaXid xid, String resourceManager) throws JtaTransactionStoreException;

    /**
     * Register the start of preparation for a transaction (optional).
     * @param xid xid
     * @throws JtaTransactionStoreException Thrown if the transaction store encounters an unexpected error condition
     */
    void preparing(GlobalJtaXid xid) throws JtaTransactionStoreException;

    /**
     * Register the start of preparation for a resource in a transaction (optional).
     * @param xid xid
     * @param resourceManager resource manager
     * @throws JtaTransactionStoreException Thrown if the transaction store encounters an unexpected error condition
     */
    void preparing(BranchJtaXid xid, String resourceManager) throws JtaTransactionStoreException;

    /**
     * Register the end of preparation for a transaction (optional).
     * @param xid xid
     * @throws JtaTransactionStoreException Thrown if the transaction store encounters an unexpected error condition
     */
    void prepared(GlobalJtaXid xid) throws JtaTransactionStoreException;

    /**
     * Register the end of preparation for a resource in a transaction (required, as this registers all participants in a transaction).
     * @param xid xid
     * @param resourceManager resource manager
     * @throws JtaTransactionStoreException Thrown if the transaction store encounters an unexpected error condition
     */
    void prepared(BranchJtaXid xid, String resourceManager) throws JtaTransactionStoreException;

    /**
     * Register the start of commit for a transaction (required, as this signals the recovery that the work should be committed and not rolled back).
     * @param xid xid
     * @throws JtaTransactionStoreException Thrown if the transaction store encounters an unexpected error condition
     */
    void committing(GlobalJtaXid xid) throws JtaTransactionStoreException;

    /**
     * Register the start of commit for a resource in a transaction (optional).
     * @param xid xid
     * @param resourceManager resource manager
     * @throws JtaTransactionStoreException Thrown if the transaction store encounters an unexpected error condition
     */
    void committing(BranchJtaXid xid, String resourceManager) throws JtaTransactionStoreException;

    /**
     * Register the end of commit for a transaction; all data about the transaction can be forgotten permanently (required).
     * @param xid xid
     * @throws JtaTransactionStoreException Thrown if the transaction store encounters an unexpected error condition
     */
    void committed(GlobalJtaXid xid) throws JtaTransactionStoreException;

    /**
     * Register the end of commit for a resource in a transaction (optional).
     * @param xid xid
     * @param resourceManager resource manager
     * @throws JtaTransactionStoreException Thrown if the transaction store encounters an unexpected error condition
     */
    void committed(BranchJtaXid xid, String resourceManager) throws JtaTransactionStoreException;

    /**
     * Register the failure of commit for a transaction; this *should* not happen, it signifies that a participant has broken the rules for XA transactions (for
     * example failing to commit after succesfully preparing).
     * @param xid xid
     * @throws JtaTransactionStoreException Thrown if the transaction store encounters an unexpected error condition
     */
    void commitFailed(GlobalJtaXid xid) throws JtaTransactionStoreException;

    /**
     * Register the failure of commit for a resource in a transaction; this *should* not happen, it signifies that the resource has broken the rules for XA
     * transactions (for example failing to commit after succesfully preparing).
     * @param xid xid
     * @param resourceManager resource manager
     * @param cause cause
     * @throws JtaTransactionStoreException Thrown if the transaction store encounters an unexpected error condition
     */
    void commitFailed(BranchJtaXid xid, String resourceManager, XAException cause) throws JtaTransactionStoreException;

    /**
     * Register the start of rolling back for a transaction (optional).
     * @param xid xid
     * @throws JtaTransactionStoreException Thrown if the transaction store encounters an unexpected error condition
     */
    void rollingBack(GlobalJtaXid xid) throws JtaTransactionStoreException;

    /**
     * Register the start of rolling back for a resource in a transaction (optional).
     * @param xid xid
     * @param resourceManager resource manager
     * @throws JtaTransactionStoreException Thrown if the transaction store encounters an unexpected error condition
     */
    void rollingBack(BranchJtaXid xid, String resourceManager) throws JtaTransactionStoreException;

    /**
     * Register the end of rolling back for a transaction; all data about the transaction can be forgotten permanently (required).
     * @param xid xid
     * @throws JtaTransactionStoreException Thrown if the transaction store encounters an unexpected error condition
     */
    void rolledBack(GlobalJtaXid xid) throws JtaTransactionStoreException;

    /**
     * Register the end of rolling back for a resource in a transaction (optional).
     * @param xid xid
     * @param resourceManager resource manager
     * @throws JtaTransactionStoreException Thrown if the transaction store encounters an unexpected error condition
     */
    void rolledBack(BranchJtaXid xid, String resourceManager) throws JtaTransactionStoreException;

    /**
     * Register the failure of rollback for a transaction; this *should* not happen.
     * @param xid xid
     * @throws JtaTransactionStoreException Thrown if the transaction store encounters an unexpected error condition
     */
    void rollbackFailed(GlobalJtaXid xid) throws JtaTransactionStoreException;

    /**
     * Register the failure of rollback for a transaction; this *should* not happen.
     * @param xid xid
     * @param resourceManager resource manager
     * @param cause cause
     * @throws JtaTransactionStoreException Thrown if the transaction store encounters an unexpected error condition
     */
    void rollbackFailed(BranchJtaXid xid, String resourceManager, XAException cause) throws JtaTransactionStoreException;
}
