package nl.futureedge.simple.jta.store.impl;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.transaction.xa.XAException;
import nl.futureedge.simple.jta.JtaTransaction;
import nl.futureedge.simple.jta.store.JtaTransactionStore;
import nl.futureedge.simple.jta.store.JtaTransactionStoreException;
import nl.futureedge.simple.jta.xid.BranchJtaXid;
import nl.futureedge.simple.jta.xid.GlobalJtaXid;
import nl.futureedge.simple.jta.xid.JtaXid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;

/**
 * Base transaction store implementation.
 */
public abstract class BaseTransactionStore implements DisposableBean, JtaTransactionStore {

    private static final Logger LOGGER = LoggerFactory.getLogger(BaseTransactionStore.class);

    /**
     * Cleanable global and resource statuses.
     */
    protected static final Map<TransactionStatus, List<TransactionStatus>> CLEANABLE = new EnumMap<>(TransactionStatus.class);

    static {
        // ACTIVE; should only contain ACTIVE (and ROLLED_BACK from recovery)
        CLEANABLE.put(TransactionStatus.ACTIVE, Arrays.asList(TransactionStatus.ACTIVE, TransactionStatus.ROLLED_BACK));

        // PREPARING/PREPARED; can be cleaned when PREPARED no longer exists (COMMITTING should not exist)
        CLEANABLE.put(TransactionStatus.PREPARING,
                Arrays.asList(TransactionStatus.ACTIVE, TransactionStatus.PREPARING, TransactionStatus.COMMITTED, TransactionStatus.ROLLED_BACK));
        CLEANABLE.put(TransactionStatus.PREPARED,
                Arrays.asList(TransactionStatus.ACTIVE, TransactionStatus.PREPARING, TransactionStatus.COMMITTED, TransactionStatus.ROLLED_BACK));

        // COMMITTING/COMMITTED; can only be cleaned when everything is COMMITTED!
        CLEANABLE.put(TransactionStatus.COMMITTING, Arrays.asList(TransactionStatus.COMMITTED));
        CLEANABLE.put(TransactionStatus.COMMITTED, Arrays.asList(TransactionStatus.COMMITTED));

        // Do not clean TransactionStatus.COMMIT_FAILED

        // ROLLING_BACK/ROLLED_BACK; can be cleaned when PREPARED no longer exists
        CLEANABLE.put(TransactionStatus.ROLLING_BACK,
                Arrays.asList(TransactionStatus.ACTIVE, TransactionStatus.PREPARING, TransactionStatus.COMMITTED, TransactionStatus.ROLLED_BACK));
        CLEANABLE.put(TransactionStatus.ROLLED_BACK,
                Arrays.asList(TransactionStatus.ACTIVE, TransactionStatus.PREPARING, TransactionStatus.COMMITTED, TransactionStatus.ROLLED_BACK));

        // Do not clean TransactionStatus.ROLLBACK_FAILED
    }

    private final Map<Long, PersistentTransaction> transactions = new HashMap<>();

    private boolean storeAll;

    public void setStoreAll(final boolean storeAll) {
        this.storeAll = storeAll;
    }

    /**
     * Retrieves a delegate to stably store information about the transaction.
     * @param transactionId transaction id
     * @return transaction store delegate for the given xid
     * @throws JtaTransactionStoreException Thrown if the transaction store encounters an unexpected error condition
     */
    protected abstract PersistentTransaction createPersistentTransaction(final long transactionId) throws JtaTransactionStoreException;

    private PersistentTransaction getPersistentTransaction(final JtaXid xid) throws JtaTransactionStoreException {
        final long transactionId = xid.getTransactionId();
        if (transactions.containsKey(transactionId)) {
            return transactions.get(transactionId);
        } else {
            synchronized (transactions) {
                if (!transactions.containsKey(transactionId)) {
                    final PersistentTransaction transaction = createPersistentTransaction(transactionId);
                    transactions.put(transactionId, transaction);

                }
                return transactions.get(transactionId);
            }
        }
    }

    @Override
    public void transactionCompleted(final JtaTransaction transaction) {
        final PersistentTransaction persistentTransaction;
        synchronized (transactions) {
            persistentTransaction = transactions.remove(transaction.getTransactionId());
        }
        if (persistentTransaction != null) {
            persistentTransaction.close();
        }
    }

    @Override
    public final void destroy() throws Exception {
        doDestroy();
        synchronized (transactions) {
            for (final PersistentTransaction transaction : transactions.values()) {
                transaction.close();
            }
            transactions.clear();
        }
    }

    protected abstract void doDestroy();


    @Override
    public final boolean isCommitting(final BranchJtaXid xid) throws JtaTransactionStoreException {
        LOGGER.debug("isCommitting(xid={})", xid);
        return TransactionStatus.COMMITTING.equals(getPersistentTransaction(xid).getStatus());
    }

    @Override
    public final void active(final GlobalJtaXid xid) throws JtaTransactionStoreException {
        LOGGER.debug("active(xid={})", xid);
        if (storeAll) {
            getPersistentTransaction(xid).save(TransactionStatus.ACTIVE);
        }
    }

    @Override
    public final void active(final BranchJtaXid xid, final String resourceManager) throws JtaTransactionStoreException {
        LOGGER.debug("active(xid={}, resourceManager={})", xid, resourceManager);
        if (storeAll) {
            getPersistentTransaction(xid).save(TransactionStatus.ACTIVE, xid.getBranchId(), resourceManager);
        }
    }

    @Override
    public final void preparing(final GlobalJtaXid xid) throws JtaTransactionStoreException {
        LOGGER.debug("preparing(xid={})", xid);
        getPersistentTransaction(xid).save(TransactionStatus.PREPARING);
    }

