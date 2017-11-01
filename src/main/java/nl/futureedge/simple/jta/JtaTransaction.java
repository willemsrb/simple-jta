package nl.futureedge.simple.jta;

import static nl.futureedge.simple.jta.JtaExceptions.illegalStateException;
import static nl.futureedge.simple.jta.JtaExceptions.rollbackException;
import static nl.futureedge.simple.jta.JtaExceptions.systemException;
import static nl.futureedge.simple.jta.JtaExceptions.unsupportedOperationException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import nl.futureedge.simple.jta.store.JtaTransactionStore;
import nl.futureedge.simple.jta.store.JtaTransactionStoreException;
import nl.futureedge.simple.jta.xa.XAResourceAdapter;
import nl.futureedge.simple.jta.xid.BranchJtaXid;
import nl.futureedge.simple.jta.xid.GlobalJtaXid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JTA Transaction.
 */
public final class JtaTransaction implements Transaction {

    private static final Logger LOGGER = LoggerFactory.getLogger(JtaTransaction.class);

    private static final String COULD_NOT_WRITE_TRANSACTION_LOG = "Could not write transaction log";

    private final GlobalJtaXid globalXid;
    private final JtaTransactionStore transactionStore;

    private Integer timeoutInSeconds;

    private int status = Status.STATUS_ACTIVE;

    private final List<EnlistedXaResource> enlistedXaResources = new ArrayList<>();
    private final List<JtaSystemCallback> systemCallbacks = new ArrayList<>();
    private final List<Synchronization> synchronizations = new ArrayList<>();

    private final Map<Object, List<Object>> connections = new HashMap<>();

    /**
     * Constructor.
     * @param xid xid
     * @param transactionStore transaction store
     * @throws SystemException when the transaction log could not be written
     */
    JtaTransaction(final GlobalJtaXid xid, final Integer timeoutInSeconds, final JtaTransactionStore transactionStore) throws SystemException {
        LOGGER.trace("JtaTransaction(xid={}, transactionStore={})", xid, transactionStore);

        this.globalXid = xid;
        this.timeoutInSeconds = timeoutInSeconds;
        this.transactionStore = transactionStore;
        try {
            transactionStore.active(this.globalXid);
        } catch (final JtaTransactionStoreException e) {
            throw systemException(COULD_NOT_WRITE_TRANSACTION_LOG, e);
        }
    }

    public long getTransactionId() {
        return globalXid.getTransactionId();
    }

    /* ***************************** */
    /* *** CONNECTIONS ************* */
    /* ***************************** */

    /**
     * Return connections by key.
     * @param key key
     * @param <T> connection type
     * @return list of connections (can be null)
     */
    public <T> List<T> getConnections(final Object key) {
        return (List<T>) connections.get(key);
    }

    /**
     * Register a connection.
     * @param key key
     * @param connection connection
     */
    public void registerConnection(final Object key, final Object connection) {
        connections.computeIfAbsent(key, unused -> new ArrayList<>());
        connections.get(key).add(connection);
    }

    /* ***************************** */
    /* *** STATUS ****************** */
    /* ***************************** */

    @Override
    public int getStatus() {
        LOGGER.trace("getStatus()");
        return status;
    }

    @Override
    public synchronized void setRollbackOnly() {
        LOGGER.trace("setRollbackOnly()");
        status = Status.STATUS_MARKED_ROLLBACK;
    }

    private void checkActive(final String operation) throws RollbackException {
        if (Status.STATUS_MARKED_ROLLBACK == status) {
            throw rollbackException("Transaction is marked for rollback only; " + operation + " not allowed");
        }
        if (Status.STATUS_ACTIVE != status) {
            throw illegalStateException("Transaction status is not active (but " + status + "); " + operation + " not allowed.");
        }
    }

    private boolean store(final boolean status, final TransactionStoreCommand command) {
        try {
            command.store();
            return status;
        } catch (final JtaTransactionStoreException e) {
            LOGGER.warn(COULD_NOT_WRITE_TRANSACTION_LOG, e);
            return false;
        }
    }

