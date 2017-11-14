package nl.futureedge.simple.jta;

import javax.transaction.InvalidTransactionException;
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

public class JtaTransactionSuspendTest {

    private XAResource resourceOne;
    private BranchJtaXid branchXidOne;
    private XAResource resourceTwo;
    private BranchJtaXid branchXidTwo;
    private XAResource resourceThree;
    private BranchJtaXid branchXidThree;

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
        resourceThree = Mockito.mock(XAResource.class);

        transaction.enlistResource(new XAResourceAdapter("resourceOne", false, false, resourceOne));
        transaction.enlistResource(new XAResourceAdapter("resourceTwo", false, true, resourceTwo));
        transaction.enlistResource(new XAResourceAdapter("resourceThree", false, false, resourceThree));
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

        final ArgumentCaptor<BranchJtaXid> branchXidTwoCaptor = ArgumentCaptor.forClass(BranchJtaXid.class);
        ordered.verify(transactionStore).active(branchXidTwoCaptor.capture(), Mockito.eq("resourceTwo"));
        branchXidTwo = branchXidTwoCaptor.getValue();
        ordered.verify(resourceTwo).start(branchXidTwo, XAResource.TMNOFLAGS);

        final ArgumentCaptor<BranchJtaXid> branchXidThreeCaptor = ArgumentCaptor.forClass(BranchJtaXid.class);
        ordered.verify(transactionStore).active(branchXidThreeCaptor.capture(), Mockito.eq("resourceThree"));
        branchXidThree = branchXidThreeCaptor.getValue();
        ordered.verify(resourceThree).start(branchXidThree, XAResource.TMNOFLAGS);
    }

    @After
    public void destroy() throws Exception {
        transactionManager.destroy();
    }

    @Test
    public void testOk() throws Exception {
        Assert.assertEquals(Status.STATUS_ACTIVE, transaction.getStatus());
        final InOrder ordered = Mockito.inOrder(transactionStore, resourceOne, resourceTwo, resourceThree);
        verifySetup(ordered);
        Mockito.verifyNoMoreInteractions(transactionStore, resourceOne, resourceTwo, resourceThree);

        // Suspend
        transaction.suspend();

        Assert.assertEquals(Status.STATUS_NO_TRANSACTION, transaction.getStatus());
        ordered.verify(resourceTwo).end(branchXidTwo, XAResource.TMSUSPEND);
        Mockito.verifyNoMoreInteractions(transactionStore, resourceOne, resourceTwo, resourceThree);

        // Resume
        transaction.resume();

        Assert.assertEquals(Status.STATUS_ACTIVE, transaction.getStatus());
        ordered.verify(resourceTwo).start(branchXidTwo, XAResource.TMRESUME);
        Mockito.verifyNoMoreInteractions(transactionStore, resourceOne, resourceTwo, resourceThree);
    }

    @Test
    public void testSuspendFailure() throws Exception {
        final InOrder ordered = Mockito.inOrder(transactionStore, resourceOne, resourceTwo, resourceThree);
        verifySetup(ordered);
        Mockito.verifyNoMoreInteractions(transactionStore, resourceOne, resourceTwo, resourceThree);

        Mockito.doThrow(new XAException("Test")).when(resourceTwo).end(branchXidTwo, XAResource.TMSUSPEND);

        // Suspend
        try {
            transaction.suspend();
            Assert.fail("SystemException expected");
        } catch (SystemException e) {
            // Expected
        }

        Assert.assertEquals(Status.STATUS_MARKED_ROLLBACK, transaction.getStatus());
        ordered.verify(resourceTwo).end(branchXidTwo, XAResource.TMSUSPEND);
        Mockito.verifyNoMoreInteractions(transactionStore, resourceOne, resourceTwo, resourceThree);

        try {
            transaction.resume();
            Assert.fail("InvalidTransactionException expected");
        } catch (InvalidTransactionException e) {
            // Expected
        }
        Mockito.verifyNoMoreInteractions(transactionStore, resourceOne, resourceTwo, resourceThree);
    }

    @Test
    public void testResumeFailed() throws Exception {
        Assert.assertEquals(Status.STATUS_ACTIVE, transaction.getStatus());
        final InOrder ordered = Mockito.inOrder(transactionStore, resourceOne, resourceTwo, resourceThree);
        verifySetup(ordered);
        Mockito.verifyNoMoreInteractions(transactionStore, resourceOne, resourceTwo, resourceThree);

        // Suspend
        transaction.suspend();

        Assert.assertEquals(Status.STATUS_NO_TRANSACTION, transaction.getStatus());
        ordered.verify(resourceTwo).end(branchXidTwo, XAResource.TMSUSPEND);
        Mockito.verifyNoMoreInteractions(transactionStore, resourceOne, resourceTwo, resourceThree);

        // Resume
        Mockito.doThrow(new XAException("Test")).when(resourceTwo).start(branchXidTwo, XAResource.TMRESUME);

        try {
            transaction.resume();
            Assert.fail("SystemException expected");
        } catch (SystemException e) {
            // Expected
        }

        Assert.assertEquals(Status.STATUS_MARKED_ROLLBACK, transaction.getStatus());
        ordered.verify(resourceTwo).start(branchXidTwo, XAResource.TMRESUME);
        Mockito.verifyNoMoreInteractions(transactionStore, resourceOne, resourceTwo, resourceThree);
    }
}
