package nl.futureedge.simple.jta;

import javax.transaction.Status;
import javax.transaction.SystemException;
import javax.transaction.xa.XAException;
import nl.futureedge.simple.jta.store.JtaTransactionStore;
import nl.futureedge.simple.jta.store.JtaTransactionStoreException;
import nl.futureedge.simple.jta.xid.GlobalJtaXid;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;

public class JtaTransactionNoPhaseTest {

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
    }

    private void verifySetup(InOrder ordered) throws JtaTransactionStoreException, XAException {
        // Startup manager
        ordered.verify(transactionStore).cleanup();

        // Start transaction
        ordered.verify(transactionStore).nextTransactionId();
        ordered.verify(transactionStore).active(globalXid);
    }

    @After
    public void destroy() throws Exception {
        transactionManager.destroy();
    }

    @Test
    public void testOk() throws Exception {
        transaction.commit();

        final InOrder ordered = Mockito.inOrder(transactionStore);
        verifySetup(ordered);

        // Commit (commit)
        ordered.verify(transactionStore).committed(globalXid);

        ordered.verify(transactionStore).transactionCompleted(transaction);
        ordered.verifyNoMoreInteractions();
    }

    @Test
    public void testStoreCommittedFailure() throws Exception {
        Mockito.doThrow(new JtaTransactionStoreException("Fail")).when(transactionStore).committed(globalXid);
        try {
            transactionManager.commit();
            Assert.fail("SystemException expected");
        } catch (SystemException e) {
            // Expected
        }

        final InOrder ordered = Mockito.inOrder(transactionStore);
        verifySetup(ordered);

        // Commit (commit)
        ordered.verify(transactionStore).committed(globalXid);

        ordered.verify(transactionStore).transactionCompleted(transaction);
        ordered.verifyNoMoreInteractions();
    }
}