    @FunctionalInterface
    private interface TransactionStoreCommand {
        void store() throws JtaTransactionStoreException;
    }

    /* ***************************** */
    /* *** RESOURCES *************** */
    /* ***************************** */

    @Override
    public synchronized boolean delistResource(final XAResource xaResource, final int flag) throws SystemException {
        LOGGER.trace("delistResource(xaResource={}, flag={})", xaResource, flag);
        throw unsupportedOperationException("delist resource not supported");
    }

    @Override
    public synchronized boolean enlistResource(final XAResource xaResource) throws RollbackException, SystemException {
        LOGGER.trace("enlistResource(xaResource={})", xaResource);
        throw unsupportedOperationException("enlistResource resource not supported");
    }

    /**
     * Enlist resource (see {@link #enlistResource(XAResource)}).
     * @param xaResource xa resource adapter
     * @throws RollbackException thrown if the transaction is marked for rollback only
     * @throws IllegalStateException thrown if no transaction is active
     * @throws SystemException thrown if the transaction manager encounters an internal error
     */
    public synchronized void enlistResource(final XAResourceAdapter xaResource) throws RollbackException, SystemException {
        LOGGER.trace("enlistResource(xaResource={})", xaResource);
        checkActive("enlist resource");

        if (xaResource.supportsJoin()) {
            LOGGER.debug("Join supported; looping through enlisted resources to possibly join already enlisted resource");
            if (join(xaResource)) {
                return;
            }
        }

        // Store
        final BranchJtaXid branchXid = globalXid.createBranchXid();
        enlistedXaResources.add(new EnlistedXaResource(xaResource, branchXid));
        try {
            transactionStore.active(branchXid, xaResource.getResourceManager());
        } catch (final JtaTransactionStoreException e) {
            throw systemException(COULD_NOT_WRITE_TRANSACTION_LOG, e);
        }

        // Start transaction on XA resource
        try {
            if (timeoutInSeconds != null) {
                xaResource.setTransactionTimeout(timeoutInSeconds);
            }
            LOGGER.debug("Calling xa_start on {} using xid {}", xaResource, branchXid);
            xaResource.start(branchXid, XAResource.TMNOFLAGS);
        } catch (final XAException e) {
            throw systemException("Could not start transaction on XA resource", e);
        }
    }

    private boolean join(final XAResourceAdapter xaResource) throws SystemException {
        for (final EnlistedXaResource enlistedResource : enlistedXaResources) {
            try {
                if (enlistedResource.getXaResource().isSameRM(xaResource)) {
                    if (timeoutInSeconds != null) {
                        xaResource.setTransactionTimeout(timeoutInSeconds);
                    }
                    // Join the 'other' xaResource
                    // Preparing and committing will be done through the 'other' xaResource, so we don't need to keep a reference to 'this' xaResource.
                    LOGGER.debug("Calling xa_start (join) on {} using xid {}", xaResource, enlistedResource.getBranchXid());
                    xaResource.start(enlistedResource.getBranchXid(), XAResource.TMJOIN);
                    return true;
                }
            } catch (XAException e) {
                throw systemException("Could not join transaction on XA resource", e);
            }
        }
        return false;
    }

    /* ***************************** */
    /* *** COMMIT/ROLLBACK ********* */
    /* ***************************** */

    @Override
    public synchronized void commit() throws RollbackException, SystemException {
        LOGGER.trace("commit()");
        checkActive("commit");

        // Before completion
        doBeforeCompletion();

        if (status == Status.STATUS_ACTIVE) {
            // Preparing
            LOGGER.debug("Starting prepare of 2-phase commit");
            doPreparing();
        }

        if (status == Status.STATUS_PREPARING) {
            // Prepare
            doPrepare();
        }

        if (status == Status.STATUS_PREPARED) {
            LOGGER.debug("Starting commit of 2-phase commit");
            // Committing
            doCommitting();
        }

        if (status == Status.STATUS_COMMITTING) {
            // Commit
            doCommit();
        } else {
            // Rollback
            LOGGER.debug("Transaction not be prepared. Executing rollback.");
            doRollback();
            throw rollbackException("Transaction rolled back. View log for previous error(s).");
        }
    }

