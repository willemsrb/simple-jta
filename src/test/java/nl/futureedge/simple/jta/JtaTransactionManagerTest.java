package nl.futureedge.simple.jta;

/*-
 * #%L
 * Simple JTA
 * %%
 * Copyright (C) 2017 Future Edge IT
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.xa.XAResource;
import nl.futureedge.simple.jta.store.JtaTransactionStore;
import nl.futureedge.simple.jta.store.JtaTransactionStoreException;
import nl.futureedge.simple.jta.xa.XAResourceAdapter;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JtaTransactionManagerTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(JtaTransactionManagerTest.class);

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
    public void test() throws Exception {
        // Initial
        Assert.assertNull(transactionManager.getTransaction());
        Assert.assertEquals(Status.STATUS_NO_TRANSACTION, transactionManager.getStatus());
        try {
            transactionManager.commit();
            Assert.fail("IllegalStateException expected");
        } catch(IllegalStateException e) {
            // Expected
        }

        // Begin
        transactionManager.begin();
        Assert.assertNotNull(transactionManager.getTransaction());
        Assert.assertEquals(Status.STATUS_ACTIVE, transactionManager.getStatus());

        try {
            transactionManager.begin();
            Assert.fail("NotSupportedException expected");
        } catch (NotSupportedException e) {
            // Expected
        }

        // Rollback
        transactionManager.rollback();
        Assert.assertNull(transactionManager.getTransaction());
        Assert.assertEquals(Status.STATUS_NO_TRANSACTION, transactionManager.getStatus());

        // Begin
        transactionManager.begin();
        Assert.assertNotNull(transactionManager.getTransaction());
        Assert.assertEquals(Status.STATUS_ACTIVE, transactionManager.getStatus());

        // Commit
        transactionManager.commit();
        Assert.assertNull(transactionManager.getTransaction());
        Assert.assertEquals(Status.STATUS_NO_TRANSACTION, transactionManager.getStatus());

        // Begin
        transactionManager.begin();
        Assert.assertNotNull(transactionManager.getTransaction());
        Assert.assertEquals(Status.STATUS_ACTIVE, transactionManager.getStatus());
        transactionManager.setRollbackOnly();
        Assert.assertEquals(Status.STATUS_MARKED_ROLLBACK, transactionManager.getStatus());
        Transaction transaction = transactionManager.getTransaction();
        Assert.assertEquals(Status.STATUS_MARKED_ROLLBACK, transaction.getStatus());
        try {
            transactionManager.commit();
            Assert.fail("RollbackException expected");
        } catch (RollbackException e) {
            // Expected
        }

        transactionManager.rollback();
        Assert.assertEquals(Status.STATUS_ROLLEDBACK, transaction.getStatus());
        Assert.assertEquals(Status.STATUS_NO_TRANSACTION, transactionManager.getStatus());
    }

    @Test
    public void testBeginNextTransactionIdFail() throws Exception {
        Mockito.when(transactionStore.nextTransactionId()).thenThrow(new JtaTransactionStoreException("Test"));

        Assert.assertEquals(Status.STATUS_NO_TRANSACTION, transactionManager.getStatus());
        try {
            transactionManager.begin();
            Assert.fail("SystemException expected");
        } catch (SystemException e) {
            // Expected
        }
        Assert.assertEquals(Status.STATUS_NO_TRANSACTION, transactionManager.getStatus());
    }

    @Test
    public void testSetTransactionTimeout() throws Exception {
        XAResource resourceOne = Mockito.mock(XAResource.class);
        XAResource resourceTwo = Mockito.mock(XAResource.class);

        transactionManager.setTransactionTimeout(15);

        transactionManager.begin();
        JtaTransaction transaction = transactionManager.getTransaction();
        transaction.enlistResource(new XAResourceAdapter("resourceOne", true, false, resourceOne));
        transaction.enlistResource(new XAResourceAdapter("resourceTwo", true, false, resourceTwo));

        transactionManager.setTransactionTimeout(67);

        // Calls in enlist
        Mockito.verify(resourceOne).setTransactionTimeout(15);
        Mockito.verify(resourceOne).start(Mockito.any(), Mockito.eq(XAResource.TMNOFLAGS));
        Mockito.verify(resourceOne).isSameRM(resourceTwo);
        Mockito.verify(resourceTwo).setTransactionTimeout(15);
        Mockito.verify(resourceTwo).start(Mockito.any(), Mockito.eq(XAResource.TMNOFLAGS));

        // Calls caused by setTransactionTimeout
        Mockito.verify(resourceOne).setTransactionTimeout(67);
        Mockito.verify(resourceTwo).setTransactionTimeout(67);

        Mockito.verifyNoMoreInteractions(resourceOne, resourceTwo);
    }
}
