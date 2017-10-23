package nl.futureedge.simple.jta;

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
import org.mockito.InOrder;
import org.mockito.Mockito;

public class JtaTransactionEnlistTest {

    private XAResource resourceOne;
    private XAResource resourceTwo;
    private XAResource resourceThree;

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
        transaction.enlistResource(new XAResourceAdapter("resourceOne", true, resourceOne));
        transaction.enlistResource(new XAResourceAdapter("resourceTwo", true, resourceTwo));
        transaction.enlistResource(new XAResourceAdapter("resourceThree", true, resourceThree));

        InOrder ordered = Mockito.inOrder(transactionStore, resourceOne, resourceTwo, resourceThree);
        verifySetup(ordered);

        // Enlist resource
        ordered.verify(transactionStore).active(globalXid, "resourceOne");
        ordered.verify(resourceOne).start(Mockito.any(), Mockito.eq(XAResource.TMNOFLAGS));

        ordered.verify(resourceOne).isSameRM(resourceTwo);
        ordered.verify(transactionStore).active(globalXid, "resourceTwo");
        ordered.verify(resourceTwo).start(Mockito.any(), Mockito.eq(XAResource.TMNOFLAGS));

        ordered.verify(resourceOne).isSameRM(resourceThree);
        ordered.verify(resourceTwo).isSameRM(resourceThree);
        ordered.verify(transactionStore).active(globalXid, "resourceThree");
        ordered.verify(resourceThree).start(Mockito.any(), Mockito.eq(XAResource.TMNOFLAGS));