    private void doBeforeCompletion() {
        LOGGER.trace("doBeforeCompletion()");
        try {
            for (final Synchronization synchronization : synchronizations) {
                synchronization.beforeCompletion();
            }
        } catch (final Exception e) {
            LOGGER.info("Exception during synchronization.beforeCompletion", e);
            status = Status.STATUS_MARKED_ROLLBACK;
        }
    }

    private void doPreparing() {
        if (store(true, () -> transactionStore.preparing(globalXid))) {
            status = Status.STATUS_PREPARING;
        } else {
            status = Status.STATUS_MARKED_ROLLBACK;
        }
    }

    private void doPrepare() {
        LOGGER.trace("doPrepare()");

        // Prepare
        boolean ok = true;

        for (final EnlistedXaResource enlistedXaResource : enlistedXaResources) {
            final XAResourceAdapter xaResource = enlistedXaResource.getXaResource();
            final BranchJtaXid branchXid = enlistedXaResource.getBranchXid();
            if (!ok) {
                LOGGER.debug("Skipping prepare on {} as previous prepare has already failed.", xaResource);
                continue;
            }

            ok = store(ok, () -> transactionStore.preparing(branchXid, xaResource.getResourceManager()));
            if (!ok) {
                continue;
            }

            try {
                LOGGER.debug("Calling xa_end on {} using xid {}", xaResource, branchXid);
                xaResource.end(branchXid, XAResource.TMSUCCESS);
                enlistedXaResource.setEnded();

                LOGGER.debug("Calling xa_prepare on {} using xid {}", xaResource, branchXid);
                final int prepareResult = xaResource.prepare(branchXid);
                if (prepareResult == XAResource.XA_OK) {
                    LOGGER.debug("xa_prepare on {}; result ok; adding xaResource to list of prepared resources.", xaResource);
                    ok = store(ok, () -> transactionStore.prepared(branchXid, xaResource.getResourceManager()));
                } else if (prepareResult == XAResource.XA_RDONLY) {
                    LOGGER.debug("xa_prepare on {}; result read-only. Skipping xa resource for commit.", xaResource);
                    enlistedXaResource.setClosed();
                    ok = store(ok, () -> transactionStore.committed(branchXid, xaResource.getResourceManager()));
                } else {
                    ok = false;
                    LOGGER.error("Unknown result {} from xaResource.prepare on {}", prepareResult, xaResource);
                }
            } catch (final XAException e) {
                ok = false;
                if (XAException.XA_RBBASE <= e.errorCode && XAException.XA_RBEND >= e.errorCode) {
                    LOGGER.debug("XA exception during prepare; xa resource is rolled back", e);
                    enlistedXaResource.setClosed();
                }
            }
        }

        if (ok) {
            ok = store(ok, () -> transactionStore.prepared(globalXid));
        }
        LOGGER.debug("Prepare of 2-phase commit completed; result = {}", ok);

        if (ok) {
            status = Status.STATUS_PREPARED;
        } else {
            status = Status.STATUS_MARKED_ROLLBACK;
        }
    }

    private void doCommitting() {
        if (store(true, () -> transactionStore.committing(globalXid))) {
            status = Status.STATUS_COMMITTING;
        } else {
            status = Status.STATUS_MARKED_ROLLBACK;
        }
    }

