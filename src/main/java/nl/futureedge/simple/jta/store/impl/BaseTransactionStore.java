package nl.futureedge.simple.jta.store.impl;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import javax.transaction.xa.XAException;
import nl.futureedge.simple.jta.store.JtaTransactionStore;
import nl.futureedge.simple.jta.store.JtaTransactionStoreException;
import nl.futureedge.simple.jta.xid.BranchJtaXid;
import nl.futureedge.simple.jta.xid.GlobalJtaXid;
import nl.futureedge.simple.jta.xid.JtaXid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base transaction store implementation.
 */
public abstract class BaseTransactionStore implements JtaTransactionStore {

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


    /**
     * Retrieves a delegate to stably store information about the transaction.
     * @param xid xid
     * @return transaction store delegate for the given xid
     * @throws JtaTransactionStoreException Thrown if the transaction store encounters an unexpected error condition
     */
    protected abstract PersistentTransaction getPersistentTransaction(JtaXid xid) throws JtaTransactionStoreException;

    @Override
    public boolean isCommitting(final BranchJtaXid xid) throws JtaTransactionStoreException {
        LOGGER.debug("isCommitting(xid={})", xid);
        return TransactionStatus.COMMITTING.equals(getPersistentTransaction(xid).getStatus());
    }

    @Override
    public void active(final GlobalJtaXid xid) throws JtaTransactionStoreException {
        LOGGER.debug("active(xid={})", xid);
        getPersistentTransaction(xid).save(TransactionStatus.ACTIVE);
    }

    @Override
    public void active(final BranchJtaXid xid, final String resourceManager) throws JtaTransactionStoreException {
        LOGGER.debug("active(xid={}, resourceManager={})", xid, resourceManager);
        getPersistentTransaction(xid).save(TransactionStatus.ACTIVE, xid.getBranchId(), resourceManager);
    }

    @Override
    public void preparing(final GlobalJtaXid xid) throws JtaTransactionStoreException {
        LOGGER.debug("preparing(xid={})", xid);
        getPersistentTransaction(xid).save(TransactionStatus.PREPARING);
    }

    @Override
    public void preparing(final BranchJtaXid xid, final String resourceManager) throws JtaTransactionStoreException {
        LOGGER.debug("preparing(xid={}, resourceManager={})", xid, resourceManager);
        getPersistentTransaction(xid).save(TransactionStatus.PREPARING, xid.getBranchId(), resourceManager);
    }

    @Override
    public void prepared(final GlobalJtaXid xid) throws JtaTransactionStoreException {
        LOGGER.debug("prepared(xid={})", xid);
        getPersistentTransaction(xid).save(TransactionStatus.PREPARED);
    }

    @Override
    public void prepared(final BranchJtaXid xid, final String resourceManager) throws JtaTransactionStoreException {
        LOGGER.debug("prepared(xid={}, resourceManager={})", xid, resourceManager);
        getPersistentTransaction(xid).save(TransactionStatus.PREPARED, xid.getBranchId(), resourceManager);
    }

    @Override
    public void committing(final GlobalJtaXid xid) throws JtaTransactionStoreException {
        LOGGER.debug("committing(xid={})", xid);
        getPersistentTransaction(xid).save(TransactionStatus.COMMITTING);
    }

    @Override
    public void committing(final BranchJtaXid xid, final String resourceManager) throws JtaTransactionStoreException {
        LOGGER.debug("committing(xid={}, resourceManager={})", xid, resourceManager);
        getPersistentTransaction(xid).save(TransactionStatus.COMMITTING, xid.getBranchId(), resourceManager);
    }

    @Override
    public void committed(final GlobalJtaXid xid) throws JtaTransactionStoreException {
        LOGGER.debug("committed(xid={})", xid);
        final PersistentTransaction persistentTransaction = getPersistentTransaction(xid);
        persistentTransaction.save(TransactionStatus.COMMITTED);
        persistentTransaction.remove();
    }

    @Override
    public void committed(final BranchJtaXid xid, final String resourceManager) throws JtaTransactionStoreException {
        LOGGER.debug("committed(xid={}, resourceManager={})", xid, resourceManager);
        getPersistentTransaction(xid).save(TransactionStatus.COMMITTED, xid.getBranchId(), resourceManager);
    }

    @Override
    public void commitFailed(final GlobalJtaXid xid) throws JtaTransactionStoreException {
        LOGGER.debug("commitFailed(xid={})", xid);
        getPersistentTransaction(xid).save(TransactionStatus.COMMIT_FAILED);
    }

    @Override
    public void commitFailed(final BranchJtaXid xid, final String resourceManager, final XAException cause) throws JtaTransactionStoreException {
        LOGGER.debug("commitFailed(xid={}, resourceManager={})", xid, resourceManager, cause);
        getPersistentTransaction(xid).save(TransactionStatus.COMMIT_FAILED, xid.getBranchId(), resourceManager, cause);
    }

    @Override
    public void rollingBack(final GlobalJtaXid xid) throws JtaTransactionStoreException {
        LOGGER.debug("rollingBack(xid={})", xid);
        getPersistentTransaction(xid).save(TransactionStatus.ROLLING_BACK);
    }

    @Override
    public void rollingBack(final BranchJtaXid xid, final String resourceManager) throws JtaTransactionStoreException {
        LOGGER.debug("rollingBack(xid={}, resourceManager={})", xid, resourceManager);
        getPersistentTransaction(xid).save(TransactionStatus.ROLLING_BACK, xid.getBranchId(), resourceManager);
    }

    @Override
    public void rolledBack(final GlobalJtaXid xid) throws JtaTransactionStoreException {
        LOGGER.debug("rolledBack(xid={})", xid);
        final PersistentTransaction persistentTransaction = getPersistentTransaction(xid);
        persistentTransaction.save(TransactionStatus.ROLLED_BACK);
        persistentTransaction.remove();
    }

    @Override
    public void rolledBack(final BranchJtaXid xid, final String resourceManager) throws JtaTransactionStoreException {
        LOGGER.debug("rolledBack(xid={}, resourceManager={})", xid, resourceManager);
        getPersistentTransaction(xid).save(TransactionStatus.ROLLED_BACK, xid.getBranchId(), resourceManager);
    }

    @Override
    public void rollbackFailed(final GlobalJtaXid xid) throws JtaTransactionStoreException {
        LOGGER.debug("rollbackFailed(xid={})", xid);
        getPersistentTransaction(xid).save(TransactionStatus.ROLLBACK_FAILED);
    }

    @Override
    public void rollbackFailed(final BranchJtaXid xid, final String resourceManager, final XAException cause) throws JtaTransactionStoreException {
        LOGGER.debug("rollbackFailed(xid={}, resourceManager={})", xid, resourceManager, cause);
        getPersistentTransaction(xid).save(TransactionStatus.ROLLBACK_FAILED, xid.getBranchId(), resourceManager, cause);
    }
}
