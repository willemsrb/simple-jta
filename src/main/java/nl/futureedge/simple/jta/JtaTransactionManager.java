package nl.futureedge.simple.jta;

import static nl.futureedge.simple.jta.JtaExceptions.illegalStateException;
import static nl.futureedge.simple.jta.JtaExceptions.notSupportedException;
import static nl.futureedge.simple.jta.JtaExceptions.systemException;
import static nl.futureedge.simple.jta.JtaExceptions.unsupportedOperationException;

import java.util.List;
import javax.transaction.InvalidTransactionException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import nl.futureedge.simple.jta.store.JtaTransactionStore;
import nl.futureedge.simple.jta.store.JtaTransactionStoreException;
import nl.futureedge.simple.jta.xa.XAResourceAdapter;
import nl.futureedge.simple.jta.xid.BranchJtaXid;
import nl.futureedge.simple.jta.xid.GlobalJtaXid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Required;

/**
 * JTA Transaction manager.
 */
public final class JtaTransactionManager implements InitializingBean, DisposableBean, TransactionManager, JtaSystemCallback {

    private static final Logger LOGGER = LoggerFactory.getLogger(JtaTransactionManager.class);
    public static final String
            COULD_NOT_WRITE_TRANSACTION_LOG_INCONSISTENT =
            "Could not write transaction log; recovery could not be logged! TRANSACTION SYSTEM IS NOW INCONSISTENT!";

    private final ThreadLocal<Integer> timeoutInSeconds = new ThreadLocal<>();
    private final ThreadLocal<JtaTransaction> transaction = new ThreadLocal<>();

    private String uniqueName;
    private JtaTransactionStore transactionStore;

    /**
     * Set unique name to use for this transaction manager.
     *
     * This must be unique with the set of transaction managers that act on a XA resource to guarantee generated XIDs do not clash. This must be consistent
     * between session for recovery (commit or rollback after crash) to work.
     *
     * Note: only the first (currently implementation 52 bytes) positions can be used to identify the transaction manager due to length restrictions in the
     * XID.
     * @param uniqueName unique name
     */
    @Required
    public void setUniqueName(final String uniqueName) {
        this.uniqueName = uniqueName;
    }

    /**
     * @return unique name for this transaction manager
     */
    public String getUniqueName() {
        return uniqueName;
    }

    /**
     * Set the transaction store to use.
     * @param transactionStore transaction store
     */
    @Required
    @Autowired
    public void setJtaTransactionStore(final JtaTransactionStore transactionStore) {
        this.transactionStore = transactionStore;
    }

    /**
     * Startup; executes an early cleanup on the transaction store.
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        transactionStore.cleanup();
    }

    /**
     * Shutdown.
     */
    @Override
    public void destroy() throws Exception {
        // Nothing now; we could wait for all transactions to end, warning for each open transaction (bad developer) and maybe kill them (the transactions).
    }

    /* ************************************** */
    /* *** SYNCHRONIZATION ****************** */
    /* ************************************** */

    @Override
    public void transactionCompleted() {
        transaction.set(null);
    }

    /* ************************************** */
    /* *** TRANSACTION MANAGER ************** */
    /* ************************************** */

    @Override
    public void begin() throws NotSupportedException, SystemException {
        LOGGER.trace("begin()");
        if (transaction.get() != null) {
            throw notSupportedException("Transaction already started");
        }

        final JtaTransaction result;
        try {
            result = new JtaTransaction(new GlobalJtaXid(uniqueName, transactionStore.nextTransactionId()), timeoutInSeconds.get(), transactionStore);
            result.registerSystemCallback(this);
        } catch (final JtaTransactionStoreException | IllegalStateException e) {
            throw systemException("Could not create new transaction", e);
        }
        transaction.set(result);
    }

    @Override
    public JtaTransaction getTransaction() {
        LOGGER.trace("getTransaction()");
        return transaction.get();
    }

    /**
     * Return the current transaction.
     * @return transaction
     * @throws IllegalStateException thrown when no transaction is active
     */
    public JtaTransaction getRequiredTransaction() {
        final JtaTransaction result = transaction.get();
        if (result == null) {
            throw illegalStateException("No transaction active");
        }
        return result;
    }

    @Override
    public int getStatus() {
        LOGGER.trace("getStatus()");
        final JtaTransaction result = transaction.get();
        if (result == null) {
            return Status.STATUS_NO_TRANSACTION;
        } else {
            return result.getStatus();
        }
    }

    @Override
    public void commit() throws RollbackException, SystemException {
        LOGGER.trace("commit()");
        getRequiredTransaction().commit();
    }

    @Override
    public void rollback() throws SystemException {
        LOGGER.trace("rollback()");
        getRequiredTransaction().rollback();
    }

    @Override
    public void setRollbackOnly() throws SystemException {
        LOGGER.trace("setRollbackOnly()");
        getRequiredTransaction().setRollbackOnly();
    }

    @Override
    public void setTransactionTimeout(final int seconds) throws SystemException {
        LOGGER.trace("setTransactionTimeout(seconds={})", seconds);
        timeoutInSeconds.set(seconds);

        final JtaTransaction currentTransaction = transaction.get();
        if (currentTransaction != null) {
            currentTransaction.setTransactionTimeout(seconds);
        }
    }

