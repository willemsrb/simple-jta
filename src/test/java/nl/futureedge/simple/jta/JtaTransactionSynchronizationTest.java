package nl.futureedge.simple.jta;

import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import nl.futureedge.simple.jta.store.JtaTransactionStore;
import nl.futureedge.simple.jta.store.JtaTransactionStoreException;
import nl.futureedge.simple.jta.xa.XAResourceAdapter;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mockito;

public class JtaTransactionSynchronizationTest {

    private XAResource resourceOne;
    private JtaXid branchXidOne;
    private Synchronization synchronization;

    private JtaTransactionStore transactionStore;
    private JtaTransactionManager transactionManager;
    private JtaTransaction transaction;
    private JtaXid globalXid;

    @Before
    public void setup() throws Exception {
        transactionStore = Mockito.mock(JtaTransactionStore.class);
        transactionManager = new JtaTransactionManager();
        transactionManager.setUniqueName("tm");
        transactionManager.setJtaTransactionStore(transactionStore);
        transactionManager.afterPropertiesSet();

        transactionManager.begin();
        transaction = transactionManager.getTransaction();
        Assert.assertEquals(Status.STATUS_ACTIVE, transaction.getStatus());
        globalXid = ReflectionTestUtils.getField(transaction, "globalXid");

        resourceOne = Mockito.mock(XAResource.class);
        transaction.enlistResource(new XAResourceAdapter("resourceOne", true, resourceOne));

        synchronization = Mockito.mock(Synchronization.class);
        transaction.registerSynchronization(synchronization);
    }

    private void verifySetup(InOrder ordered) throws JtaTransactionStoreException, XAException {
        // Startup manager
        ordered.verify(transactionStore).cleanup();

        // Start transaction
        ordered.verify(transactionStore).nextTransactionId();
        ordered.verify(transactionStore).active(globalXid);

        // Enlist resource
        ordered.verify(transactionStore).active(globalXid, "resourceOne");
        final ArgumentCaptor<JtaXid> branchXidOneCaptor = ArgumentCaptor.forClass(JtaXid.class);
        ordered.verify(resourceOne).start(branchXidOneCaptor.capture(), Mockito.eq(XAResource.TMNOFLAGS));
        branchXidOne = branchXidOneCaptor.getValue();
    }

    @After
    public void destroy() throws Exception {
        transactionManager.destroy();
    }

    @Test
    public void testCommit() throws Exception {
        transaction.commit();

        InOrder ordered = Mockito.inOrder(synchronization, transactionStore, resourceOne);
        verifySetup(ordered);

        // Before completion
        ordered.verify(synchronization).beforeCompletion();

        // Commit (prepare)
        ordered.verify(transactionStore).preparing(globalXid);
        ordered.verify(transactionStore).preparing(globalXid, "resourceOne");
        ordered.verify(resourceOne).end(branchXidOne, XAResource.TMSUCCESS);
        ordered.verify(resourceOne).prepare(branchXidOne);
        ordered.verify(transactionStore).prepared(globalXid, "resourceOne");
        ordered.verify(transactionStore).prepared(globalXid);

        // Commit (commit)
        ordered.verify(transactionStore).committing(globalXid);
        ordered.verify(transactionStore).committing(globalXid, "resourceOne");
        ordered.verify(resourceOne).commit(branchXidOne, false);
        ordered.verify(transactionStore).committed(globalXid, "resourceOne");
        ordered.verify(transactionStore).committed(globalXid);

        // After completion
        ordered.verify(synchronization).afterCompletion(Status.STATUS_COMMITTED);

        Mockito.verifyNoMoreInteractions(synchronization, transactionStore, resourceOne);
    }

    @Test
    public void testPrepareFailure() throws Exception {
        Mockito.when(resourceOne.prepare(Mockito.any())).thenThrow(new XAException("Fail"));
        try {
            transaction.commit();
            Assert.fail("RollbackException expected");
        } catch (RollbackException e) {
            // Expected
        }

        InOrder ordered = Mockito.inOrder(synchronization, transactionStore, resourceOne);
        verifySetup(ordered);

        // Before completion
        ordered.verify(synchronization).beforeCompletion();

        // Commit (prepare)
        ordered.verify(transactionStore).preparing(globalXid);
        ordered.verify(transactionStore).preparing(globalXid, "resourceOne");
        ordered.verify(resourceOne).end(branchXidOne, XAResource.TMSUCCESS);
        ordered.verify(resourceOne).prepare(branchXidOne);

        // Rollback
        ordered.verify(transactionStore).rollingBack(globalXid);
        ordered.verify(transactionStore).rollingBack(globalXid, "resourceOne");
        ordered.verify(resourceOne).rollback(branchXidOne);
        ordered.verify(transactionStore).rolledBack(globalXid, "resourceOne");
        ordered.verify(transactionStore).rolledBack(globalXid);

        // After completion
        ordered.verify(synchronization).afterCompletion(Status.STATUS_ROLLEDBACK);

        Mockito.verifyNoMoreInteractions(synchronization, transactionStore, resourceOne);
    }

    @Test
    public void testBeforeCompletionFailure() throws Exception {
        Mockito.doThrow(new NullPointerException("Fail")).when(synchronization).beforeCompletion();
        try {
            transaction.commit();
            Assert.fail("RollbackException expected");
        } catch (RollbackException e) {
            // Expected
        }

        InOrder ordered = Mockito.inOrder(synchronization, transactionStore, resourceOne);
        verifySetup(ordered);

        // Before completion
        ordered.verify(synchronization).beforeCompletion();

        // Rollback
        ordered.verify(transactionStore).rollingBack(globalXid);
        ordered.verify(transactionStore).rollingBack(globalXid, "resourceOne");
        ordered.verify(resourceOne).end(branchXidOne, XAResource.TMFAIL);
        ordered.verify(resourceOne).rollback(branchXidOne);
        ordered.verify(transactionStore).rolledBack(globalXid, "resourceOne");
        ordered.verify(transactionStore).rolledBack(globalXid);

        // After completion
        ordered.verify(synchronization).afterCompletion(Status.STATUS_ROLLEDBACK);

        Mockito.verifyNoMoreInteractions(synchronization, transactionStore, resourceOne);
    }
}
