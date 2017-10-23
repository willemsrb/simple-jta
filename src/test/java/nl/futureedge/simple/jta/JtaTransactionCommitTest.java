package nl.futureedge.simple.jta;

import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.SystemException;
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

public class JtaTransactionCommitTest {

    private XAResource resourceOne;
    private JtaXid branchXidOne;
    private XAResource resourceTwo;
    private JtaXid branchXidTwo;
    private XAResource resourceThree;
    private JtaXid branchXidThree;

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
        resourceTwo = Mockito.mock(XAResource.class);
        resourceThree = Mockito.mock(XAResource.class);

        transaction.enlistResource(new XAResourceAdapter("resourceOne", true, resourceOne));
        transaction.enlistResource(new XAResourceAdapter("resourceTwo", true, resourceTwo));
        transaction.enlistResource(new XAResourceAdapter("resourceThree", true, resourceThree));
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

        ordered.verify(resourceOne).isSameRM(resourceTwo);
        ordered.verify(transactionStore).active(globalXid, "resourceTwo");
        final ArgumentCaptor<JtaXid> branchXidTwoCaptor = ArgumentCaptor.forClass(JtaXid.class);
        ordered.verify(resourceTwo).start(branchXidTwoCaptor.capture(), Mockito.eq(XAResource.TMNOFLAGS));
        branchXidTwo = branchXidTwoCaptor.getValue();

        ordered.verify(resourceOne).isSameRM(resourceThree);
        ordered.verify(resourceTwo).isSameRM(resourceThree);
        ordered.verify(transactionStore).active(globalXid, "resourceThree");
        final ArgumentCaptor<JtaXid> branchXidThreeCaptor = ArgumentCaptor.forClass(JtaXid.class);
        ordered.verify(resourceThree).start(branchXidThreeCaptor.capture(), Mockito.eq(XAResource.TMNOFLAGS));
        branchXidThree = branchXidThreeCaptor.getValue();

        // Commit (prepare)
        ordered.verify(transactionStore).preparing(globalXid);
        ordered.verify(transactionStore).preparing(globalXid, "resourceOne");
        ordered.verify(resourceOne).end(branchXidOne, XAResource.TMSUCCESS);
        ordered.verify(resourceOne).prepare(Mockito.any());
        ordered.verify(transactionStore).prepared(globalXid, "resourceOne");

        ordered.verify(transactionStore).preparing(globalXid, "resourceTwo");
        ordered.verify(resourceTwo).end(branchXidTwo, XAResource.TMSUCCESS);
        ordered.verify(resourceTwo).prepare(Mockito.any());
        ordered.verify(transactionStore).prepared(globalXid, "resourceTwo");

        ordered.verify(transactionStore).preparing(globalXid, "resourceThree");
        ordered.verify(resourceThree).end(branchXidThree, XAResource.TMSUCCESS);
        ordered.verify(resourceThree).prepare(Mockito.any());
        ordered.verify(transactionStore).prepared(globalXid, "resourceThree");
        ordered.verify(transactionStore).prepared(globalXid);

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

        InOrder ordered = Mockito.inOrder(transactionStore, resourceOne, resourceTwo, resourceThree);
        verifySetup(ordered);

        // Commit (commit)
        ordered.verify(transactionStore).committing(globalXid);
        ordered.verify(transactionStore).committing(globalXid, "resourceOne");
        ordered.verify(resourceOne).commit(branchXidOne, false);
        ordered.verify(transactionStore).committed(globalXid, "resourceOne");
        ordered.verify(transactionStore).committing(globalXid, "resourceTwo");
        ordered.verify(resourceTwo).commit(branchXidTwo, false);
        ordered.verify(transactionStore).committed(globalXid, "resourceTwo");
        ordered.verify(transactionStore).committing(globalXid, "resourceThree");
        ordered.verify(resourceThree).commit(branchXidThree, false);
        ordered.verify(transactionStore).committed(globalXid, "resourceThree");
        ordered.verify(transactionStore).committed(globalXid);

        Mockito.verifyNoMoreInteractions(transactionStore, resourceOne, resourceTwo, resourceThree);
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

        InOrder ordered = Mockito.inOrder(transactionStore, resourceOne, resourceTwo, resourceThree);
        verifySetup(ordered);

        // Commit (commit)
        ordered.verify(transactionStore).committing(globalXid);

