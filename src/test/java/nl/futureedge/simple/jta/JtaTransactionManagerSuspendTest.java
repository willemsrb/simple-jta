package nl.futureedge.simple.jta;

import javax.transaction.InvalidTransactionException;
import javax.transaction.Status;
import javax.transaction.Transaction;
import nl.futureedge.simple.jta.store.JtaTransactionStore;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JtaTransactionManagerSuspendTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(JtaTransactionManagerSuspendTest.class);

    private JtaTransactionStore transactionStore;
    private long transactionId;
    private JtaTransactionManager transactionManager;

    @Before
    public void setup() throws Exception {
        transactionStore = Mockito.mock(JtaTransactionStore.class);
        Mockito.when(transactionStore.nextTransactionId()).thenAnswer(invocation -> transactionId++);

        transactionManager = new JtaTransactionManager();
        transactionManager.setUniqueName(this.getClass().getSimpleName());
        Assert.assertEquals(this.getClass().getSimpleName(), transactionManager.getUniqueName());
        transactionManager.setJtaTransactionStore(transactionStore);
        transactionManager.afterPropertiesSet();
    }

    @After
    public void destroy() throws Exception {
        transactionManager.destroy();
    }

    @Test
    public void testSuspendResume() throws Exception {
        Assert.assertEquals(Status.STATUS_NO_TRANSACTION, transactionManager.getStatus());
        Assert.assertNull(transactionManager.suspend());

        transactionManager.begin();
        Assert.assertEquals(Status.STATUS_ACTIVE, transactionManager.getStatus());
        LOGGER.info("FIRST: {}", transactionManager.getTransaction());

        Transaction transaction = transactionManager.suspend();
        LOGGER.info("FIRST (suspended): {}", transaction);
        Assert.assertEquals(Status.STATUS_NO_TRANSACTION, transactionManager.getStatus());

        transactionManager.begin();
        Assert.assertEquals(Status.STATUS_ACTIVE, transactionManager.getStatus());
        LOGGER.info("SECOND: {}", transactionManager.getTransaction());
        Assert.assertNotEquals(transaction, transactionManager.getTransaction());

        try {
            transactionManager.resume(transaction);
            Assert.fail("IllegalStateException expected");
        } catch (IllegalStateException e) {
            // Expected
        }

        transactionManager.commit();
        Assert.assertEquals(Status.STATUS_NO_TRANSACTION, transactionManager.getStatus());

        try {
            transactionManager.resume(null);
            Assert.fail("InvalidTransactionException expected");
        } catch (InvalidTransactionException e) {
            // Expected
        }

        transactionManager.resume(transaction);
        Assert.assertEquals(Status.STATUS_ACTIVE, transactionManager.getStatus());

        transactionManager.commit();
        Assert.assertEquals(Status.STATUS_NO_TRANSACTION, transactionManager.getStatus());
    }

}
