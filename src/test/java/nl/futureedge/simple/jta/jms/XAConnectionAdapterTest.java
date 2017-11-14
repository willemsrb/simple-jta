package nl.futureedge.simple.jta.jms;

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
import javax.jms.ConnectionMetaData;
import javax.jms.ExceptionListener;
import javax.jms.JMSException;
import javax.jms.Session;
import javax.jms.XAConnection;
import javax.jms.XASession;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import nl.futureedge.simple.jta.JtaTransactionManager;
import nl.futureedge.simple.jta.store.JtaTransactionStore;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class XAConnectionAdapterTest {

    private JtaTransactionManager transactionManager;
    private XAConnection xaConnection;

    private XAConnectionAdapter subject;


    @Before
    public void setup() throws Exception {
        JtaTransactionStore transactionStore = Mockito.mock(JtaTransactionStore.class);
        transactionManager = new JtaTransactionManager();
        transactionManager.setUniqueName("test");
        transactionManager.setJtaTransactionStore(transactionStore);
        transactionManager.afterPropertiesSet();

        xaConnection = Mockito.mock(XAConnection.class);
        subject = new XAConnectionAdapter("testResourceManager", false, false, xaConnection, transactionManager);

        Mockito.verifyNoMoreInteractions(xaConnection);
    }

    @Test
    public void close() throws Exception {
        subject.close();
        Mockito.verify(xaConnection).close();
        Mockito.verifyNoMoreInteractions(xaConnection);
    }

    @Test
    public void closeFailed() throws Exception {
        Mockito.doThrow(new JMSException("test")).when(xaConnection).close();

        transactionManager.begin();
        subject.close();
        Mockito.verifyNoMoreInteractions(xaConnection);

        subject.transactionCompleted(null);
        Mockito.verify(xaConnection).close();
        Mockito.verifyNoMoreInteractions(xaConnection);
    }

    @Test
    public void closeAfterTransaction() throws Exception {
        transactionManager.begin();
        subject.close();
        Mockito.verifyNoMoreInteractions(xaConnection);

        subject.transactionCompleted(null);
        Mockito.verify(xaConnection).close();
        Mockito.verifyNoMoreInteractions(xaConnection);
    }

    @Test
    public void createSessionUntransacted() throws Exception {
        Session session = Mockito.mock(Session.class);
        Mockito.when(xaConnection.createSession(false, Session.AUTO_ACKNOWLEDGE)).thenReturn(session);

        Assert.assertSame(session, subject.createSession(false, Session.AUTO_ACKNOWLEDGE));
        Mockito.verify(xaConnection).createSession(false, Session.AUTO_ACKNOWLEDGE);
        Mockito.verifyNoMoreInteractions(xaConnection);
    }

    @Test
    public void createSessionNoTransaction() throws Exception {
        try {
            subject.createSession(true, Session.AUTO_ACKNOWLEDGE);
            Assert.fail("IllegalStateException expected");
        } catch (IllegalStateException e) {
            // Expected
        }
        Mockito.verifyNoMoreInteractions(xaConnection);
    }

    @Test
    public void createSession() throws Exception {
        XAResource xaResource = Mockito.mock(XAResource.class);
        XASession xaSession = Mockito.mock(XASession.class);
        Mockito.when(xaConnection.createXASession()).thenReturn(xaSession);
        Mockito.when(xaSession.getXAResource()).thenReturn(xaResource);

        transactionManager.begin();
        Assert.assertSame(xaSession, subject.createSession(true, Session.AUTO_ACKNOWLEDGE));

        Mockito.verify(xaConnection).createXASession();
        Mockito.verify(xaSession).getXAResource();
        Mockito.verify(xaResource).start(Mockito.any(), Mockito.eq(XAResource.TMNOFLAGS));
        Mockito.verifyNoMoreInteractions(xaConnection, xaSession, xaResource);
    }

    @Test
    public void createSessionFailed() throws Exception {
        XAResource xaResource = Mockito.mock(XAResource.class);
        XASession xaSession = Mockito.mock(XASession.class);
        Mockito.when(xaConnection.createXASession()).thenReturn(xaSession);
        Mockito.when(xaSession.getXAResource()).thenReturn(xaResource);
        Mockito.doThrow(new XAException("Test")).when(xaResource).start(Mockito.any(), Mockito.eq(XAResource.TMNOFLAGS));

        transactionManager.begin();
        try {
            subject.createSession(true, Session.AUTO_ACKNOWLEDGE);
            Assert.fail("JMSException expected");
        } catch (JMSException e) {
            // Expected
        }

        Mockito.verify(xaConnection).createXASession();
        Mockito.verify(xaSession).getXAResource();
        Mockito.verify(xaResource).start(Mockito.any(), Mockito.eq(XAResource.TMNOFLAGS));
        Mockito.verifyNoMoreInteractions(xaConnection, xaSession, xaResource);
    }

    @Test
    public void getClientID() throws Exception {
        Mockito.when(xaConnection.getClientID()).thenReturn("clientID");
        Assert.assertEquals("clientID", subject.getClientID());
        Mockito.verify(xaConnection).getClientID();
        Mockito.verifyNoMoreInteractions(xaConnection);
    }

    @Test
    public void setClientID() throws Exception {
        subject.setClientID("clientID");
        Mockito.verify(xaConnection).setClientID("clientID");
        Mockito.verifyNoMoreInteractions(xaConnection);
    }

    @Test
    public void getMetaData() throws Exception {
        ConnectionMetaData result = Mockito.mock(ConnectionMetaData.class);
        Mockito.when(xaConnection.getMetaData()).thenReturn(result);
        Assert.assertSame(result, subject.getMetaData());
        Mockito.verify(xaConnection).getMetaData();
        Mockito.verifyNoMoreInteractions(xaConnection);
    }

    @Test
    public void getExceptionListener() throws Exception {
        ExceptionListener result = Mockito.mock(ExceptionListener.class);
        Mockito.when(xaConnection.getExceptionListener()).thenReturn(result);
        Assert.assertSame(result, subject.getExceptionListener());
        Mockito.verify(xaConnection).getExceptionListener();
        Mockito.verifyNoMoreInteractions(xaConnection);
    }

    @Test
    public void setExceptionListener() throws Exception {
        ExceptionListener exceptionListener = Mockito.mock(ExceptionListener.class);
        subject.setExceptionListener(exceptionListener);
        Mockito.verify(xaConnection).setExceptionListener(exceptionListener);
        Mockito.verifyNoMoreInteractions(xaConnection);
    }

    @Test
    public void start() throws Exception {
        subject.start();
        Mockito.verify(xaConnection).start();
        Mockito.verifyNoMoreInteractions(xaConnection);
    }

    @Test
    public void stop() throws Exception {
        subject.stop();
        Mockito.verify(xaConnection).stop();
        Mockito.verifyNoMoreInteractions(xaConnection);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void createConnectionConsumer() throws Exception {
        subject.createConnectionConsumer(null, null, null, 100);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void createDurableConnectionConsumer() throws Exception {
        subject.createDurableConnectionConsumer(null, null, null, null, 100);
    }

}