        // Rollback
        ordered.verify(transactionStore).rollingBack(globalXid);
        ordered.verify(transactionStore).rollingBack(globalXid, "resourceOne");
        ordered.verify(resourceOne).rollback(branchXidOne);
        ordered.verify(transactionStore).rolledBack(globalXid, "resourceOne");
        ordered.verify(transactionStore).rollingBack(globalXid, "resourceTwo");
        ordered.verify(resourceTwo).rollback(branchXidTwo);
        ordered.verify(transactionStore).rolledBack(globalXid, "resourceTwo");
        ordered.verify(transactionStore).rollingBack(globalXid, "resourceThree");
        ordered.verify(resourceThree).rollback(branchXidThree);
        ordered.verify(transactionStore).rolledBack(globalXid, "resourceThree");
        ordered.verify(transactionStore).rolledBack(globalXid);

        Mockito.verifyNoMoreInteractions(transactionStore, resourceOne, resourceTwo, resourceThree);
    }

    @Test
    public void testStoreCommittingResourceFailure() throws Exception {
        Mockito.doThrow(new JtaTransactionStoreException("Fail")).when(transactionStore).committing(globalXid, "resourceTwo");
        commitExpectSystemException();

        InOrder ordered = Mockito.inOrder(transactionStore, resourceOne, resourceTwo, resourceThree);
        verifySetup(ordered);

        // Commit (commit)
        ordered.verify(transactionStore).committing(globalXid);
        ordered.verify(transactionStore).committing(globalXid, "resourceOne");
        ordered.verify(resourceOne).commit(branchXidOne, false);
        ordered.verify(transactionStore).committed(globalXid, "resourceOne");
        ordered.verify(transactionStore).committing(globalXid, "resourceTwo");
        ordered.verify(resourceTwo).commit(branchXidTwo, false);
        ordered.verify(transactionStore).committed(globalXid, "resourceTwo");
        ordered.verify(transactionStore).committing(globalXid, "resourceThree");
        ordered.verify(resourceThree).commit(branchXidThree, false);
        ordered.verify(transactionStore).committed(globalXid, "resourceThree");
        ordered.verify(transactionStore).committed(globalXid);

        Mockito.verifyNoMoreInteractions(transactionStore, resourceOne, resourceTwo, resourceThree);
    }

    @Test
    public void testStoreCommittedResourceFailure() throws Exception {
        Mockito.doThrow(new JtaTransactionStoreException("Fail")).when(transactionStore).committed(globalXid, "resourceTwo");
        commitExpectSystemException();

        InOrder ordered = Mockito.inOrder(transactionStore, resourceOne, resourceTwo, resourceThree);
        verifySetup(ordered);

        // Commit (commit)
        ordered.verify(transactionStore).committing(globalXid);
        ordered.verify(transactionStore).committing(globalXid, "resourceOne");
        ordered.verify(resourceOne).commit(branchXidOne, false);
        ordered.verify(transactionStore).committed(globalXid, "resourceOne");
        ordered.verify(transactionStore).committing(globalXid, "resourceTwo");
        ordered.verify(resourceTwo).commit(branchXidTwo, false);
        ordered.verify(transactionStore).committed(globalXid, "resourceTwo");
        ordered.verify(transactionStore).committing(globalXid, "resourceThree");
        ordered.verify(resourceThree).commit(branchXidThree, false);
        ordered.verify(transactionStore).committed(globalXid, "resourceThree");
        ordered.verify(transactionStore).committed(globalXid);

        Mockito.verifyNoMoreInteractions(transactionStore, resourceOne, resourceTwo, resourceThree);
    }

    @Test
    public void testStoreCommittedFailure() throws Exception {
        Mockito.doThrow(new JtaTransactionStoreException("Fail")).when(transactionStore).committed(globalXid);
        commitExpectSystemException();

        InOrder ordered = Mockito.inOrder(transactionStore, resourceOne, resourceTwo, resourceThree);
        verifySetup(ordered);

        // Commit (commit)
        ordered.verify(transactionStore).committing(globalXid);
        ordered.verify(transactionStore).committing(globalXid, "resourceOne");
        ordered.verify(resourceOne).commit(branchXidOne, false);
        ordered.verify(transactionStore).committed(globalXid, "resourceOne");
        ordered.verify(transactionStore).committing(globalXid, "resourceTwo");
        ordered.verify(resourceTwo).commit(branchXidTwo, false);
        ordered.verify(transactionStore).committed(globalXid, "resourceTwo");
        ordered.verify(transactionStore).committing(globalXid, "resourceThree");
        ordered.verify(resourceThree).commit(branchXidThree, false);
        ordered.verify(transactionStore).committed(globalXid, "resourceThree");
        ordered.verify(transactionStore).committed(globalXid);

        Mockito.verifyNoMoreInteractions(transactionStore, resourceOne, resourceTwo, resourceThree);
    }

    @Test
    public void testCommitFailure() throws Exception {
        XAException failure = new XAException("Fail");
        Mockito.doThrow(failure).when(resourceTwo).commit(Mockito.any(), Mockito.eq(false));
        commitExpectSystemException();

        InOrder ordered = Mockito.inOrder(transactionStore, resourceOne, resourceTwo, resourceThree);
        verifySetup(ordered);

        // Commit (commit)
        ordered.verify(transactionStore).committing(globalXid);
        ordered.verify(transactionStore).committing(globalXid, "resourceOne");
        ordered.verify(resourceOne).commit(branchXidOne, false);
        ordered.verify(transactionStore).committed(globalXid, "resourceOne");
        ordered.verify(transactionStore).committing(globalXid, "resourceTwo");
        ordered.verify(resourceTwo).commit(branchXidTwo, false);
        ordered.verify(transactionStore).commitFailed(globalXid, "resourceTwo", failure);
        ordered.verify(transactionStore).committing(globalXid, "resourceThree");
        ordered.verify(resourceThree).commit(branchXidThree, false);
        ordered.verify(transactionStore).committed(globalXid, "resourceThree");
        ordered.verify(transactionStore).commitFailed(globalXid);

        Mockito.verifyNoMoreInteractions(transactionStore, resourceOne, resourceTwo, resourceThree);
    }

    @Test
    public void testStoreCommitFailedResourceFailure() throws Exception {
        XAException failure = new XAException("Fail");
        Mockito.doThrow(failure).when(resourceTwo).commit(Mockito.any(), Mockito.eq(false));
        Mockito.doThrow(new JtaTransactionStoreException("Fail")).when(transactionStore).commitFailed(globalXid, "resourceTwo", failure);
        commitExpectSystemException();

        InOrder ordered = Mockito.inOrder(transactionStore, resourceOne, resourceTwo, resourceThree);
        verifySetup(ordered);

        // Commit (commit)
        ordered.verify(transactionStore).committing(globalXid);
        ordered.verify(transactionStore).committing(globalXid, "resourceOne");
        ordered.verify(resourceOne).commit(branchXidOne, false);
        ordered.verify(transactionStore).committed(globalXid, "resourceOne");
        ordered.verify(transactionStore).committing(globalXid, "resourceTwo");
        ordered.verify(resourceTwo).commit(branchXidTwo, false);
        ordered.verify(transactionStore).commitFailed(globalXid, "resourceTwo", failure);
        ordered.verify(transactionStore).committing(globalXid, "resourceThree");
        ordered.verify(resourceThree).commit(branchXidThree, false);
        ordered.verify(transactionStore).committed(globalXid, "resourceThree");
        ordered.verify(transactionStore).commitFailed(globalXid);

        Mockito.verifyNoMoreInteractions(transactionStore, resourceOne, resourceTwo, resourceThree);
    }


    @Test
    public void testStoreCommitFailedFailure() throws Exception {
        XAException failure = new XAException("Fail");
        Mockito.doThrow(failure).when(resourceTwo).commit(Mockito.any(), Mockito.eq(false));
        Mockito.doThrow(new JtaTransactionStoreException("Fail")).when(transactionStore).commitFailed(globalXid);
        commitExpectSystemException();

        InOrder ordered = Mockito.inOrder(transactionStore, resourceOne, resourceTwo, resourceThree);
        verifySetup(ordered);

        // Commit (commit)
        ordered.verify(transactionStore).committing(globalXid);
        ordered.verify(transactionStore).committing(globalXid, "resourceOne");
        ordered.verify(resourceOne).commit(branchXidOne, false);
        ordered.verify(transactionStore).committed(globalXid, "resourceOne");
        ordered.verify(transactionStore).committing(globalXid, "resourceTwo");
        ordered.verify(resourceTwo).commit(branchXidTwo, false);
        ordered.verify(transactionStore).commitFailed(globalXid, "resourceTwo", failure);
        ordered.verify(transactionStore).committing(globalXid, "resourceThree");
        ordered.verify(resourceThree).commit(branchXidThree, false);
        ordered.verify(transactionStore).committed(globalXid, "resourceThree");
        ordered.verify(transactionStore).commitFailed(globalXid);

        Mockito.verifyNoMoreInteractions(transactionStore, resourceOne, resourceTwo, resourceThree);
    }
}
