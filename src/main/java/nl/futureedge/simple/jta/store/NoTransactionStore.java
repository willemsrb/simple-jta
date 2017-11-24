package nl.futureedge.simple.jta.store;

import java.util.concurrent.atomic.AtomicLong;
import javax.transaction.xa.XAException;
import nl.futureedge.simple.jta.JtaTransaction;
import nl.futureedge.simple.jta.xid.BranchJtaXid;
import nl.futureedge.simple.jta.xid.GlobalJtaXid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

public final class NoTransactionStore implements JtaTransactionStore, InitializingBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(NoTransactionStore.class);

    private static AtomicLong TRANSACTION_ID = new AtomicLong();

    private boolean suppressWarning;

    public void setSuppressWarning(boolean suppressWarning) {
        this.suppressWarning = suppressWarning;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        if (!suppressWarning) {
            LOGGER.warn(
                    "*** WARNING ***\n*** WARNING ***\n*** WARNING: No persistent transaction storage! Recovery of partially committed transactions is NOT "
                            + "POSSIBLE!\n*** WARNING ***\n*** WARNING ***");
        }
    }

    @Override
    public long nextTransactionId() throws JtaTransactionStoreException {
        return TRANSACTION_ID.getAndIncrement();
    }

    @Override
    public boolean isCommitting(BranchJtaXid xid) throws JtaTransactionStoreException {
        return false;
    }

    @Override
    public void cleanup() throws JtaTransactionStoreException {
        // Nothing
    }

    @Override
    public void active(GlobalJtaXid xid) throws JtaTransactionStoreException {
        // Nothing
    }

    @Override
    public void active(BranchJtaXid xid, String resourceManager) throws JtaTransactionStoreException {
        // Nothing
    }

    @Override
    public void preparing(GlobalJtaXid xid) throws JtaTransactionStoreException {
        // Nothing
    }

    @Override
    public void preparing(BranchJtaXid xid, String resourceManager) throws JtaTransactionStoreException {
        // Nothing
    }

    @Override
    public void prepared(GlobalJtaXid xid) throws JtaTransactionStoreException {
        // Nothing
    }

    @Override
    public void prepared(BranchJtaXid xid, String resourceManager) throws JtaTransactionStoreException {
        // Nothing
    }

    @Override
    public void committing(GlobalJtaXid xid) throws JtaTransactionStoreException {
        // Nothing
    }

    @Override
    public void committing(BranchJtaXid xid, String resourceManager) throws JtaTransactionStoreException {
        // Nothing
    }

    @Override
    public void committed(GlobalJtaXid xid) throws JtaTransactionStoreException {
        // Nothing
    }

    @Override
    public void committed(BranchJtaXid xid, String resourceManager) throws JtaTransactionStoreException {
        // Nothing
    }

    @Override
    public void commitFailed(GlobalJtaXid xid) throws JtaTransactionStoreException {
        // Nothing
    }

    @Override
    public void commitFailed(BranchJtaXid xid, String resourceManager, XAException cause) throws JtaTransactionStoreException {
        // Nothing
    }

    @Override
    public void rollingBack(GlobalJtaXid xid) throws JtaTransactionStoreException {
        // Nothing
    }

    @Override
    public void rollingBack(BranchJtaXid xid, String resourceManager) throws JtaTransactionStoreException {
        // Nothing
    }

    @Override
    public void rolledBack(GlobalJtaXid xid) throws JtaTransactionStoreException {
        // Nothing
    }

    @Override
    public void rolledBack(BranchJtaXid xid, String resourceManager) throws JtaTransactionStoreException {
        // Nothing
    }

    @Override
    public void rollbackFailed(GlobalJtaXid xid) throws JtaTransactionStoreException {
        // Nothing
    }

    @Override
    public void rollbackFailed(BranchJtaXid xid, String resourceManager, XAException cause) throws JtaTransactionStoreException {
        // Nothing
    }

    @Override
    public void transactionCompleted(JtaTransaction transaction) {
        // Nothing
    }
}
