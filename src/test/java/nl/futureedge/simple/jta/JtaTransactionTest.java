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
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mockito;

public class JtaTransactionTest {

    private XAResource resourceOne;
    private XAResource resourceTwo;
    private XAResource resourceThree;

    private JtaTransactionStore transactionStore;
    private JtaTransactionManager transactionManager;
    private JtaTransaction transaction;
    private GlobalJtaXid globalXid;

    @Before
    public void setup() throws Exception {
        transactionStore = Mockito.mock(JtaTransactionStore.class);
        transactionManager = new JtaTransactionManager();
        transactionManager.setUniqueName(this.getClass().getSimpleName());
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

    @Test
    public void testActiveFail() throws Exception {
        final GlobalJtaXid xid2 = new GlobalJtaXid("Test", 2L);
        Mockito.doThrow(new JtaTransactionStoreException("Test")).when(transactionStore).active(xid2);
        try {
            new JtaTransaction(xid2, null, transactionStore);
            Assert.fail("SystemException expected");
        } catch (SystemException e) {
            // Expected
        }
    }

    @Test
    public void testCommit() throws Exception {
        XAResource resourceFour = Mockito.mock(XAResource.class);
        Mockito.when(resourceThree.isSameRM(resourceFour)).thenReturn(true);
        transaction.enlistResource(new XAResourceAdapter("resourceFour", true, resourceFour));

        Mockito.when(resourceTwo.prepare(Mockito.any())).thenReturn(XAResource.XA_RDONLY);
        transactionManager.commit();
        Assert.assertEquals(Status.STATUS_COMMITTED, transaction.getStatus());

        InOrder ordered = Mockito.inOrder(transactionStore, resourceOne, resourceTwo, resourceThree, resourceFour);

        // Startup manager
        ordered.verify(transactionStore).cleanup();

        // Start transaction
        ordered.verify(transactionStore).nextTransactionId();
        ordered.verify(transactionStore).active(globalXid);

        // Enlist resource
        final ArgumentCaptor<BranchJtaXid> branchXidOneCaptor = ArgumentCaptor.forClass(BranchJtaXid.class);
        ordered.verify(transactionStore).active(branchXidOneCaptor.capture(), Mockito.eq("resourceOne"));
        final BranchJtaXid branchXidOne = branchXidOneCaptor.getValue();
        ordered.verify(resourceOne).start(branchXidOne, XAResource.TMNOFLAGS);

        ordered.verify(resourceOne).isSameRM(resourceTwo);
        final ArgumentCaptor<BranchJtaXid> branchXidTwoCaptor = ArgumentCaptor.forClass(BranchJtaXid.class);
        ordered.verify(transactionStore).active(branchXidTwoCaptor.capture(), Mockito.eq("resourceTwo"));
        final BranchJtaXid branchXidTwo = branchXidTwoCaptor.getValue();
        ordered.verify(resourceTwo).start(branchXidTwo, XAResource.TMNOFLAGS);

        ordered.verify(resourceOne).isSameRM(resourceThree);
        ordered.verify(resourceTwo).isSameRM(resourceThree);
        final ArgumentCaptor<BranchJtaXid> branchXidThreeCaptor = ArgumentCaptor.forClass(BranchJtaXid.class);
        ordered.verify(transactionStore).active(branchXidThreeCaptor.capture(), Mockito.eq("resourceThree"));
        final BranchJtaXid branchXidThree = branchXidThreeCaptor.getValue();
        ordered.verify(resourceThree).start(branchXidThree, XAResource.TMNOFLAGS);

        ordered.verify(resourceOne).isSameRM(resourceFour);
        ordered.verify(resourceTwo).isSameRM(resourceFour);
        ordered.verify(resourceThree).isSameRM(resourceFour);
        ordered.verify(resourceFour).start(Mockito.any(), Mockito.eq(XAResource.TMJOIN));

        // Commit (prepare)
        ordered.verify(transactionStore).preparing(globalXid);
        ordered.verify(transactionStore).preparing(branchXidOne, "resourceOne");
        ordered.verify(resourceOne).end(branchXidOne, XAResource.TMSUCCESS);
        ordered.verify(resourceOne).prepare(Mockito.any());
        ordered.verify(transactionStore).prepared(branchXidOne, "resourceOne");
        ordered.verify(transactionStore).preparing(branchXidTwo, "resourceTwo");
        ordered.verify(resourceTwo).end(branchXidTwo, XAResource.TMSUCCESS);
        ordered.verify(resourceTwo).prepare(Mockito.any());
        ordered.verify(transactionStore).committed(branchXidTwo, "resourceTwo");
        ordered.verify(transactionStore).preparing(branchXidThree, "resourceThree");
        ordered.verify(resourceThree).end(branchXidThree, XAResource.TMSUCCESS);
        ordered.verify(resourceThree).prepare(Mockito.any());
        ordered.verify(transactionStore).prepared(branchXidThree, "resourceThree");
        ordered.verify(transactionStore).prepared(globalXid);

        // Commit (commit)
        ordered.verify(transactionStore).committing(globalXid);
        ordered.verify(transactionStore).committing(branchXidOne, "resourceOne");
        ordered.verify(resourceOne).commit(branchXidOne, false);
        ordered.verify(transactionStore).committed(branchXidOne, "resourceOne");
        ordered.verify(transactionStore).committing(branchXidThree, "resourceThree");
        ordered.verify(resourceThree).commit(branchXidThree, false);
        ordered.verify(transactionStore).committed(branchXidThree, "resourceThree");
        ordered.verify(transactionStore).committed(globalXid);

        ordered.verify(transactionStore).transactionCompleted(transaction);
        Mockito.verifyNoMoreInteractions(transactionStore, resourceOne, resourceTwo, resourceThree, resourceFour);
    }

    @Test
    public void testPrepareFailure() throws Exception {
        Mockito.when(resourceTwo.prepare(Mockito.any())).thenThrow(new XAException("Commit failure"));

        try {
            transactionManager.commit();
            Assert.fail("RollbackException expected");
        } catch (RollbackException e) {
            // Expected
        }
        Assert.assertEquals(Status.STATUS_ROLLEDBACK, transaction.getStatus());

        InOrder ordered = Mockito.inOrder(transactionStore, resourceOne, resourceTwo, resourceThree);

        // Startup manager
        ordered.verify(transactionStore).cleanup();

        // Start transaction
        ordered.verify(transactionStore).nextTransactionId();
        ordered.verify(transactionStore).active(globalXid);

        // Enlist resource
        final ArgumentCaptor<BranchJtaXid> branchXidOneCaptor = ArgumentCaptor.forClass(BranchJtaXid.class);
        ordered.verify(transactionStore).active(branchXidOneCaptor.capture(), Mockito.eq("resourceOne"));
        final BranchJtaXid branchXidOne = branchXidOneCaptor.getValue();
        ordered.verify(resourceOne).start(branchXidOne, XAResource.TMNOFLAGS);

        ordered.verify(resourceOne).isSameRM(resourceTwo);
        final ArgumentCaptor<BranchJtaXid> branchXidTwoCaptor = ArgumentCaptor.forClass(BranchJtaXid.class);
        ordered.verify(transactionStore).active(branchXidTwoCaptor.capture(), Mockito.eq("resourceTwo"));
        final BranchJtaXid branchXidTwo = branchXidTwoCaptor.getValue();
        ordered.verify(resourceTwo).start(branchXidTwo, XAResource.TMNOFLAGS);

        ordered.verify(resourceOne).isSameRM(resourceThree);
        ordered.verify(resourceTwo).isSameRM(resourceThree);
        final ArgumentCaptor<BranchJtaXid> branchXidThreeCaptor = ArgumentCaptor.forClass(BranchJtaXid.class);
        ordered.verify(transactionStore).active(branchXidThreeCaptor.capture(), Mockito.eq("resourceThree"));
        final BranchJtaXid branchXidThree = branchXidThreeCaptor.getValue();
        ordered.verify(resourceThree).start(branchXidThree, XAResource.TMNOFLAGS);

        // Commit (prepare)
        ordered.verify(transactionStore).preparing(globalXid);
        ordered.verify(transactionStore).preparing(branchXidOne, "resourceOne");
        ordered.verify(resourceOne).end(branchXidOne, XAResource.TMSUCCESS);
        ordered.verify(resourceOne).prepare(Mockito.any());
        ordered.verify(transactionStore).prepared(branchXidOne, "resourceOne");
        ordered.verify(transactionStore).preparing(branchXidTwo, "resourceTwo");
        ordered.verify(resourceTwo).end(branchXidTwo, XAResource.TMSUCCESS);
        ordered.verify(resourceTwo).prepare(Mockito.any());

        // Rollback
        ordered.verify(transactionStore).rollingBack(globalXid);
        ordered.verify(transactionStore).rollingBack(branchXidOne, "resourceOne");
        ordered.verify(resourceOne).rollback(branchXidOne);
        ordered.verify(transactionStore).rolledBack(branchXidOne, "resourceOne");
        ordered.verify(transactionStore).rollingBack(branchXidTwo, "resourceTwo");
        ordered.verify(resourceTwo).rollback(branchXidTwo);
        ordered.verify(transactionStore).rolledBack(branchXidTwo, "resourceTwo");
        ordered.verify(transactionStore).rollingBack(branchXidThree, "resourceThree");
        ordered.verify(resourceThree).end(branchXidThree, XAResource.TMFAIL);
        ordered.verify(resourceThree).rollback(branchXidThree);
        ordered.verify(transactionStore).rolledBack(branchXidThree, "resourceThree");
        ordered.verify(transactionStore).rolledBack(globalXid);

        ordered.verify(transactionStore).transactionCompleted(transaction);
        Mockito.verifyNoMoreInteractions(transactionStore, resourceOne, resourceTwo, resourceThree);
    }


    @Test
    public void testTimeoutAndRollback() throws Exception {
        try {
            transactionManager.setTransactionTimeout(-1);
            Assert.fail("SystemException expected");
        } catch (SystemException e) {
            // Expected
        }

        Mockito.doThrow(new XAException("Fail")).when(resourceTwo).setTransactionTimeout(45);
        try {
            transactionManager.setTransactionTimeout(45);
            Assert.fail("SystemException expected");
        } catch (SystemException e) {
            // Expected
        }
        transactionManager.rollback();
        Assert.assertEquals(Status.STATUS_ROLLEDBACK, transaction.getStatus());

        try {
            transaction.rollback();
            Assert.fail("IllegalStateException expected");
        } catch (IllegalStateException e) {
            // Expected
        }

        InOrder ordered = Mockito.inOrder(transactionStore, resourceOne, resourceTwo, resourceThree);

        // Startup manager
        ordered.verify(transactionStore).cleanup();

        // Start transaction
        ordered.verify(transactionStore).nextTransactionId();
        ordered.verify(transactionStore).active(globalXid);

        // Enlist resource
        final ArgumentCaptor<BranchJtaXid> branchXidOneCaptor = ArgumentCaptor.forClass(BranchJtaXid.class);
        ordered.verify(transactionStore).active(branchXidOneCaptor.capture(), Mockito.eq("resourceOne"));
        final BranchJtaXid branchXidOne = branchXidOneCaptor.getValue();
        ordered.verify(resourceOne).start(branchXidOne, XAResource.TMNOFLAGS);

        ordered.verify(resourceOne).isSameRM(resourceTwo);
        final ArgumentCaptor<BranchJtaXid> branchXidTwoCaptor = ArgumentCaptor.forClass(BranchJtaXid.class);
        ordered.verify(transactionStore).active(branchXidTwoCaptor.capture(), Mockito.eq("resourceTwo"));
        final BranchJtaXid branchXidTwo = branchXidTwoCaptor.getValue();
        ordered.verify(resourceTwo).start(branchXidTwo, XAResource.TMNOFLAGS);

        ordered.verify(resourceOne).isSameRM(resourceThree);
        ordered.verify(resourceTwo).isSameRM(resourceThree);
        final ArgumentCaptor<BranchJtaXid> branchXidThreeCaptor = ArgumentCaptor.forClass(BranchJtaXid.class);
        ordered.verify(transactionStore).active(branchXidThreeCaptor.capture(), Mockito.eq("resourceThree"));
        final BranchJtaXid branchXidThree = branchXidThreeCaptor.getValue();
        ordered.verify(resourceThree).start(branchXidThree, XAResource.TMNOFLAGS);

        // Set timeout
        ordered.verify(resourceOne).setTransactionTimeout(45);
        ordered.verify(resourceTwo).setTransactionTimeout(45);

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
        ordered.verify(transactionStore).rollingBack(branchXidThree, "resourceThree");
        ordered.verify(resourceThree).end(branchXidThree, XAResource.TMFAIL);
        ordered.verify(resourceThree).rollback(branchXidThree);
        ordered.verify(transactionStore).rolledBack(branchXidThree, "resourceThree");
        ordered.verify(transactionStore).rolledBack(globalXid);

        ordered.verify(transactionStore).transactionCompleted(transaction);
        Mockito.verifyNoMoreInteractions(transactionStore, resourceOne, resourceTwo, resourceThree);
    }

    @Test
    public void testUnsupported() throws Exception {
        try {
            transaction.enlistResource(resourceOne);
            Assert.fail("UnsupportedOperationException expected");
        } catch (UnsupportedOperationException e) {
            // Expected
        }
        try {
            transaction.delistResource(resourceOne, XAResource.TMFAIL);
            Assert.fail("UnsupportedOperationException expected");
        } catch (UnsupportedOperationException e) {
            // Expected
        }
    }

    @Test
    public void testSetRollbackOnly() throws Exception {
        transaction.setRollbackOnly();
        try {
            transaction.commit();
            Assert.fail("RollbackException expected");
        } catch (RollbackException e) {
            // Expected
        }
        transaction.rollback();
        try {
            transaction.commit();
            Assert.fail("IllegalStateException expected");
        } catch (IllegalStateException e) {
            // Expected
        }
    }
}
