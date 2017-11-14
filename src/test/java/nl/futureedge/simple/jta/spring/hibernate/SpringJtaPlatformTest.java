package nl.futureedge.simple.jta.spring.hibernate;

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
import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.UserTransaction;
import org.hibernate.engine.transaction.jta.platform.spi.JtaPlatformException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.transaction.jta.JtaTransactionManager;

public class SpringJtaPlatformTest {

    private JtaTransactionManager springJtaTransactionManager;
    private SpringJtaPlatform subject;

    @Before
    public void setup() {
        springJtaTransactionManager = Mockito.mock(JtaTransactionManager.class);

        subject = new SpringJtaPlatform();
        subject.setSpringJtaTransactionManager(springJtaTransactionManager);
    }

    @Test
    public void retrieveTransactionManager() {
        TransactionManager result = Mockito.mock(TransactionManager.class);
        Mockito.when(springJtaTransactionManager.getTransactionManager()).thenReturn(result);
        Assert.assertSame(result, subject.retrieveTransactionManager());
        Mockito.verify(springJtaTransactionManager).getTransactionManager();
        Mockito.verifyNoMoreInteractions(springJtaTransactionManager);
    }

    @Test
    public void retrieveUserTransaction() {
        UserTransaction result = Mockito.mock(UserTransaction.class);
        Mockito.when(springJtaTransactionManager.getUserTransaction()).thenReturn(result);
        Assert.assertSame(result, subject.retrieveUserTransaction());
        Mockito.verify(springJtaTransactionManager).getUserTransaction();
        Mockito.verifyNoMoreInteractions(springJtaTransactionManager);
    }

    @Test
    public void getTransactionIdentifier() {
        Transaction transaction = Mockito.mock(Transaction.class);
        Assert.assertSame(transaction, subject.getTransactionIdentifier(transaction));
    }

    @Test
    public void getCurrentStatus() throws Exception {
        TransactionManager transactionManager = Mockito.mock(TransactionManager.class);
        Mockito.when(springJtaTransactionManager.getTransactionManager()).thenReturn(transactionManager);
        Mockito.when(transactionManager.getStatus()).thenReturn(Status.STATUS_PREPARING);

        Assert.assertEquals(Status.STATUS_PREPARING, subject.getCurrentStatus());

        Mockito.verify(springJtaTransactionManager).getTransactionManager();
        Mockito.verify(transactionManager).getStatus();
        Mockito.verifyNoMoreInteractions(springJtaTransactionManager, transactionManager);
    }

    @Test
    public void canRegisterSynchronization() throws Exception {
        TransactionManager transactionManager = Mockito.mock(TransactionManager.class);
        Mockito.when(springJtaTransactionManager.getTransactionManager()).thenReturn(transactionManager);
        Mockito.when(transactionManager.getStatus()).thenReturn(Status.STATUS_PREPARING, Status.STATUS_ACTIVE, Status.STATUS_MARKED_ROLLBACK);

        Assert.assertFalse(subject.canRegisterSynchronization());
        Assert.assertTrue(subject.canRegisterSynchronization());
        Assert.assertFalse(subject.canRegisterSynchronization());

        Mockito.verify(springJtaTransactionManager, Mockito.times(3)).getTransactionManager();
        Mockito.verify(transactionManager, Mockito.times(3)).getStatus();
        Mockito.verifyNoMoreInteractions(springJtaTransactionManager, transactionManager);
    }


    @Test
    public void canRegisterSynchronizationFailed() throws Exception {
        TransactionManager transactionManager = Mockito.mock(TransactionManager.class);
        Mockito.when(springJtaTransactionManager.getTransactionManager()).thenReturn(transactionManager);
        Mockito.when(transactionManager.getStatus()).thenThrow(new SystemException("Test"));

        try {
            subject.canRegisterSynchronization();
            Assert.fail("JtaPlatformException expected");
        } catch (JtaPlatformException e) {
            //Expected
        }

        Mockito.verify(springJtaTransactionManager).getTransactionManager();
        Mockito.verify(transactionManager).getStatus();
        Mockito.verifyNoMoreInteractions(springJtaTransactionManager, transactionManager);
    }

    @Test
    public void registerSynchronization() throws Exception {
        TransactionManager transactionManager = Mockito.mock(TransactionManager.class);
        Transaction transaction = Mockito.mock(Transaction.class);
        Mockito.when(springJtaTransactionManager.getTransactionManager()).thenReturn(transactionManager);
        Mockito.when(transactionManager.getTransaction()).thenReturn(transaction);

        Synchronization synchronization = Mockito.mock(Synchronization.class);
        subject.registerSynchronization(synchronization);

        Mockito.verify(springJtaTransactionManager).getTransactionManager();
        Mockito.verify(transactionManager).getTransaction();
        Mockito.verify(transaction).registerSynchronization(synchronization);
        Mockito.verifyNoMoreInteractions(springJtaTransactionManager, transactionManager, transaction);
    }

    @Test
    public void registerSynchronizationFailed() throws Exception {
        TransactionManager transactionManager = Mockito.mock(TransactionManager.class);
        Transaction transaction = Mockito.mock(Transaction.class);
        Mockito.when(springJtaTransactionManager.getTransactionManager()).thenReturn(transactionManager);
        Mockito.when(transactionManager.getTransaction()).thenReturn(transaction);

        Synchronization synchronization = Mockito.mock(Synchronization.class);
        Mockito.doThrow(new RollbackException("test")).when(transaction).registerSynchronization(synchronization);

        try {
            subject.registerSynchronization(synchronization);
            Assert.fail("JtaPlatformException expected");
        } catch (JtaPlatformException e) {
            //Expected
        }

        Mockito.verify(springJtaTransactionManager).getTransactionManager();
        Mockito.verify(transactionManager).getTransaction();
        Mockito.verify(transaction).registerSynchronization(synchronization);
        Mockito.verifyNoMoreInteractions(springJtaTransactionManager, transactionManager, transaction);
    }
}