    @Override
    public void resume(final Transaction transaction) throws InvalidTransactionException, SystemException {
        LOGGER.trace("resume(transaction={})", transaction);
        throw unsupportedOperationException("Transaction suspension is not supported");
    }

    @Override
    public Transaction suspend() throws SystemException {
        LOGGER.trace("suspend()");
        throw unsupportedOperationException("Transaction suspension is not supported");
    }

    /* ***************************** */
    /* *** RECOVERY **************** */
    /* ***************************** */

    /**
     * Execute recovery for the given resource.
     * @param xaResource resource
     * @throws SystemException thrown if the transaction manager encounters an unexpected error condition.
     */
    public void recover(final XAResourceAdapter xaResource) throws SystemException {
        LOGGER.info("Starting recovery for {}", xaResource.getResourceManager());
        // Process XIDs for this transaction manager only (so unique name needs to be consistent)
        final List<BranchJtaXid> xids;
        try {
            xids = BranchJtaXid.filterRecoveryXids(xaResource.recover(XAResource.TMENDRSCAN), uniqueName);
        } catch (XAException e) {
            LOGGER.error("Could not retrieve XIDs for recovery from resource {}", xaResource.getResourceManager(), e);
            return;
        }
        for (final BranchJtaXid xid : xids) {
            // Check if partial transaction should be committed
            final boolean committing;
            try {
                committing = transactionStore.isCommitting(xid);
            } catch (JtaTransactionStoreException e) {
                LOGGER.error("Could not determine status for XID for recovery", e);
                continue;
            }
            if (committing) {
                // Commit
                recoveryCommit(xaResource, xid);

            } else {
                // Rollback
                recoveryRollback(xaResource, xid);
            }
        }

        LOGGER.info("Completed recovery for {}", xaResource.getResourceManager());
        try {
            transactionStore.cleanup();
        } catch (JtaTransactionStoreException e) {
            LOGGER.error("Could not execute cleanup on transaction store", e);

        }
    }

    private void recoveryCommit(final XAResourceAdapter xaResource, final BranchJtaXid xid) throws SystemException {
        LOGGER.info("Committing partial transaction {}", xid);
        storeCommitting(xaResource, xid);

        try {
            xaResource.commit(xid, true);
            storeCommitted(xaResource, xid);
        } catch (final XAException e) {
            LOGGER.error("XA exception during recovery commit", e);
            storeCommitFailed(xaResource, xid, e);
        }
    }

    private void storeCommitting(final XAResourceAdapter xaResource, final BranchJtaXid xid) throws SystemException {
        try {
            transactionStore.committing(xid, xaResource.getResourceManager());
        } catch (JtaTransactionStoreException e) {
            throw JtaExceptions.systemException("Could not write transaction log; recovery could not be started!", e);
        }
    }

    private void storeCommitted(final XAResourceAdapter xaResource, final BranchJtaXid xid) throws SystemException {
        try {
            transactionStore.committed(xid, xaResource.getResourceManager());
        } catch (final JtaTransactionStoreException e) {
            throw JtaExceptions.systemException(COULD_NOT_WRITE_TRANSACTION_LOG_INCONSISTENT, e);
        }
    }

    private void storeCommitFailed(final XAResourceAdapter xaResource, final BranchJtaXid xid, final XAException e) throws SystemException {
        try {
            transactionStore.commitFailed(xid, xaResource.getResourceManager(), e);
        } catch (final JtaTransactionStoreException e2) {
            throw JtaExceptions.systemException(COULD_NOT_WRITE_TRANSACTION_LOG_INCONSISTENT, e2);
        }
    }

    private void recoveryRollback(final XAResourceAdapter xaResource, final BranchJtaXid xid) throws SystemException {
        LOGGER.info("Rolling back partial transaction {}", xid);
        storeRollingBack(xaResource, xid);
        try {
            xaResource.rollback(xid);
            storeRolledBack(xaResource, xid);
        } catch (final XAException e) {
            LOGGER.error("XA exception during recovery rollback", e);
            storeRollbackFailed(xaResource, xid, e);
        }
    }

    private void storeRollingBack(final XAResourceAdapter xaResource, final BranchJtaXid xid) throws SystemException {
        try {
            transactionStore.rollingBack(xid, xaResource.getResourceManager());
        } catch (final JtaTransactionStoreException e) {
            throw JtaExceptions.systemException("Could not write transaction log; recovery could not be started!", e);
        }
    }

    private void storeRolledBack(final XAResourceAdapter xaResource, final BranchJtaXid xid) throws SystemException {
        try {
            transactionStore.rolledBack(xid, xaResource.getResourceManager());
        } catch (final JtaTransactionStoreException e) {
            throw JtaExceptions.systemException(COULD_NOT_WRITE_TRANSACTION_LOG_INCONSISTENT, e);
        }
    }

    private void storeRollbackFailed(final XAResourceAdapter xaResource, final BranchJtaXid xid, final XAException e) throws SystemException {
        try {
            transactionStore.rollbackFailed(xid, xaResource.getResourceManager(), e);
        } catch (final JtaTransactionStoreException e2) {
            throw JtaExceptions.systemException(COULD_NOT_WRITE_TRANSACTION_LOG_INCONSISTENT, e2);
        }
    }

    @Override
    public String toString() {
        return "JtaTransactionManager{" +
                "uniqueName='" + uniqueName + '\'' +
                '}';
    }
}