    @Override
    public final void preparing(final BranchJtaXid xid, final String resourceManager) throws JtaTransactionStoreException {
        LOGGER.debug("preparing(xid={}, resourceManager={})", xid, resourceManager);
        if (storeAll) {
            getPersistentTransaction(xid).save(TransactionStatus.PREPARING, xid.getBranchId(), resourceManager);
        }
    }

    @Override
    public final void prepared(final GlobalJtaXid xid) throws JtaTransactionStoreException {
        LOGGER.debug("prepared(xid={})", xid);
        if (storeAll) {
            getPersistentTransaction(xid).save(TransactionStatus.PREPARED);
        }
    }

    @Override
    public final void prepared(final BranchJtaXid xid, final String resourceManager) throws JtaTransactionStoreException {
        LOGGER.debug("prepared(xid={}, resourceManager={})", xid, resourceManager);
        getPersistentTransaction(xid).save(TransactionStatus.PREPARED, xid.getBranchId(), resourceManager);
    }

    @Override
    public final void committing(final GlobalJtaXid xid) throws JtaTransactionStoreException {
        LOGGER.debug("committing(xid={})", xid);
        // Store if transaction exists:
        // - As it is created by preparing this status is written by a two-phase commit
        // - As a single phase single-phase commit does not prepare the status is not written
        if (transactions.containsKey(xid.getTransactionId()) || storeAll) {
            getPersistentTransaction(xid).save(TransactionStatus.COMMITTING);
        }
    }

    @Override
    public final void committing(final BranchJtaXid xid, final String resourceManager) throws JtaTransactionStoreException {
        LOGGER.debug("committing(xid={}, resourceManager={})", xid, resourceManager);
        if (storeAll) {
            getPersistentTransaction(xid).save(TransactionStatus.COMMITTING, xid.getBranchId(), resourceManager);
        }
    }

    @Override
    public final void committed(final GlobalJtaXid xid) throws JtaTransactionStoreException {
        LOGGER.debug("committed(xid={})", xid);
        final PersistentTransaction persistentTransaction = getPersistentTransaction(xid);
        if (storeAll) {
            persistentTransaction.save(TransactionStatus.COMMITTED);
        }
        persistentTransaction.remove();
    }

    @Override
    public final void committed(final BranchJtaXid xid, final String resourceManager) throws JtaTransactionStoreException {
        LOGGER.debug("committed(xid={}, resourceManager={})", xid, resourceManager);
        // Store if transaction exists (see description at {@link #committing(GlobalJtaXid)}
        if (transactions.containsKey(xid.getTransactionId()) || storeAll) {
            getPersistentTransaction(xid).save(TransactionStatus.COMMITTED, xid.getBranchId(), resourceManager);
        }
    }

    @Override
    public final void commitFailed(final GlobalJtaXid xid) throws JtaTransactionStoreException {
        LOGGER.debug("commitFailed(xid={})", xid);
        getPersistentTransaction(xid).save(TransactionStatus.COMMIT_FAILED);
    }

    @Override
    public final void commitFailed(final BranchJtaXid xid, final String resourceManager, final XAException cause) throws JtaTransactionStoreException {
        LOGGER.debug("commitFailed(xid={}, resourceManager={})", xid, resourceManager, cause);
        getPersistentTransaction(xid).save(TransactionStatus.COMMIT_FAILED, xid.getBranchId(), resourceManager, cause);
    }

    @Override
    public final void rollingBack(final GlobalJtaXid xid) throws JtaTransactionStoreException {
        LOGGER.debug("rollingBack(xid={})", xid);
        // Store if transaction exists (see description at {@link #committing(GlobalJtaXid)}
        if (transactions.containsKey(xid.getTransactionId()) || storeAll) {
            getPersistentTransaction(xid).save(TransactionStatus.ROLLING_BACK);
        }
    }

    @Override
    public final void rollingBack(final BranchJtaXid xid, final String resourceManager) throws JtaTransactionStoreException {
        LOGGER.debug("rollingBack(xid={}, resourceManager={})", xid, resourceManager);
        if (storeAll) {
            getPersistentTransaction(xid).save(TransactionStatus.ROLLING_BACK, xid.getBranchId(), resourceManager);
        }
    }

    @Override
    public final void rolledBack(final GlobalJtaXid xid) throws JtaTransactionStoreException {
        LOGGER.debug("rolledBack(xid={})", xid);
        final PersistentTransaction persistentTransaction = getPersistentTransaction(xid);
        if (storeAll) {
            persistentTransaction.save(TransactionStatus.ROLLED_BACK);
        }
        persistentTransaction.remove();
    }

    @Override
    public final void rolledBack(final BranchJtaXid xid, final String resourceManager) throws JtaTransactionStoreException {
        LOGGER.debug("rolledBack(xid={}, resourceManager={})", xid, resourceManager);
        // Store if transaction exists (see description at {@link #committing(GlobalJtaXid)}
        if (transactions.containsKey(xid.getTransactionId()) || storeAll) {
            getPersistentTransaction(xid).save(TransactionStatus.ROLLED_BACK, xid.getBranchId(), resourceManager);
        }
    }

    @Override
    public final void rollbackFailed(final GlobalJtaXid xid) throws JtaTransactionStoreException {
        LOGGER.debug("rollbackFailed(xid={})", xid);
        getPersistentTransaction(xid).save(TransactionStatus.ROLLBACK_FAILED);
    }

    @Override
    public final void rollbackFailed(final BranchJtaXid xid, final String resourceManager, final XAException cause) throws JtaTransactionStoreException {
        LOGGER.debug("rollbackFailed(xid={}, resourceManager={})", xid, resourceManager, cause);
        getPersistentTransaction(xid).save(TransactionStatus.ROLLBACK_FAILED, xid.getBranchId(), resourceManager, cause);
    }
}
