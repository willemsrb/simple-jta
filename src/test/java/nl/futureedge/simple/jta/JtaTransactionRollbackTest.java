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
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mockito;

public class JtaTransactionRollbackTest {

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

        transaction.enlistResource(new XAResourceAdapter("resourceOne", true, false, resourceOne));
        transaction.enlistResource(new XAResourceAdapter("resourceTwo", true, false, resourceTwo));
        transaction.enlistResource(new XAResourceAdapter("resourceThree", true, false, resourceThree));
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

        ordered.verify(resourceOne).isSameRM(resourceTwo);
        final ArgumentCaptor<BranchJtaXid> branchXidTwoCaptor = ArgumentCaptor.forClass(BranchJtaXid.class);
        ordered.verify(transactionStore).active(branchXidTwoCaptor.capture(), Mockito.eq("resourceTwo"));
        branchXidTwo = branchXidTwoCaptor.getValue();
        ordered.verify(resourceTwo).start(branchXidTwo, XAResource.TMNOFLAGS);

        ordered.verify(resourceOne).isSameRM(resourceThree);
        ordered.verify(resourceTwo).isSameRM(resourceThree);
        final ArgumentCaptor<BranchJtaXid> branchXidThreeCaptor = ArgumentCaptor.forClass(BranchJtaXid.class);
        ordered.verify(transactionStore).active(branchXidThreeCaptor.capture(), Mockito.eq("resourceThree"));
        branchXidThree = branchXidThreeCaptor.getValue();
        ordered.verify(resourceThree).start(branchXidThree, XAResource.TMNOFLAGS);
    }

    private void rollbackExpectSystemException() throws RollbackException {
        try {
            transactionManager.rollback();
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
        transaction.rollback();

        InOrder ordered = Mockito.inOrder(transactionStore, resourceOne, resourceTwo, resourceThree);
        verifySetup(ordered);

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
    public void testStoreRollingBackFailure() throws Exception {
        Mockito.doThrow(new JtaTransactionStoreException("Fail")).when(transactionStore).rollingBack(globalXid);
        rollbackExpectSystemException();

        InOrder ordered = Mockito.inOrder(transactionStore, resourceOne, resourceTwo, resourceThree);
        verifySetup(ordered);

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
    public void testStoreRollingBackResourceFailure() throws Exception {
        Mockito.doThrow(new JtaTransactionStoreException("Fail")).when(transactionStore).rollingBack(Mockito.any(), Mockito.eq("resourceTwo"));
        rollbackExpectSystemException();

        InOrder ordered = Mockito.inOrder(transactionStore, resourceOne, resourceTwo, resourceThree);
        verifySetup(ordered);

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
    public void testEndFailure() throws Exception {
        XAException failure = new XAException("Fail");
        Mockito.doThrow(failure).when(resourceTwo).end(Mockito.any(), Mockito.eq(XAResource.TMFAIL));
        rollbackExpectSystemException();

        InOrder ordered = Mockito.inOrder(transactionStore, resourceOne, resourceTwo, resourceThree);
        verifySetup(ordered);

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
        ordered.verify(transactionStore).rollbackFailed(globalXid);

        ordered.verify(transactionStore).transactionCompleted(transaction);
        Mockito.verifyNoMoreInteractions(transactionStore, resourceOne, resourceTwo, resourceThree);
    }

    @Test
    public void testRollbackFailure() throws Exception {
        XAException failure = new XAException("Fail");
        Mockito.doThrow(failure).when(resourceTwo).rollback(Mockito.any());
        rollbackExpectSystemException();

        InOrder ordered = Mockito.inOrder(transactionStore, resourceOne, resourceTwo, resourceThree);
        verifySetup(ordered);

        // Rollback
        ordered.verify(transactionStore).rollingBack(globalXid);
        ordered.verify(transactionStore).rollingBack(branchXidOne, "resourceOne");
        ordered.verify(resourceOne).end(branchXidOne, XAResource.TMFAIL);
        ordered.verify(resourceOne).rollback(branchXidOne);
        ordered.verify(transactionStore).rolledBack(branchXidOne, "resourceOne");
        ordered.verify(transactionStore).rollingBack(branchXidTwo, "resourceTwo");
        ordered.verify(resourceTwo).end(branchXidTwo, XAResource.TMFAIL);
        ordered.verify(resourceTwo).rollback(branchXidTwo);
        ordered.verify(transactionStore).rollbackFailed(branchXidTwo, "resourceTwo", failure);
        ordered.verify(transactionStore).rollingBack(branchXidThree, "resourceThree");
        ordered.verify(resourceThree).end(branchXidThree, XAResource.TMFAIL);
        ordered.verify(resourceThree).rollback(branchXidThree);
        ordered.verify(transactionStore).rolledBack(branchXidThree, "resourceThree");
        ordered.verify(transactionStore).rollbackFailed(globalXid);

        ordered.verify(transactionStore).transactionCompleted(transaction);
        Mockito.verifyNoMoreInteractions(transactionStore, resourceOne, resourceTwo, resourceThree);
    }

    @Test
    public void testStoreRolledBackResourceFailure() throws Exception {
        Mockito.doThrow(new JtaTransactionStoreException("Fail")).when(transactionStore).rolledBack(Mockito.any(), Mockito.eq("resourceTwo"));
        rollbackExpectSystemException();

        InOrder ordered = Mockito.inOrder(transactionStore, resourceOne, resourceTwo, resourceThree);
        verifySetup(ordered);

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
    public void testStoreRolledBackFailure() throws Exception {
        Mockito.doThrow(new JtaTransactionStoreException("Fail")).when(transactionStore).rolledBack(globalXid);
        rollbackExpectSystemException();

        InOrder ordered = Mockito.inOrder(transactionStore, resourceOne, resourceTwo, resourceThree);
        verifySetup(ordered);

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
    public void testStoreRollbackResourceFailure() throws Exception {
        XAException failure = new XAException("Fail");
        Mockito.doThrow(failure).when(resourceTwo).rollback(Mockito.any());
        Mockito.doThrow(new JtaTransactionStoreException("Fail")).when(transactionStore).rollbackFailed(branchXidTwo, "resourceTwo", failure);
        rollbackExpectSystemException();

        InOrder ordered = Mockito.inOrder(transactionStore, resourceOne, resourceTwo, resourceThree);
        verifySetup(ordered);

        // Rollback
        ordered.verify(transactionStore).rollingBack(globalXid);
        ordered.verify(transactionStore).rollingBack(branchXidOne, "resourceOne");
        ordered.verify(resourceOne).end(branchXidOne, XAResource.TMFAIL);
        ordered.verify(resourceOne).rollback(branchXidOne);
        ordered.verify(transactionStore).rolledBack(branchXidOne, "resourceOne");
        ordered.verify(transactionStore).rollingBack(branchXidTwo, "resourceTwo");
        ordered.verify(resourceTwo).end(branchXidTwo, XAResource.TMFAIL);
        ordered.verify(resourceTwo).rollback(branchXidTwo);
        ordered.verify(transactionStore).rollbackFailed(branchXidTwo, "resourceTwo", failure);
        ordered.verify(transactionStore).rollingBack(branchXidThree, "resourceThree");
        ordered.verify(resourceThree).end(branchXidThree, XAResource.TMFAIL);
        ordered.verify(resourceThree).rollback(branchXidThree);
        ordered.verify(transactionStore).rolledBack(branchXidThree, "resourceThree");
        ordered.verify(transactionStore).rollbackFailed(globalXid);

        ordered.verify(transactionStore).transactionCompleted(transaction);
        Mockito.verifyNoMoreInteractions(transactionStore, resourceOne, resourceTwo, resourceThree);
    }

    @Test
    public void testStoreRollbackFailure() throws Exception {
        XAException failure = new XAException("Fail");
        Mockito.doThrow(failure).when(resourceTwo).rollback(Mockito.any());
        Mockito.doThrow(new JtaTransactionStoreException("Fail")).when(transactionStore).rollbackFailed(globalXid);
        rollbackExpectSystemException();

        InOrder ordered = Mockito.inOrder(transactionStore, resourceOne, resourceTwo, resourceThree);
        verifySetup(ordered);

        // Rollback
        ordered.verify(transactionStore).rollingBack(globalXid);
        ordered.verify(transactionStore).rollingBack(branchXidOne, "resourceOne");
        ordered.verify(resourceOne).end(branchXidOne, XAResource.TMFAIL);
        ordered.verify(resourceOne).rollback(branchXidOne);
        ordered.verify(transactionStore).rolledBack(branchXidOne, "resourceOne");
        ordered.verify(transactionStore).rollingBack(branchXidTwo, "resourceTwo");
        ordered.verify(resourceTwo).end(branchXidTwo, XAResource.TMFAIL);
        ordered.verify(resourceTwo).rollback(branchXidTwo);
        ordered.verify(transactionStore).rollbackFailed(branchXidTwo, "resourceTwo", failure);
        ordered.verify(transactionStore).rollingBack(branchXidThree, "resourceThree");
        ordered.verify(resourceThree).end(branchXidThree, XAResource.TMFAIL);
        ordered.verify(resourceThree).rollback(branchXidThree);
        ordered.verify(transactionStore).rolledBack(branchXidThree, "resourceThree");
        ordered.verify(transactionStore).rollbackFailed(globalXid);

        ordered.verify(transactionStore).transactionCompleted(transaction);
        Mockito.verifyNoMoreInteractions(transactionStore, resourceOne, resourceTwo, resourceThree);
    }
}
