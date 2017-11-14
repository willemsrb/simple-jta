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