        Mockito.verifyNoMoreInteractions(transactionStore, resourceOne, resourceTwo, resourceThree);
    }

    @Test
    public void testWithTimeoutOk() throws Exception {
        transaction.setTransactionTimeout(30);
        transaction.enlistResource(new XAResourceAdapter("resourceOne", true, resourceOne));
        transaction.enlistResource(new XAResourceAdapter("resourceTwo", true, resourceTwo));
        transaction.enlistResource(new XAResourceAdapter("resourceThree", true, resourceThree));

        InOrder ordered = Mockito.inOrder(transactionStore, resourceOne, resourceTwo, resourceThree);
        verifySetup(ordered);

        // Enlist resource
        ordered.verify(transactionStore).active(globalXid, "resourceOne");
        ordered.verify(resourceOne).setTransactionTimeout(30);
        ordered.verify(resourceOne).start(Mockito.any(), Mockito.eq(XAResource.TMNOFLAGS));

        ordered.verify(resourceOne).isSameRM(resourceTwo);
        ordered.verify(transactionStore).active(globalXid, "resourceTwo");
        ordered.verify(resourceTwo).setTransactionTimeout(30);
        ordered.verify(resourceTwo).start(Mockito.any(), Mockito.eq(XAResource.TMNOFLAGS));

        ordered.verify(resourceOne).isSameRM(resourceThree);
        ordered.verify(resourceTwo).isSameRM(resourceThree);
        ordered.verify(transactionStore).active(globalXid, "resourceThree");
        ordered.verify(resourceThree).setTransactionTimeout(30);
        ordered.verify(resourceThree).start(Mockito.any(), Mockito.eq(XAResource.TMNOFLAGS));

        Mockito.verifyNoMoreInteractions(transactionStore, resourceOne, resourceTwo, resourceThree);
    }

    @Test
    public void testStoreActiveResourceFailure() throws Exception {
        Mockito.doThrow(new JtaTransactionStoreException("Fail")).when(transactionStore).active(globalXid, "resourceOne");

        try {
            transaction.enlistResource(new XAResourceAdapter("resourceOne", true, resourceOne));
            Assert.fail("SystemException expected");
        } catch (SystemException e) {
            // Expected
        }

        InOrder ordered = Mockito.inOrder(transactionStore, resourceOne, resourceTwo, resourceThree);
        verifySetup(ordered);

        // Enlist resource
        ordered.verify(transactionStore).active(globalXid, "resourceOne");

        Mockito.verifyNoMoreInteractions(transactionStore, resourceOne, resourceTwo, resourceThree);
    }

    @Test
    public void testSetTransactionTimeoutFailure() throws Exception {
        Mockito.doThrow(new XAException("Fail")).when(resourceOne).setTransactionTimeout(30);

        try {
            transaction.setTransactionTimeout(30);
            transaction.enlistResource(new XAResourceAdapter("resourceOne", true, resourceOne));
            Assert.fail("SystemException expected");
        } catch (SystemException e) {
            // Expected
        }

        InOrder ordered = Mockito.inOrder(transactionStore, resourceOne, resourceTwo, resourceThree);
        verifySetup(ordered);

        // Enlist resource
        ordered.verify(transactionStore).active(globalXid, "resourceOne");
        ordered.verify(resourceOne).setTransactionTimeout(30);

        Mockito.verifyNoMoreInteractions(transactionStore, resourceOne, resourceTwo, resourceThree);
    }

    @Test
    public void testStartFailure() throws Exception {
        Mockito.doThrow(new XAException("Fail")).when(resourceOne).start(Mockito.any(), Mockito.eq(XAResource.TMNOFLAGS));

        try {
            transaction.enlistResource(new XAResourceAdapter("resourceOne", true, resourceOne));
            Assert.fail("SystemException expected");
        } catch (SystemException e) {
            // Expected
        }

        InOrder ordered = Mockito.inOrder(transactionStore, resourceOne, resourceTwo, resourceThree);
        verifySetup(ordered);

        // Enlist resource
        ordered.verify(transactionStore).active(globalXid, "resourceOne");
        ordered.verify(resourceOne).start(Mockito.any(), Mockito.eq(XAResource.TMNOFLAGS));

        Mockito.verifyNoMoreInteractions(transactionStore, resourceOne, resourceTwo, resourceThree);
    }

    @Test
    public void testJoinOk() throws Exception {
        Mockito.doReturn(true).when(resourceOne).isSameRM(resourceTwo);

        transaction.enlistResource(new XAResourceAdapter("resourceOne", true, resourceOne));
        transaction.enlistResource(new XAResourceAdapter("resourceTwo", true, resourceTwo));
        transaction.enlistResource(new XAResourceAdapter("resourceThree", true, resourceThree));

        InOrder ordered = Mockito.inOrder(transactionStore, resourceOne, resourceTwo, resourceThree);
        verifySetup(ordered);

        // Enlist resource
        ordered.verify(transactionStore).active(globalXid, "resourceOne");
        ordered.verify(resourceOne).start(Mockito.any(), Mockito.eq(XAResource.TMNOFLAGS));

        ordered.verify(resourceOne).isSameRM(resourceTwo);
        ordered.verify(resourceTwo).start(Mockito.any(), Mockito.eq(XAResource.TMJOIN));

        ordered.verify(resourceOne).isSameRM(resourceThree);
        ordered.verify(transactionStore).active(globalXid, "resourceThree");
        ordered.verify(resourceThree).start(Mockito.any(), Mockito.eq(XAResource.TMNOFLAGS));

        Mockito.verifyNoMoreInteractions(transactionStore, resourceOne, resourceTwo, resourceThree);
    }

    @Test
    public void testJoinWithTimeoutOk() throws Exception {
        Mockito.doReturn(true).when(resourceOne).isSameRM(resourceTwo);

        transaction.setTransactionTimeout(30);
        transaction.enlistResource(new XAResourceAdapter("resourceOne", true, resourceOne));
        transaction.enlistResource(new XAResourceAdapter("resourceTwo", true, resourceTwo));
        transaction.enlistResource(new XAResourceAdapter("resourceThree", true, resourceThree));

        InOrder ordered = Mockito.inOrder(transactionStore, resourceOne, resourceTwo, resourceThree);
        verifySetup(ordered);

        // Enlist resource
        ordered.verify(transactionStore).active(globalXid, "resourceOne");
        ordered.verify(resourceOne).setTransactionTimeout(30);
        ordered.verify(resourceOne).start(Mockito.any(), Mockito.eq(XAResource.TMNOFLAGS));

        ordered.verify(resourceOne).isSameRM(resourceTwo);
        ordered.verify(resourceTwo).setTransactionTimeout(30);
        ordered.verify(resourceTwo).start(Mockito.any(), Mockito.eq(XAResource.TMJOIN));

        ordered.verify(resourceOne).isSameRM(resourceThree);
        ordered.verify(transactionStore).active(globalXid, "resourceThree");
        ordered.verify(resourceThree).setTransactionTimeout(30);
        ordered.verify(resourceThree).start(Mockito.any(),Mockito.eq( XAResource.TMNOFLAGS));

        Mockito.verifyNoMoreInteractions(transactionStore, resourceOne, resourceTwo, resourceThree);
    }

    @Test
    public void testJoinSetTransactionTimeoutFailure() throws Exception {
        Mockito.doReturn(true).when(resourceOne).isSameRM(resourceTwo);
        Mockito.doThrow(new XAException("Fail")).when(resourceTwo).setTransactionTimeout(30);

        transaction.setTransactionTimeout(30);
        transaction.enlistResource(new XAResourceAdapter("resourceOne", true, resourceOne));
        try {
            transaction.enlistResource(new XAResourceAdapter("resourceTwo", true, resourceTwo));
            Assert.fail("SystemException expected");
        } catch (SystemException e) {
            // Expected
        }

        InOrder ordered = Mockito.inOrder(transactionStore, resourceOne, resourceTwo, resourceThree);
        verifySetup(ordered);

        // Enlist resource
        ordered.verify(transactionStore).active(globalXid, "resourceOne");
        ordered.verify(resourceOne).setTransactionTimeout(30);
        ordered.verify(resourceOne).start(Mockito.any(), Mockito.eq(XAResource.TMNOFLAGS));

        ordered.verify(resourceOne).isSameRM(resourceTwo);
        ordered.verify(resourceTwo).setTransactionTimeout(30);

        Mockito.verifyNoMoreInteractions(transactionStore, resourceOne, resourceTwo, resourceThree);
    }


    @Test
    public void testJoinStartFailure() throws Exception {
        Mockito.doReturn(true).when(resourceOne).isSameRM(resourceTwo);
        Mockito.doThrow(new XAException("Fail")).when(resourceTwo).start(Mockito.any(), Mockito.eq(XAResource.TMJOIN));

        transaction.enlistResource(new XAResourceAdapter("resourceOne", true, resourceOne));
        try {
            transaction.enlistResource(new XAResourceAdapter("resourceTwo", true, resourceTwo));
            Assert.fail("SystemException expected");
        } catch (SystemException e) {
            // Expected
        }

        InOrder ordered = Mockito.inOrder(transactionStore, resourceOne, resourceTwo, resourceThree);
        verifySetup(ordered);

        // Enlist resource
        ordered.verify(transactionStore).active(globalXid, "resourceOne");
        ordered.verify(resourceOne).start(Mockito.any(), Mockito.eq(XAResource.TMNOFLAGS));

        ordered.verify(resourceOne).isSameRM(resourceTwo);
        ordered.verify(resourceTwo).start(Mockito.any(), Mockito.eq(XAResource.TMJOIN));

        Mockito.verifyNoMoreInteractions(transactionStore, resourceOne, resourceTwo, resourceThree);
    }
}