    private void doCommit() throws SystemException {
        LOGGER.trace("doCommit()");
        boolean storeOk = true;
        boolean commitOk = true;

        for (final EnlistedXaResource enlistedXaResource : enlistedXaResources) {
            final XAResourceAdapter xaResource = enlistedXaResource.getXaResource();
            final BranchJtaXid branchXid = enlistedXaResource.getBranchXid();

            if (enlistedXaResource.isClosed()) {
                LOGGER.debug("Skipping commit on {} as it has already been closed (readonly)", xaResource);
                continue;
            }
            storeOk = store(storeOk, () -> transactionStore.committing(branchXid, xaResource.getResourceManager()));
            try {
                LOGGER.debug("Calling xa_commit on {} using xid {}", xaResource, branchXid);
                xaResource.commit(branchXid, false);
                storeOk = store(storeOk, () -> transactionStore.committed(branchXid, xaResource.getResourceManager()));
            } catch (final XAException e) {
                commitOk = false;
                LOGGER.error("XA exception during commit", e);
                storeOk = store(storeOk, () -> transactionStore.commitFailed(branchXid, xaResource.getResourceManager(), e));
            }
        }

        LOGGER.debug("Commit of 2-phase commit completed; success = {}", commitOk);
        if (commitOk) {
            storeOk = store(storeOk, () -> transactionStore.committed(globalXid));
        } else {
            storeOk = store(storeOk, () -> transactionStore.commitFailed(globalXid));
        }
        status = Status.STATUS_COMMITTED;
        doSystemCallbacks();

        if (!commitOk) {
            throw systemException(
                    "Transaction could not be committed completely (after successful preparation). DATA CAN BE INCONSISTENT! View log for previous error(s).");
        } else if (!storeOk) {
            throw systemException(
                    "Transaction was committed completely (DATA SHOULD BE CONSISTENT); transaction log could not be stored successfully! View log for previous"
                            + " error(s).");
        }
        doAfterCompletion();
    }

    @Override
    public synchronized void rollback() throws SystemException {
        LOGGER.trace("rollback()");
        if (Status.STATUS_ACTIVE != status && Status.STATUS_MARKED_ROLLBACK != status) {
            throw illegalStateException("Transaction status is not active or marked for rollback (but " + status + "); rollback not allowed.");
        }

        doRollback();
    }


    private void doRollback() throws SystemException {
        LOGGER.trace("rollback()");

        // Rollback
        LOGGER.debug("Starting rollback");
        status = Status.STATUS_ROLLING_BACK;
        boolean storeOk = store(true, () -> transactionStore.rollingBack(globalXid));
        boolean rollbackOk = true;

        for (final EnlistedXaResource enlistedXaResource : enlistedXaResources) {
            final XAResourceAdapter xaResource = enlistedXaResource.getXaResource();
            final BranchJtaXid branchXid = enlistedXaResource.getBranchXid();

            if (enlistedXaResource.isClosed()) {
                LOGGER.debug("Skipping rollback on {} as it has already been closed (readonly/rolled back)", xaResource);
                continue;
            }
            storeOk = store(storeOk, () -> transactionStore.rollingBack(branchXid, xaResource.getResourceManager()));
            try {
                if (enlistedXaResource.isEnded()) {
                    LOGGER.debug("Skipping xa_end on {} as it has already been called", xaResource);
                } else {
                    LOGGER.debug("Calling xa_end on {} using xid {}", xaResource, branchXid);
                    try {
                        xaResource.end(branchXid, XAResource.TMFAIL);
                    } catch (final XAException e) {
                        rollbackOk = false;
                        LOGGER.warn("XA exception during end", e);
                        // Exception is lost here, as we also try to rollback
                    }
                }
                LOGGER.debug("Calling xa_rollback on {} using xid {}", xaResource, branchXid);
                xaResource.rollback(branchXid);

                storeOk = store(storeOk, () -> transactionStore.rolledBack(branchXid, xaResource.getResourceManager()));
            } catch (final XAException e) {
                rollbackOk = false;
                LOGGER.warn("XA exception during rollback", e);
                storeOk = store(storeOk, () -> transactionStore.rollbackFailed(branchXid, xaResource.getResourceManager(), e));
            }
        }

        LOGGER.debug("Rollback completed; success = {}", rollbackOk);
        if (rollbackOk) {
            storeOk = store(storeOk, () -> transactionStore.rolledBack(globalXid));
        } else {
            storeOk = store(storeOk, () -> transactionStore.rollbackFailed(globalXid));
        }
        status = Status.STATUS_ROLLEDBACK;
        doSystemCallbacks();

        if (!rollbackOk) {
            throw systemException(
                    "Transaction could not be rolled back completely. DATA CAN BE INCONSISTENT! View log for previous error(s).");
        } else if (!storeOk) {
            throw systemException(
                    "Transaction was rolled back completely (DATA SHOULD BE CONSISTENT); transaction log could not be stored successfully!"
                            + " View log for previous error(s).");
        }

        doAfterCompletion();
    }

