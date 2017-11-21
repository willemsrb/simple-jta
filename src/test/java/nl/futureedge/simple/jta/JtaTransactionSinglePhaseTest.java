package nl.futureedge.simple.jta;

import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.SystemException;
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

public class JtaTransactionSinglePhaseTest {

    private XAResource resourceOne;
    private BranchJtaXid branchXidOne;

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

        transaction.enlistResource(new XAResourceAdapter("resourceOne", true, false, resourceOne));
    }

    private void verifySetup(InOrder ordered) throws JtaTransactionStoreException, XAException {
        // Startup manager
        ordered.verify(transactionStore).cleanup();

        // Start transaction
        ordered.verify(transactionStore).nextTransactionId();
        ordered.verify(transactionStore).active(globalXid);

        // Enlist resource
        final ArgumentCaptor<BranchJtaXid> branchXidOneCaptor = ArgumentCaptor.forClass(BranchJtaXid.class);
        ordered.verify(transactionStore).active(branchXidOneCaptor.capture(), Mockito.eq("resourceOne"));
        branchXidOne = branchXidOneCaptor.getValue();
        ordered.verify(resourceOne).start(branchXidOne, XAResource.TMNOFLAGS);
    }

    private void commitExpectSystemException() throws RollbackException {
        try {
            transactionManager.commit();
            Assert.fail("SystemException expected");
        } catch (SystemException e) {
            // Expected
        }
    }

    @After
    public void destroy() throws Exception {
        transactionManager.destroy();
    }

    @Test
    public void testOk() throws Exception {
        transaction.commit();

        InOrder ordered = Mockito.inOrder(transactionStore, resourceOne);
        verifySetup(ordered);

        // Commit (commit)
        ordered.verify(transactionStore).committing(globalXid);
        ordered.verify(transactionStore).committing(branchXidOne, "resourceOne");
        ordered.verify(resourceOne).commit(branchXidOne, true);
        ordered.verify(transactionStore).committed(branchXidOne, "resourceOne");
        ordered.verify(transactionStore).committed(globalXid);

        ordered.verify(transactionStore).transactionCompleted(transaction);
        ordered.verifyNoMoreInteractions();
    }


    @Test
    public void testStoreCommittingFailure() throws Exception {
        Mockito.doThrow(new JtaTransactionStoreException("Fail")).when(transactionStore).committing(globalXid);
        try {
            transactionManager.commit();
            Assert.fail("RollbackException expected");
        } catch (RollbackException e) {
            // Expected
        }

        InOrder ordered = Mockito.inOrder(transactionStore, resourceOne);
        verifySetup(ordered);

        // Commit (commit)
        ordered.verify(transactionStore).committing(globalXid);

        // Rollback
        ordered.verify(transactionStore).rollingBack(globalXid);
        ordered.verify(transactionStore).rollingBack(branchXidOne, "resourceOne");
        ordered.verify(resourceOne).end(branchXidOne, XAResource.TMFAIL);
        ordered.verify(resourceOne).rollback(branchXidOne);
        ordered.verify(transactionStore).rolledBack(branchXidOne, "resourceOne");
        ordered.verify(transactionStore).rolledBack(globalXid);

        ordered.verify(transactionStore).transactionCompleted(transaction);
        Mockito.verifyNoMoreInteractions(transactionStore, resourceOne);
    }

    @Test
    public void testStoreCommittingResourceFailure() throws Exception {
        Mockito.doThrow(new JtaTransactionStoreException("Fail")).when(transactionStore).committing(Mockito.any(), Mockito.eq("resourceOne"));
        try {
            transactionManager.commit();
            Assert.fail("RollbackException expected");
        } catch (RollbackException e) {
            // Expected
        }

        InOrder ordered = Mockito.inOrder(transactionStore, resourceOne);
        verifySetup(ordered);

        // Commit (commit)
        ordered.verify(transactionStore).committing(globalXid);
        ordered.verify(transactionStore).committing(branchXidOne, "resourceOne");

        // Rollback
        ordered.verify(transactionStore).rollingBack(globalXid);
        ordered.verify(transactionStore).rollingBack(branchXidOne, "resourceOne");
        ordered.verify(resourceOne).end(branchXidOne, XAResource.TMFAIL);
        ordered.verify(resourceOne).rollback(branchXidOne);
        ordered.verify(transactionStore).rolledBack(branchXidOne, "resourceOne");
        ordered.verify(transactionStore).rolledBack(globalXid);

        ordered.verify(transactionStore).transactionCompleted(transaction);
        Mockito.verifyNoMoreInteractions(transactionStore, resourceOne);
    }

    @Test
    public void testStoreCommittedResourceFailure() throws Exception {
        Mockito.doThrow(new JtaTransactionStoreException("Fail")).when(transactionStore).committed(Mockito.any(), Mockito.eq("resourceOne"));
        commitExpectSystemException();

        InOrder ordered = Mockito.inOrder(transactionStore, resourceOne);
        verifySetup(ordered);

        // Commit (commit)
        ordered.verify(transactionStore).committing(globalXid);
        ordered.verify(transactionStore).committing(branchXidOne, "resourceOne");
        ordered.verify(resourceOne).end(branchXidOne, XAResource.TMSUCCESS);
        ordered.verify(resourceOne).commit(branchXidOne, true);
        ordered.verify(transactionStore).committed(branchXidOne, "resourceOne");
        ordered.verify(transactionStore).committed(globalXid);

        ordered.verify(transactionStore).transactionCompleted(transaction);
        Mockito.verifyNoMoreInteractions(transactionStore, resourceOne);
    }

    @Test
    public void testStoreCommittedFailure() throws Exception {
        Mockito.doThrow(new JtaTransactionStoreException("Fail")).when(transactionStore).committed(globalXid);
        commitExpectSystemException();

        InOrder ordered = Mockito.inOrder(transactionStore, resourceOne);
        verifySetup(ordered);

        // Commit (commit)
        ordered.verify(transactionStore).committing(globalXid);
        ordered.verify(transactionStore).committing(branchXidOne, "resourceOne");
        ordered.verify(resourceOne).end(branchXidOne, XAResource.TMSUCCESS);
        ordered.verify(resourceOne).commit(branchXidOne, true);
        ordered.verify(transactionStore).committed(branchXidOne, "resourceOne");
        ordered.verify(transactionStore).committed(globalXid);

        ordered.verify(transactionStore).transactionCompleted(transaction);
        Mockito.verifyNoMoreInteractions(transactionStore, resourceOne);
    }

    @Test
    public void testEndFailure() throws Exception {
        XAException failure = new XAException("Fail");
        Mockito.doThrow(failure).when(resourceOne).end(Mockito.any(), Mockito.eq(XAResource.TMSUCCESS));
        try {
            transactionManager.commit();
            Assert.fail("RollbackException expected");
        } catch (RollbackException e) {
            // Expected
        }

        InOrder ordered = Mockito.inOrder(transactionStore, resourceOne);
        verifySetup(ordered);

        // Commit (commit)
        ordered.verify(transactionStore).committing(globalXid);
        ordered.verify(transactionStore).committing(branchXidOne, "resourceOne");
        ordered.verify(resourceOne).end(branchXidOne, XAResource.TMSUCCESS);

        // Rollback
        ordered.verify(transactionStore).rollingBack(globalXid);
        ordered.verify(transactionStore).rollingBack(branchXidOne, "resourceOne");
        ordered.verify(resourceOne).end(branchXidOne, XAResource.TMFAIL);
        ordered.verify(resourceOne).rollback(branchXidOne);
        ordered.verify(transactionStore).rolledBack(branchXidOne, "resourceOne");
        ordered.verify(transactionStore).rolledBack(globalXid);

        ordered.verify(transactionStore).transactionCompleted(transaction);
        Mockito.verifyNoMoreInteractions(transactionStore, resourceOne);
    }

    @Test
    public void testCommitFailure() throws Exception {
        XAException failure = new XAException("Fail");
        Mockito.doThrow(failure).when(resourceOne).commit(Mockito.any(), Mockito.eq(true));
        try {
            transactionManager.commit();
            Assert.fail("RollbackException expected");
        } catch (RollbackException e) {
            // Expected
        }

        InOrder ordered = Mockito.inOrder(transactionStore, resourceOne);
        verifySetup(ordered);

        // Commit (commit)
        ordered.verify(transactionStore).committing(globalXid);
        ordered.verify(transactionStore).committing(branchXidOne, "resourceOne");
        ordered.verify(resourceOne).end(branchXidOne, XAResource.TMSUCCESS);
        ordered.verify(resourceOne).commit(branchXidOne, true);

        // Rollback
        ordered.verify(transactionStore).rollingBack(globalXid);
        ordered.verify(transactionStore).rollingBack(branchXidOne, "resourceOne");
        ordered.verify(resourceOne).rollback(branchXidOne);
        ordered.verify(transactionStore).rolledBack(branchXidOne, "resourceOne");
        ordered.verify(transactionStore).rolledBack(globalXid);

        ordered.verify(transactionStore).transactionCompleted(transaction);
        Mockito.verifyNoMoreInteractions(transactionStore, resourceOne);
    }
}
