package nl.futureedge.simple.jta;

import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import nl.futureedge.simple.jta.store.JtaTransactionStore;
import nl.futureedge.simple.jta.store.JtaTransactionStoreException;
import nl.futureedge.simple.jta.xa.XAResourceAdapter;
import nl.futureedge.simple.jta.xid.BranchJtaXid;
import nl.futureedge.simple.jta.xid.GlobalJtaXid;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mockito;

public class JtaTransactionSynchronizationTest {

    private XAResource resourceOne;
    private XAResource resourceTwo;
    private BranchJtaXid branchXidOne;
    private BranchJtaXid branchXidTwo;
    private Synchronization synchronization;

    private JtaTransactionStore transactionStore;
    private JtaTransactionManager transactionManager;
    private JtaTransaction transaction;
    private GlobalJtaXid globalXid;

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
        resourceTwo = Mockito.mock(XAResource.class);
        transaction.enlistResource(new XAResourceAdapter("resourceOne", false, false, resourceOne));
        transaction.enlistResource(new XAResourceAdapter("resourceTwo", false, false, resourceTwo));

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
        final ArgumentCaptor<BranchJtaXid> branchXidOneCaptor = ArgumentCaptor.forClass(BranchJtaXid.class);
        ordered.verify(transactionStore).active(branchXidOneCaptor.capture(),Mockito.eq( "resourceOne"));
        branchXidOne = branchXidOneCaptor.getValue();
        ordered.verify(resourceOne).start(branchXidOne, XAResource.TMNOFLAGS);

        final ArgumentCaptor<BranchJtaXid> branchXidTwoCaptor = ArgumentCaptor.forClass(BranchJtaXid.class);
        ordered.verify(transactionStore).active(branchXidTwoCaptor.capture(),Mockito.eq( "resourceTwo"));
        branchXidTwo = branchXidTwoCaptor.getValue();
        ordered.verify(resourceTwo).start(branchXidTwo, XAResource.TMNOFLAGS);
    }

    @After
    public void destroy() throws Exception {
        Mockito.verifyNoMoreInteractions(synchronization, transactionStore, resourceOne, resourceTwo);

        transactionManager.destroy();
    }

    @Test
    public void testCommit() throws Exception {
        transaction.commit();

        InOrder ordered = Mockito.inOrder(synchronization, transactionStore, resourceOne, resourceTwo);
        verifySetup(ordered);

        // Before completion
        ordered.verify(synchronization).beforeCompletion();

        // Commit (prepare)
        ordered.verify(transactionStore).preparing(globalXid);
        ordered.verify(transactionStore).preparing(branchXidOne, "resourceOne");
        ordered.verify(resourceOne).end(branchXidOne, XAResource.TMSUCCESS);
        ordered.verify(resourceOne).prepare(branchXidOne);
        ordered.verify(transactionStore).prepared(branchXidOne, "resourceOne");
        ordered.verify(transactionStore).preparing(branchXidTwo, "resourceTwo");
        ordered.verify(resourceTwo).end(branchXidTwo, XAResource.TMSUCCESS);
        ordered.verify(resourceTwo).prepare(branchXidTwo);
        ordered.verify(transactionStore).prepared(branchXidTwo, "resourceTwo");
        ordered.verify(transactionStore).prepared(globalXid);

        // Commit (commit)
        ordered.verify(transactionStore).committing(globalXid);
        ordered.verify(transactionStore).committing(branchXidOne, "resourceOne");
        ordered.verify(resourceOne).commit(branchXidOne, false);
        ordered.verify(transactionStore).committed(branchXidOne, "resourceOne");
        ordered.verify(transactionStore).committing(branchXidTwo, "resourceTwo");
        ordered.verify(resourceTwo).commit(branchXidTwo, false);
        ordered.verify(transactionStore).committed(branchXidTwo, "resourceTwo");
        ordered.verify(transactionStore).committed(globalXid);

        ordered.verify(transactionStore).transactionCompleted(transaction);

        // After completion
        ordered.verify(synchronization).afterCompletion(Status.STATUS_COMMITTED);
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

        InOrder ordered = Mockito.inOrder(synchronization, transactionStore, resourceOne, resourceTwo);
        verifySetup(ordered);

        // Before completion
        ordered.verify(synchronization).beforeCompletion();

        // Commit (prepare)
        ordered.verify(transactionStore).preparing(globalXid);
        ordered.verify(transactionStore).preparing(branchXidOne, "resourceOne");
        ordered.verify(resourceOne).end(branchXidOne, XAResource.TMSUCCESS);
        ordered.verify(resourceOne).prepare(branchXidOne);

        // Rollback
        ordered.verify(transactionStore).rollingBack(globalXid);
        ordered.verify(transactionStore).rollingBack(branchXidOne, "resourceOne");
        ordered.verify(resourceOne).rollback(branchXidOne);
        ordered.verify(transactionStore).rolledBack(branchXidOne, "resourceOne");
        ordered.verify(transactionStore).rollingBack(branchXidTwo, "resourceTwo");
        ordered.verify(resourceTwo).end(branchXidTwo, XAResource.TMFAIL);
        ordered.verify(resourceTwo).rollback(branchXidTwo);
        ordered.verify(transactionStore).rolledBack(branchXidTwo, "resourceTwo");
        ordered.verify(transactionStore).rolledBack(globalXid);

        ordered.verify(transactionStore).transactionCompleted(transaction);

        // After completion
        ordered.verify(synchronization).afterCompletion(Status.STATUS_ROLLEDBACK);

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

        InOrder ordered = Mockito.inOrder(synchronization, transactionStore, resourceOne, resourceTwo);
        verifySetup(ordered);

        // Before completion
        ordered.verify(synchronization).beforeCompletion();

        // Rollback
        ordered.verify(transactionStore).rollingBack(globalXid);
        ordered.verify(transactionStore).rollingBack(branchXidOne, "resourceOne");
        ordered.verify(resourceOne).end(branchXidOne, XAResource.TMFAIL);
        ordered.verify(resourceOne).rollback(branchXidOne);
        ordered.verify(transactionStore).rolledBack(branchXidOne, "resourceOne");
        ordered.verify(transactionStore).rollingBack(branchXidTwo, "resourceTwo");
        ordered.verify(resourceTwo).end(branchXidTwo, XAResource.TMFAIL);
        ordered.verify(resourceTwo).rollback(branchXidTwo);
        ordered.verify(transactionStore).rolledBack(branchXidTwo, "resourceTwo");
        ordered.verify(transactionStore).rolledBack(globalXid);

        ordered.verify(transactionStore).transactionCompleted(transaction);

        // After completion
        ordered.verify(synchronization).afterCompletion(Status.STATUS_ROLLEDBACK);

    }
}
