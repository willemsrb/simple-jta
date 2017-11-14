package nl.futureedge.simple.jta;

import javax.transaction.SystemException;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;
import nl.futureedge.simple.jta.store.JtaTransactionStore;
import nl.futureedge.simple.jta.store.JtaTransactionStoreException;
import nl.futureedge.simple.jta.xa.XAResourceAdapter;
import nl.futureedge.simple.jta.xid.BranchJtaXid;
import nl.futureedge.simple.jta.xid.GlobalJtaXid;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class JtaTransactionManagerRecoveryTest {

    private static final String TRANSACTION_MANAGER = "transactionManager";
    private static final String RESOURCE_MANAGER = "resourceManager";

    private XAResource xaResource;
    private XAResourceAdapter xaResourceAdapter;
    private JtaTransactionStore transactionStore;

    private JtaTransactionManager subject;

    @Before
    public void setup() throws Exception {
        transactionStore = Mockito.mock(JtaTransactionStore.class);
        subject = new JtaTransactionManager();
        subject.setUniqueName(TRANSACTION_MANAGER);
        subject.setJtaTransactionStore(transactionStore);

        xaResource = Mockito.mock(XAResource.class);
        xaResourceAdapter = new XAResourceAdapter(RESOURCE_MANAGER, false, false, xaResource);
    }

    @Test
    public void recoverNothing() throws Exception {
        Mockito.when(xaResource.recover(XAResource.TMENDRSCAN)).thenReturn(new Xid[]{});

        subject.recover(xaResourceAdapter);

        Mockito.verify(xaResource).recover(XAResource.TMENDRSCAN);
        Mockito.verify(transactionStore).cleanup();
        Mockito.verifyNoMoreInteractions(xaResource, transactionStore);
    }

    @Test
    public void recoverFailed() throws Exception {
        Mockito.doThrow(new XAException("Test")).when(xaResource).recover(XAResource.TMENDRSCAN);

        subject.recover(xaResourceAdapter);

        Mockito.verify(xaResource).recover(XAResource.TMENDRSCAN);
        Mockito.verifyNoMoreInteractions(xaResource, transactionStore);
    }

    @Test
    public void recover() throws Exception {
        GlobalJtaXid globalXid1 = new GlobalJtaXid(TRANSACTION_MANAGER, 1);
        BranchJtaXid xid1 = globalXid1.createBranchXid();
        BranchJtaXid xid2 = new GlobalJtaXid("other", 2).createBranchXid();
        GlobalJtaXid globalXid3 = new GlobalJtaXid(TRANSACTION_MANAGER, 3);
        BranchJtaXid xid3 = globalXid3.createBranchXid();

        Mockito.when(transactionStore.isCommitting(xid1)).thenReturn(true);
        Mockito.when(transactionStore.isCommitting(xid3)).thenReturn(false);

        Mockito.when(xaResource.recover(XAResource.TMENDRSCAN)).thenReturn(new Xid[]{xid1, xid2, xid3});

        subject.recover(xaResourceAdapter);

        Mockito.verify(xaResource).recover(XAResource.TMENDRSCAN);
        Mockito.verify(transactionStore).isCommitting(xid1);
        Mockito.verify(transactionStore).committing(xid1, RESOURCE_MANAGER);
        Mockito.verify(xaResource).commit(xid1, true);
        Mockito.verify(transactionStore).committed(xid1, RESOURCE_MANAGER);
        Mockito.verify(transactionStore).isCommitting(xid3);
        Mockito.verify(transactionStore).rollingBack(xid3, RESOURCE_MANAGER);
        Mockito.verify(xaResource).rollback(xid3);
        Mockito.verify(transactionStore).rolledBack(xid3, RESOURCE_MANAGER);

        Mockito.verify(transactionStore).cleanup();
        Mockito.verifyNoMoreInteractions(xaResource, transactionStore);
    }

    @Test
    public void recoverCleanupFailed() throws Exception {
        Mockito.when(xaResource.recover(XAResource.TMENDRSCAN)).thenReturn(new Xid[]{});
        Mockito.doThrow(new JtaTransactionStoreException("Test")).when(transactionStore).cleanup();

        subject.recover(xaResourceAdapter);

        Mockito.verify(xaResource).recover(XAResource.TMENDRSCAN);
        Mockito.verify(transactionStore).cleanup();
        Mockito.verifyNoMoreInteractions(xaResource, transactionStore);
    }

    @Test
    public void recoverStoreIsCommittingFailed() throws Exception {
        GlobalJtaXid globalXid1 = new GlobalJtaXid(TRANSACTION_MANAGER, 1);
        BranchJtaXid xid1 = globalXid1.createBranchXid();
        GlobalJtaXid globalXid2 = new GlobalJtaXid(TRANSACTION_MANAGER, 2);
        BranchJtaXid xid2 = globalXid2.createBranchXid();

        XAException cause = new XAException("Test");
        Mockito.doThrow(cause).when(xaResource).commit(xid1, true);

        Mockito.doThrow(new JtaTransactionStoreException("Test")).when(transactionStore).isCommitting(xid1);
        Mockito.when(transactionStore.isCommitting(xid2)).thenReturn(false);

        Mockito.when(xaResource.recover(XAResource.TMENDRSCAN)).thenReturn(new Xid[]{xid1, xid2});

        subject.recover(xaResourceAdapter);

        Mockito.verify(xaResource).recover(XAResource.TMENDRSCAN);
        Mockito.verify(transactionStore).isCommitting(xid1);
        Mockito.verify(transactionStore).isCommitting(xid2);
        Mockito.verify(transactionStore).rollingBack(xid2, RESOURCE_MANAGER);
        Mockito.verify(xaResource).rollback(xid2);
        Mockito.verify(transactionStore).rolledBack(xid2, RESOURCE_MANAGER);

        Mockito.verify(transactionStore).cleanup();
        Mockito.verifyNoMoreInteractions(xaResource, transactionStore);
    }

    @Test
    public void recoverCommitFailed() throws Exception {
        GlobalJtaXid globalXid1 = new GlobalJtaXid(TRANSACTION_MANAGER, 1);
        BranchJtaXid xid1 = globalXid1.createBranchXid();
        GlobalJtaXid globalXid2 = new GlobalJtaXid(TRANSACTION_MANAGER, 2);
        BranchJtaXid xid2 = globalXid2.createBranchXid();

        XAException cause = new XAException("Test");
        Mockito.doThrow(cause).when(xaResource).commit(xid1, true);

        Mockito.when(transactionStore.isCommitting(xid1)).thenReturn(true);
        Mockito.when(transactionStore.isCommitting(xid2)).thenReturn(false);

        Mockito.when(xaResource.recover(XAResource.TMENDRSCAN)).thenReturn(new Xid[]{xid1, xid2});

        subject.recover(xaResourceAdapter);

        Mockito.verify(xaResource).recover(XAResource.TMENDRSCAN);
        Mockito.verify(transactionStore).isCommitting(xid1);
        Mockito.verify(transactionStore).committing(xid1, RESOURCE_MANAGER);
        Mockito.verify(xaResource).commit(xid1, true);
        Mockito.verify(transactionStore).commitFailed(xid1, RESOURCE_MANAGER, cause);
        Mockito.verify(transactionStore).isCommitting(xid2);
        Mockito.verify(transactionStore).rollingBack(xid2, RESOURCE_MANAGER);
        Mockito.verify(xaResource).rollback(xid2);
        Mockito.verify(transactionStore).rolledBack(xid2, RESOURCE_MANAGER);

        Mockito.verify(transactionStore).cleanup();
        Mockito.verifyNoMoreInteractions(xaResource, transactionStore);
    }

    @Test
    public void recoverStoreCommittingFailed() throws Exception {
        GlobalJtaXid globalXid1 = new GlobalJtaXid(TRANSACTION_MANAGER, 1);
        BranchJtaXid xid1 = globalXid1.createBranchXid();
        GlobalJtaXid globalXid2 = new GlobalJtaXid(TRANSACTION_MANAGER, 2);
        BranchJtaXid xid2 = globalXid2.createBranchXid();

        Mockito.when(xaResource.recover(XAResource.TMENDRSCAN)).thenReturn(new Xid[]{xid1, xid2});

        JtaTransactionStoreException cause = new JtaTransactionStoreException("Test");
        Mockito.doThrow(cause).when(transactionStore).committing(xid1, RESOURCE_MANAGER);
        Mockito.when(transactionStore.isCommitting(xid1)).thenReturn(true);

        try {
            subject.recover(xaResourceAdapter);
            Assert.fail("SystemException expected");
        } catch (SystemException e) {
            // Expected
        }

        Mockito.verify(xaResource).recover(XAResource.TMENDRSCAN);
        Mockito.verify(transactionStore).isCommitting(xid1);
        Mockito.verify(transactionStore).committing(xid1, RESOURCE_MANAGER);

        Mockito.verifyNoMoreInteractions(xaResource, transactionStore);
    }

    @Test
    public void recoverStoreCommittedFailed() throws Exception {
        GlobalJtaXid globalXid1 = new GlobalJtaXid(TRANSACTION_MANAGER, 1);
        BranchJtaXid xid1 = globalXid1.createBranchXid();
        GlobalJtaXid globalXid2 = new GlobalJtaXid(TRANSACTION_MANAGER, 2);
        BranchJtaXid xid2 = globalXid2.createBranchXid();

        Mockito.when(xaResource.recover(XAResource.TMENDRSCAN)).thenReturn(new Xid[]{xid1, xid2});

        JtaTransactionStoreException cause = new JtaTransactionStoreException("Test");
        Mockito.doThrow(cause).when(transactionStore).committed(xid1, RESOURCE_MANAGER);
        Mockito.when(transactionStore.isCommitting(xid1)).thenReturn(true);

        try {
            subject.recover(xaResourceAdapter);
            Assert.fail("SystemException expected");
        } catch (SystemException e) {
            // Expected
        }

        Mockito.verify(xaResource).recover(XAResource.TMENDRSCAN);
        Mockito.verify(transactionStore).isCommitting(xid1);
        Mockito.verify(transactionStore).committing(xid1, RESOURCE_MANAGER);
        Mockito.verify(xaResource).commit(xid1, true);
        Mockito.verify(transactionStore).committed(xid1, RESOURCE_MANAGER);

        Mockito.verifyNoMoreInteractions(xaResource, transactionStore);
    }

    @Test
    public void recoverStoreCommitFailedFailed() throws Exception {
        GlobalJtaXid globalXid1 = new GlobalJtaXid(TRANSACTION_MANAGER, 1);
        BranchJtaXid xid1 = globalXid1.createBranchXid();
        GlobalJtaXid globalXid2 = new GlobalJtaXid(TRANSACTION_MANAGER, 2);
        BranchJtaXid xid2 = globalXid2.createBranchXid();

        Mockito.when(xaResource.recover(XAResource.TMENDRSCAN)).thenReturn(new Xid[]{xid1, xid2});

        Mockito.when(transactionStore.isCommitting(xid1)).thenReturn(true);
        XAException commitCause = new XAException("Test");
        Mockito.doThrow(commitCause).when(xaResource).commit(xid1, true);
        Mockito.doThrow(new JtaTransactionStoreException("Test")).when(transactionStore).commitFailed(xid1, RESOURCE_MANAGER, commitCause);

        try {
            subject.recover(xaResourceAdapter);
            Assert.fail("SystemException expected");
        } catch (SystemException e) {
            // Expected
        }

        Mockito.verify(xaResource).recover(XAResource.TMENDRSCAN);
        Mockito.verify(transactionStore).isCommitting(xid1);
        Mockito.verify(transactionStore).committing(xid1, RESOURCE_MANAGER);
        Mockito.verify(xaResource).commit(xid1, true);
        Mockito.verify(transactionStore).commitFailed(xid1, RESOURCE_MANAGER, commitCause);

        Mockito.verifyNoMoreInteractions(xaResource, transactionStore);
    }

    @Test
    public void recoverRollbackFailed() throws Exception {
        GlobalJtaXid globalXid1 = new GlobalJtaXid(TRANSACTION_MANAGER, 1);
        BranchJtaXid xid1 = globalXid1.createBranchXid();
        GlobalJtaXid globalXid2 = new GlobalJtaXid(TRANSACTION_MANAGER, 2);
        BranchJtaXid xid2 = globalXid2.createBranchXid();

        XAException cause = new XAException("Test");
        Mockito.doThrow(cause).when(xaResource).rollback(xid1);

        Mockito.when(transactionStore.isCommitting(xid1)).thenReturn(false);
        Mockito.when(transactionStore.isCommitting(xid2)).thenReturn(false);

        Mockito.when(xaResource.recover(XAResource.TMENDRSCAN)).thenReturn(new Xid[]{xid1, xid2});

        subject.recover(xaResourceAdapter);

        Mockito.verify(xaResource).recover(XAResource.TMENDRSCAN);
        Mockito.verify(transactionStore).isCommitting(xid1);
        Mockito.verify(transactionStore).rollingBack(xid1, RESOURCE_MANAGER);
        Mockito.verify(xaResource).rollback(xid1);
        Mockito.verify(transactionStore).rollbackFailed(xid1, RESOURCE_MANAGER, cause);
        Mockito.verify(transactionStore).isCommitting(xid2);
        Mockito.verify(transactionStore).rollingBack(xid2, RESOURCE_MANAGER);
        Mockito.verify(xaResource).rollback(xid2);
        Mockito.verify(transactionStore).rolledBack(xid2, RESOURCE_MANAGER);

        Mockito.verify(transactionStore).cleanup();
        Mockito.verifyNoMoreInteractions(xaResource, transactionStore);
    }

    @Test
    public void recoverStoreRollingBackFailed() throws Exception {
        GlobalJtaXid globalXid1 = new GlobalJtaXid(TRANSACTION_MANAGER, 1);
        BranchJtaXid xid1 = globalXid1.createBranchXid();
        GlobalJtaXid globalXid2 = new GlobalJtaXid(TRANSACTION_MANAGER, 2);
        BranchJtaXid xid2 = globalXid2.createBranchXid();

        Mockito.when(xaResource.recover(XAResource.TMENDRSCAN)).thenReturn(new Xid[]{xid1, xid2});

        JtaTransactionStoreException cause = new JtaTransactionStoreException("Test");
        Mockito.doThrow(cause).when(transactionStore).rollingBack(xid1, RESOURCE_MANAGER);

        try {
            subject.recover(xaResourceAdapter);
            Assert.fail("SystemException expected");
        } catch (SystemException e) {
            // Expected
        }

        Mockito.verify(xaResource).recover(XAResource.TMENDRSCAN);
        Mockito.verify(transactionStore).isCommitting(xid1);
        Mockito.verify(transactionStore).rollingBack(xid1, RESOURCE_MANAGER);

        Mockito.verifyNoMoreInteractions(xaResource, transactionStore);
    }

    @Test
    public void recoverStoreRolledbackFailed() throws Exception {
        GlobalJtaXid globalXid1 = new GlobalJtaXid(TRANSACTION_MANAGER, 1);
        BranchJtaXid xid1 = globalXid1.createBranchXid();
        GlobalJtaXid globalXid2 = new GlobalJtaXid(TRANSACTION_MANAGER, 2);
        BranchJtaXid xid2 = globalXid2.createBranchXid();

        Mockito.when(xaResource.recover(XAResource.TMENDRSCAN)).thenReturn(new Xid[]{xid1, xid2});

        JtaTransactionStoreException cause = new JtaTransactionStoreException("Test");
        Mockito.doThrow(cause).when(transactionStore).rolledBack(xid1, RESOURCE_MANAGER);

        try {
            subject.recover(xaResourceAdapter);
            Assert.fail("SystemException expected");
        } catch (SystemException e) {
            // Expected
        }

        Mockito.verify(xaResource).recover(XAResource.TMENDRSCAN);
        Mockito.verify(transactionStore).isCommitting(xid1);
        Mockito.verify(transactionStore).rollingBack(xid1, RESOURCE_MANAGER);
        Mockito.verify(xaResource).rollback(xid1);
        Mockito.verify(transactionStore).rolledBack(xid1, RESOURCE_MANAGER);

        Mockito.verifyNoMoreInteractions(xaResource, transactionStore);
    }

    @Test
    public void recoverStoreRollbackFailedFailed() throws Exception {
        GlobalJtaXid globalXid1 = new GlobalJtaXid(TRANSACTION_MANAGER, 1);
        BranchJtaXid xid1 = globalXid1.createBranchXid();
        GlobalJtaXid globalXid2 = new GlobalJtaXid(TRANSACTION_MANAGER, 2);
        BranchJtaXid xid2 = globalXid2.createBranchXid();

        Mockito.when(xaResource.recover(XAResource.TMENDRSCAN)).thenReturn(new Xid[]{xid1, xid2});

        XAException commitCause = new XAException("Test");
        Mockito.doThrow(commitCause).when(xaResource).rollback(xid1);
        Mockito.doThrow(new JtaTransactionStoreException("Test")).when(transactionStore).rollbackFailed(xid1, RESOURCE_MANAGER, commitCause);

        try {
            subject.recover(xaResourceAdapter);
            Assert.fail("SystemException expected");
        } catch (SystemException e) {
            // Expected
        }

        Mockito.verify(xaResource).recover(XAResource.TMENDRSCAN);
        Mockito.verify(transactionStore).isCommitting(xid1);
        Mockito.verify(transactionStore).rollingBack(xid1, RESOURCE_MANAGER);
        Mockito.verify(xaResource).rollback(xid1);
        Mockito.verify(transactionStore).rollbackFailed(xid1, RESOURCE_MANAGER, commitCause);

        Mockito.verifyNoMoreInteractions(xaResource, transactionStore);
    }

}
