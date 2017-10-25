package nl.futureedge.simple.jta.store.impl;

import javax.transaction.xa.XAException;
import nl.futureedge.simple.jta.JtaXid;
import nl.futureedge.simple.jta.store.JtaTransactionStore;
import nl.futureedge.simple.jta.store.JtaTransactionStoreException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class BaseTransactionStore implements JtaTransactionStore {

    private static final Logger LOGGER = LoggerFactory.getLogger(BaseTransactionStore.class);

    protected abstract PersistentTransaction getPersistentTransaction(JtaXid xid) throws JtaTransactionStoreException;

    @Override
    public boolean isCommitting(final JtaXid xid) throws JtaTransactionStoreException {
        LOGGER.debug("isCommitting(xid={})", xid);
        return TransactionStatus.COMMITTING.equals(getPersistentTransaction(xid).getStatus());
    }

    @Override
    public void active(final JtaXid xid) throws JtaTransactionStoreException {
        LOGGER.debug("active(xid={})", xid);
        getPersistentTransaction(xid).save(TransactionStatus.ACTIVE);
    }

    @Override
    public void active(final JtaXid xid, final String resourceManager) throws JtaTransactionStoreException {
        LOGGER.debug("active(xid={}, resourceManager={})", xid, resourceManager);
        getPersistentTransaction(xid).save(TransactionStatus.ACTIVE, resourceManager);
    }

    @Override
    public void preparing(final JtaXid xid) throws JtaTransactionStoreException {
        LOGGER.debug("preparing(xid={})", xid);
        getPersistentTransaction(xid).save(TransactionStatus.PREPARING);
    }

    @Override
    public void preparing(final JtaXid xid, final String resourceManager) throws JtaTransactionStoreException {
        LOGGER.debug("preparing(xid={}, resourceManager={})", xid, resourceManager);
        getPersistentTransaction(xid).save(TransactionStatus.PREPARING, resourceManager);
    }

    @Override
    public void prepared(final JtaXid xid) throws JtaTransactionStoreException {
        LOGGER.debug("prepared(xid={})", xid);
        getPersistentTransaction(xid).save(TransactionStatus.PREPARED);
    }

    @Override
    public void prepared(final JtaXid xid, final String resourceManager) throws JtaTransactionStoreException {
        LOGGER.debug("prepared(xid={}, resourceManager={})", xid, resourceManager);
        getPersistentTransaction(xid).save(TransactionStatus.PREPARED, resourceManager);
    }

    @Override
    public void committing(final JtaXid xid) throws JtaTransactionStoreException {
        LOGGER.debug("committing(xid={})", xid);
        getPersistentTransaction(xid).save(TransactionStatus.COMMITTING);
    }

    @Override
    public void committing(final JtaXid xid, final String resourceManager) throws JtaTransactionStoreException {
        LOGGER.debug("committing(xid={}, resourceManager={})", xid, resourceManager);
        getPersistentTransaction(xid).save(TransactionStatus.COMMITTING, resourceManager);
    }

    @Override
    public void committed(final JtaXid xid) throws JtaTransactionStoreException {
        LOGGER.debug("committed(xid={})", xid);
        final PersistentTransaction persistentTransaction = getPersistentTransaction(xid);
        persistentTransaction.save(TransactionStatus.COMMITTED);
        persistentTransaction.remove();
    }

    @Override
    public void committed(final JtaXid xid, final String resourceManager) throws JtaTransactionStoreException {
        LOGGER.debug("committed(xid={}, resourceManager={})", xid, resourceManager);
        getPersistentTransaction(xid).save(TransactionStatus.COMMITTED, resourceManager);
    }

    @Override
    public void commitFailed(final JtaXid xid) throws JtaTransactionStoreException {
        LOGGER.debug("commitFailed(xid={})", xid);
        getPersistentTransaction(xid).save(TransactionStatus.COMMIT_FAILED);
    }

    @Override
    public void commitFailed(final JtaXid xid, final String resourceManager, final XAException cause) throws JtaTransactionStoreException {
        LOGGER.debug("commitFailed(xid={}, resourceManager={})", xid, resourceManager, cause);
        getPersistentTransaction(xid).save(TransactionStatus.COMMIT_FAILED, resourceManager, cause);
    }

    @Override
    public void rollingBack(final JtaXid xid) throws JtaTransactionStoreException {
        LOGGER.debug("rollingBack(xid={})", xid);
        getPersistentTransaction(xid).save(TransactionStatus.ROLLING_BACK);
    }

    @Override
    public void rollingBack(final JtaXid xid, final String resourceManager) throws JtaTransactionStoreException {
        LOGGER.debug("rollingBack(xid={}, resourceManager={})", xid, resourceManager);
        getPersistentTransaction(xid).save(TransactionStatus.ROLLING_BACK, resourceManager);
    }

    @Override
    public void rolledBack(final JtaXid xid) throws JtaTransactionStoreException {
        LOGGER.debug("rolledBack(xid={})", xid);
        final PersistentTransaction persistentTransaction = getPersistentTransaction(xid);
        persistentTransaction.save(TransactionStatus.ROLLED_BACK);
        persistentTransaction.remove();
    }

    @Override
    public void rolledBack(final JtaXid xid, final String resourceManager) throws JtaTransactionStoreException {
        LOGGER.debug("rolledBack(xid={}, resourceManager={})", xid, resourceManager);
        getPersistentTransaction(xid).save(TransactionStatus.ROLLED_BACK, resourceManager);
    }

    @Override
    public void rollbackFailed(final JtaXid xid) throws JtaTransactionStoreException {
        LOGGER.debug("rollbackFailed(xid={})", xid);
        getPersistentTransaction(xid).save(TransactionStatus.ROLLBACK_FAILED);
    }

    @Override
    public void rollbackFailed(final JtaXid xid, final String resourceManager, final XAException cause) throws JtaTransactionStoreException {
        LOGGER.debug("rollbackFailed(xid={}, resourceManager={})", xid, resourceManager, cause);
        getPersistentTransaction(xid).save(TransactionStatus.ROLLBACK_FAILED, resourceManager, cause);
    }
}
