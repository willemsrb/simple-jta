package nl.futureedge.simple.jta;

import javax.transaction.Status;
import javax.transaction.SystemException;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import nl.futureedge.simple.jta.store.JtaTransactionStore;
import nl.futureedge.simple.jta.store.JtaTransactionStoreException;
import nl.futureedge.simple.jta.xa.XAResourceAdapter;
import nl.futureedge.simple.jta.xid.BranchJtaXid;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mockito;

public class JtaTransactionEnlistTest {

    private XAResource resourceOne;
    private XAResource resourceTwo;
    private XAResource resourceThree;

    private JtaTransactionStore transactionStore;
    private JtaTransactionManager transactionManager;
    private JtaTransaction transaction;

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

        resourceOne = Mockito.mock(XAResource.class);
        resourceTwo = Mockito.mock(XAResource.class);
        resourceThree = Mockito.mock(XAResource.class);
    }

    private void verifySetup(InOrder ordered) throws JtaTransactionStoreException, XAException {
        // Startup manager
        ordered.verify(transactionStore).cleanup();

        // Start transaction
        ordered.verify(transactionStore).nextTransactionId();
        ordered.verify(transactionStore).active(Mockito.any());
    }

    @After
    public void destroy() throws Exception {
        transactionManager.destroy();
    }

    @Test
    public void testOk() throws Exception {
        transaction.enlistResource(new XAResourceAdapter("resourceOne", true, false, resourceOne));
        transaction.enlistResource(new XAResourceAdapter("resourceTwo", true, false, resourceTwo));
        transaction.enlistResource(new XAResourceAdapter("resourceThree", true, false, resourceThree));

        InOrder ordered = Mockito.inOrder(transactionStore, resourceOne, resourceTwo, resourceThree);
        verifySetup(ordered);

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

        Mockito.verifyNoMoreInteractions(transactionStore, resourceOne, resourceTwo, resourceThree);
    }

    @Test
    public void testWithTimeoutOk() throws Exception {
        transaction.setTransactionTimeout(30);
        transaction.enlistResource(new XAResourceAdapter("resourceOne", true, false, resourceOne));
        transaction.enlistResource(new XAResourceAdapter("resourceTwo", true, false, resourceTwo));
        transaction.enlistResource(new XAResourceAdapter("resourceThree", true, false, resourceThree));

        InOrder ordered = Mockito.inOrder(transactionStore, resourceOne, resourceTwo, resourceThree);
        verifySetup(ordered);

        // Enlist resource
        final ArgumentCaptor<BranchJtaXid> branchXidOneCaptor = ArgumentCaptor.forClass(BranchJtaXid.class);
        ordered.verify(transactionStore).active(branchXidOneCaptor.capture(), Mockito.eq("resourceOne"));
        final BranchJtaXid branchXidOne = branchXidOneCaptor.getValue();
        ordered.verify(resourceOne).setTransactionTimeout(30);
        ordered.verify(resourceOne).start(branchXidOne, XAResource.TMNOFLAGS);

        ordered.verify(resourceOne).isSameRM(resourceTwo);
        final ArgumentCaptor<BranchJtaXid> branchXidTwoCaptor = ArgumentCaptor.forClass(BranchJtaXid.class);
        ordered.verify(transactionStore).active(branchXidTwoCaptor.capture(), Mockito.eq("resourceTwo"));
        final BranchJtaXid branchXidTwo = branchXidTwoCaptor.getValue();
        ordered.verify(resourceTwo).setTransactionTimeout(30);
        ordered.verify(resourceTwo).start(branchXidTwo, XAResource.TMNOFLAGS);

        ordered.verify(resourceOne).isSameRM(resourceThree);
        ordered.verify(resourceTwo).isSameRM(resourceThree);
        final ArgumentCaptor<BranchJtaXid> branchXidThreeCaptor = ArgumentCaptor.forClass(BranchJtaXid.class);
        ordered.verify(transactionStore).active(branchXidThreeCaptor.capture(), Mockito.eq("resourceThree"));
        final BranchJtaXid branchXidThree = branchXidThreeCaptor.getValue();
        ordered.verify(resourceThree).setTransactionTimeout(30);
        ordered.verify(resourceThree).start(branchXidThree, XAResource.TMNOFLAGS);

        Mockito.verifyNoMoreInteractions(transactionStore, resourceOne, resourceTwo, resourceThree);
    }

    @Test
    public void testStoreActiveResourceFailure() throws Exception {
        Mockito.doThrow(new JtaTransactionStoreException("Fail")).when(transactionStore).active(Mockito.any(), Mockito.eq("resourceOne"));

        try {
            transaction.enlistResource(new XAResourceAdapter("resourceOne", true, false, resourceOne));
            Assert.fail("SystemException expected");
        } catch (SystemException e) {
            // Expected
        }

        InOrder ordered = Mockito.inOrder(transactionStore, resourceOne, resourceTwo, resourceThree);
        verifySetup(ordered);

        // Enlist resource
        ordered.verify(transactionStore).active(Mockito.any(), Mockito.eq("resourceOne"));

        Mockito.verifyNoMoreInteractions(transactionStore, resourceOne, resourceTwo, resourceThree);
    }

    @Test
    public void testSetTransactionTimeoutFailure() throws Exception {
        Mockito.doThrow(new XAException("Fail")).when(resourceOne).setTransactionTimeout(30);

        try {
            transaction.setTransactionTimeout(30);
            transaction.enlistResource(new XAResourceAdapter("resourceOne", true, false, resourceOne));
            Assert.fail("SystemException expected");
        } catch (SystemException e) {
            // Expected
        }

        InOrder ordered = Mockito.inOrder(transactionStore, resourceOne, resourceTwo, resourceThree);
        verifySetup(ordered);

        // Enlist resource
        ordered.verify(transactionStore).active(Mockito.any(), Mockito.eq("resourceOne"));
        ordered.verify(resourceOne).setTransactionTimeout(30);

        Mockito.verifyNoMoreInteractions(transactionStore, resourceOne, resourceTwo, resourceThree);
    }

    @Test
    public void testStartFailure() throws Exception {
        Mockito.doThrow(new XAException("Fail")).when(resourceOne).start(Mockito.any(), Mockito.eq(XAResource.TMNOFLAGS));

        try {
            transaction.enlistResource(new XAResourceAdapter("resourceOne", true, false, resourceOne));
            Assert.fail("SystemException expected");
        } catch (SystemException e) {
            // Expected
        }

        InOrder ordered = Mockito.inOrder(transactionStore, resourceOne, resourceTwo, resourceThree);
        verifySetup(ordered);

        // Enlist resource
        final ArgumentCaptor<BranchJtaXid> branchXidOneCaptor = ArgumentCaptor.forClass(BranchJtaXid.class);
        ordered.verify(transactionStore).active(branchXidOneCaptor.capture(), Mockito.eq("resourceOne"));
        final BranchJtaXid branchXidOne = branchXidOneCaptor.getValue();
        ordered.verify(resourceOne).start(branchXidOne, XAResource.TMNOFLAGS);

        Mockito.verifyNoMoreInteractions(transactionStore, resourceOne, resourceTwo, resourceThree);
    }

    @Test
    public void testJoinOk() throws Exception {
        Mockito.doReturn(true).when(resourceOne).isSameRM(resourceTwo);

        transaction.enlistResource(new XAResourceAdapter("resourceOne", true, false, resourceOne));
        transaction.enlistResource(new XAResourceAdapter("resourceTwo", true, false, resourceTwo));
        transaction.enlistResource(new XAResourceAdapter("resourceThree", true, false, resourceThree));

        InOrder ordered = Mockito.inOrder(transactionStore, resourceOne, resourceTwo, resourceThree);
        verifySetup(ordered);

        // Enlist resource

        // Enlist resource
        final ArgumentCaptor<BranchJtaXid> branchXidOneCaptor = ArgumentCaptor.forClass(BranchJtaXid.class);
        ordered.verify(transactionStore).active(branchXidOneCaptor.capture(), Mockito.eq("resourceOne"));
        final BranchJtaXid branchXidOne = branchXidOneCaptor.getValue();
        ordered.verify(resourceOne).start(branchXidOne, XAResource.TMNOFLAGS);

        ordered.verify(resourceOne).isSameRM(resourceTwo);
        ordered.verify(resourceTwo).start(Mockito.any(), Mockito.eq(XAResource.TMJOIN));

        ordered.verify(resourceOne).isSameRM(resourceThree);
        final ArgumentCaptor<BranchJtaXid> branchXidThreeCaptor = ArgumentCaptor.forClass(BranchJtaXid.class);
        ordered.verify(transactionStore).active(branchXidThreeCaptor.capture(), Mockito.eq("resourceThree"));
        final BranchJtaXid branchXidThree = branchXidThreeCaptor.getValue();
        ordered.verify(resourceThree).start(branchXidThree, XAResource.TMNOFLAGS);

        Mockito.verifyNoMoreInteractions(transactionStore, resourceOne, resourceTwo, resourceThree);
    }

    @Test
    public void testJoinWithTimeoutOk() throws Exception {
        Mockito.doReturn(true).when(resourceOne).isSameRM(resourceTwo);

        transaction.setTransactionTimeout(30);
        transaction.enlistResource(new XAResourceAdapter("resourceOne", true, false, resourceOne));
        transaction.enlistResource(new XAResourceAdapter("resourceTwo", true, false, resourceTwo));
        transaction.enlistResource(new XAResourceAdapter("resourceThree", true, false, resourceThree));

        InOrder ordered = Mockito.inOrder(transactionStore, resourceOne, resourceTwo, resourceThree);
        verifySetup(ordered);

        // Enlist resource
        final ArgumentCaptor<BranchJtaXid> branchXidOneCaptor = ArgumentCaptor.forClass(BranchJtaXid.class);
        ordered.verify(transactionStore).active(branchXidOneCaptor.capture(), Mockito.eq("resourceOne"));
        final BranchJtaXid branchXidOne = branchXidOneCaptor.getValue();
        ordered.verify(resourceOne).setTransactionTimeout(30);
        ordered.verify(resourceOne).start(branchXidOne, XAResource.TMNOFLAGS);

        ordered.verify(resourceOne).isSameRM(resourceTwo);
        ordered.verify(resourceTwo).setTransactionTimeout(30);
        ordered.verify(resourceTwo).start(Mockito.any(), Mockito.eq(XAResource.TMJOIN));

        ordered.verify(resourceOne).isSameRM(resourceThree);
        final ArgumentCaptor<BranchJtaXid> branchXidThreeCaptor = ArgumentCaptor.forClass(BranchJtaXid.class);
        ordered.verify(transactionStore).active(branchXidThreeCaptor.capture(), Mockito.eq("resourceThree"));
        final BranchJtaXid branchXidThree = branchXidThreeCaptor.getValue();
        ordered.verify(resourceThree).setTransactionTimeout(30);
        ordered.verify(resourceThree).start(branchXidThree, XAResource.TMNOFLAGS);

        Mockito.verifyNoMoreInteractions(transactionStore, resourceOne, resourceTwo, resourceThree);
    }

    @Test
    public void testJoinSetTransactionTimeoutFailure() throws Exception {
        Mockito.doReturn(true).when(resourceOne).isSameRM(resourceTwo);
        Mockito.doThrow(new XAException("Fail")).when(resourceTwo).setTransactionTimeout(30);

        transaction.setTransactionTimeout(30);
        transaction.enlistResource(new XAResourceAdapter("resourceOne", true, false, resourceOne));
        try {
            transaction.enlistResource(new XAResourceAdapter("resourceTwo", true, false, resourceTwo));
            Assert.fail("SystemException expected");
        } catch (SystemException e) {
            // Expected
        }

        InOrder ordered = Mockito.inOrder(transactionStore, resourceOne, resourceTwo, resourceThree);
        verifySetup(ordered);

        // Enlist resource
        final ArgumentCaptor<BranchJtaXid> branchXidOneCaptor = ArgumentCaptor.forClass(BranchJtaXid.class);
        ordered.verify(transactionStore).active(branchXidOneCaptor.capture(), Mockito.eq("resourceOne"));
        final BranchJtaXid branchXidOne = branchXidOneCaptor.getValue();
        ordered.verify(resourceOne).setTransactionTimeout(30);
        ordered.verify(resourceOne).start(branchXidOne, XAResource.TMNOFLAGS);

        ordered.verify(resourceOne).isSameRM(resourceTwo);
        ordered.verify(resourceTwo).setTransactionTimeout(30);

        Mockito.verifyNoMoreInteractions(transactionStore, resourceOne, resourceTwo, resourceThree);
    }


    @Test
    public void testJoinStartFailure() throws Exception {
        Mockito.doReturn(true).when(resourceOne).isSameRM(resourceTwo);
        Mockito.doThrow(new XAException("Fail")).when(resourceTwo).start(Mockito.any(), Mockito.eq(XAResource.TMJOIN));

        transaction.enlistResource(new XAResourceAdapter("resourceOne", true, false, resourceOne));
        try {
            transaction.enlistResource(new XAResourceAdapter("resourceTwo", true, false, resourceTwo));
            Assert.fail("SystemException expected");
        } catch (SystemException e) {
            // Expected
        }

        InOrder ordered = Mockito.inOrder(transactionStore, resourceOne, resourceTwo, resourceThree);
        verifySetup(ordered);

        // Enlist resource
        final ArgumentCaptor<BranchJtaXid> branchXidOneCaptor = ArgumentCaptor.forClass(BranchJtaXid.class);
        ordered.verify(transactionStore).active(branchXidOneCaptor.capture(), Mockito.eq("resourceOne"));
        final BranchJtaXid branchXidOne = branchXidOneCaptor.getValue();
        ordered.verify(resourceOne).start(branchXidOne, XAResource.TMNOFLAGS);

        ordered.verify(resourceOne).isSameRM(resourceTwo);
        ordered.verify(resourceTwo).start(Mockito.any(), Mockito.eq(XAResource.TMJOIN));

        Mockito.verifyNoMoreInteractions(transactionStore, resourceOne, resourceTwo, resourceThree);
    }
}
