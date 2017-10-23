package nl.futureedge.simple.jta.store.impl;

import javax.transaction.xa.XAException;
import nl.futureedge.simple.jta.JtaXid;
import nl.futureedge.simple.jta.store.JtaTransactionStore;
import nl.futureedge.simple.jta.store.JtaTransactionStoreException;

public abstract class BaseTransactionStore implements JtaTransactionStore {

    protected abstract PersistentTransaction getPersistentTransaction(JtaXid xid) throws JtaTransactionStoreException;

    @Override
    public boolean isCommitting(final JtaXid xid) throws JtaTransactionStoreException {
        return TransactionStatus.COMMITTING.equals(getPersistentTransaction(xid).getStatus());
    }

    @Override
    public void active(final JtaXid xid) throws JtaTransactionStoreException {
        getPersistentTransaction(xid).save(TransactionStatus.ACTIVE);
    }

    @Override
    public void active(final JtaXid xid, final String resourceManager) throws JtaTransactionStoreException {
        getPersistentTransaction(xid).save(TransactionStatus.ACTIVE, resourceManager);
    }

    @Override
    public void preparing(final JtaXid xid) throws JtaTransactionStoreException {
        getPersistentTransaction(xid).save(TransactionStatus.PREPARING);
    }

    @Override
    public void preparing(final JtaXid xid, final String resourceManager) throws JtaTransactionStoreException {
        getPersistentTransaction(xid).save(TransactionStatus.PREPARING, resourceManager);
    }

    @Override
    public void prepared(final JtaXid xid) throws JtaTransactionStoreException {
        getPersistentTransaction(xid).save(TransactionStatus.PREPARED);
    }

    @Override
    public void prepared(final JtaXid xid, final String resourceManager) throws JtaTransactionStoreException {
        getPersistentTransaction(xid).save(TransactionStatus.PREPARED, resourceManager);
    }

    @Override
    public void committing(final JtaXid xid) throws JtaTransactionStoreException {
        getPersistentTransaction(xid).save(TransactionStatus.COMMITTING);
    }

    @Override
    public void committing(final JtaXid xid, final String resourceManager) throws JtaTransactionStoreException {
        getPersistentTransaction(xid).save(TransactionStatus.COMMITTING, resourceManager);
    }

    @Override
    public void committed(final JtaXid xid) throws JtaTransactionStoreException {
        final PersistentTransaction persistentTransaction = getPersistentTransaction(xid);
        persistentTransaction.save(TransactionStatus.COMMITTED);
        persistentTransaction.remove();
    }

    @Override
    public void committed(final JtaXid xid, final String resourceManager) throws JtaTransactionStoreException {
        getPersistentTransaction(xid).save(TransactionStatus.COMMITTED, resourceManager);
    }

    @Override
    public void commitFailed(final JtaXid xid) throws JtaTransactionStoreException {
        getPersistentTransaction(xid).save(TransactionStatus.COMMIT_FAILED);
    }

    @Override
    public void commitFailed(final JtaXid xid, final String resourceManager, final XAException cause) throws JtaTransactionStoreException {
        getPersistentTransaction(xid).save(TransactionStatus.COMMIT_FAILED, resourceManager, cause);
    }

    @Override
    public void rollingBack(final JtaXid xid) throws JtaTransactionStoreException {
        getPersistentTransaction(xid).save(TransactionStatus.ROLLINGBACK);
    }

    @Override
    public void rollingBack(final JtaXid xid, final String resourceManager) throws JtaTransactionStoreException {
        getPersistentTransaction(xid).save(TransactionStatus.ROLLINGBACK, resourceManager);
    }

    @Override
    public void rolledBack(final JtaXid xid) throws JtaTransactionStoreException {
        final PersistentTransaction persistentTransaction = getPersistentTransaction(xid);
        persistentTransaction.save(TransactionStatus.ROLLED_BACK);
        persistentTransaction.remove();
    }

    @Override
    public void rolledBack(final JtaXid xid, final String resourceManager) throws JtaTransactionStoreException {
        getPersistentTransaction(xid).save(TransactionStatus.ROLLED_BACK, resourceManager);
    }

    @Override
    public void rollbackFailed(final JtaXid xid) throws JtaTransactionStoreException {
        getPersistentTransaction(xid).save(TransactionStatus.ROLLBACK_FAILED);
    }

    @Override
    public void rollbackFailed(final JtaXid xid, final String resourceManager, final XAException cause) throws JtaTransactionStoreException {
        getPersistentTransaction(xid).save(TransactionStatus.ROLLBACK_FAILED, resourceManager, cause);
    }
}