    /* ***************************** */
    /* *** CALLBACKS *************** */
    /* ***************************** */

    /**
     * Register a system callback; not for external use!
     * @param systemCallback system callback
     */
    public synchronized void registerSystemCallback(final JtaSystemCallback systemCallback) {
        LOGGER.trace("registerAfterCompletionCallback(systemCallback={}", systemCallback);
        systemCallbacks.add(systemCallback);
    }

    private void doSystemCallbacks() {
        for (final JtaSystemCallback systemCallback : systemCallbacks) {
            systemCallback.transactionCompleted(this);
        }
    }

    /* ***************************** */
    /* *** SYNCHRONIZATIONS ******** */
    /* ***************************** */

    @Override
    public synchronized void registerSynchronization(final Synchronization synchronization) throws RollbackException {
        LOGGER.trace("registerSynchronization(synchronization={}", synchronization);
        checkActive("register synchronization");

        synchronizations.add(synchronization);
    }

    private void doAfterCompletion() {
        LOGGER.trace("doAfterCompletion()");
        for (final Synchronization synchronization : synchronizations) {
            synchronization.afterCompletion(status);
        }
    }

    /* ***************************** */
    /* *** TIMEOUT ***************** */
    /* ***************************** */

    /**
     * Modify the timeout value that is associated with transactions started by the current thread with the begin method.
     *
     * <p> If an application has not called this method, the transaction service uses some default value for the transaction timeout.
     * @param seconds The value of the timeout in seconds. If the value is zero, the transaction service restores the default value. If the value is negative a
     * SystemException is thrown.
     * @throws SystemException Thrown if the transaction manager encounters an unexpected error condition.
     */
    synchronized void setTransactionTimeout(final int seconds) throws SystemException {
        LOGGER.trace("setTransactionTimeout(seconds={})", seconds);

        if (seconds < 0) {
            throw systemException("Timeout may not be a negative value");
        }

        timeoutInSeconds = seconds;

        // Update all enlisted xa resources
        for (final EnlistedXaResource enlistedXaResource : enlistedXaResources) {
            try {
                enlistedXaResource.getXaResource().setTransactionTimeout(timeoutInSeconds);
            } catch (final XAException e) {
                throw systemException("Could not set timeout on XA resource", e);
            }
        }
    }

    @Override
    public String toString() {
        return "JtaTransaction{" +
                "globalXid=" + globalXid +
                ", status=" + status +
                '}';
    }

    private static final class EnlistedXaResource {
        private final XAResourceAdapter xaResource;
        private final BranchJtaXid branchXid;
        private boolean ended;
        private boolean closed;

        EnlistedXaResource(final XAResourceAdapter xaResource, final BranchJtaXid branchXid) {
            this.xaResource = xaResource;
            this.branchXid = branchXid;
            ended = false;
            closed = false;
        }

        XAResourceAdapter getXaResource() {
            return xaResource;
        }

        BranchJtaXid getBranchXid() {
            return branchXid;
        }

        void setEnded() {
            ended = true;
        }

        boolean isEnded() {
            return ended;
        }

        void setClosed() {
            closed = true;
        }

        boolean isClosed() {
            return closed;
        }
    }
}
