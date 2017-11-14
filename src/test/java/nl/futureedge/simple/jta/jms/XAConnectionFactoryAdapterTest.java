package nl.futureedge.simple.jta.jms;

import javax.jms.Connection;
import javax.jms.XAConnection;
import javax.jms.XAConnectionFactory;
import javax.jms.XASession;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;
import nl.futureedge.simple.jta.JtaTransactionManager;
import nl.futureedge.simple.jta.ReflectionTestUtils;
import nl.futureedge.simple.jta.store.JtaTransactionStore;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class XAConnectionFactoryAdapterTest {

    private JtaTransactionManager transactionManager;
    private XAConnectionFactory xaConnectionFactory;

    private XAConnectionFactoryAdapter subject;

    @Before
    public void setup() throws Exception {
        JtaTransactionStore transactionStore = Mockito.mock(JtaTransactionStore.class);
        transactionManager = new JtaTransactionManager();
        transactionManager.setUniqueName("test");
        transactionManager.setJtaTransactionStore(transactionStore);
        transactionManager.afterPropertiesSet();

        xaConnectionFactory = Mockito.mock(XAConnectionFactory.class);

        subject = new XAConnectionFactoryAdapter();
        subject.setUniqueName("testXaConnectionFactory");
        subject.setXaConnectionFactory(xaConnectionFactory);
        subject.setJtaTransactionManager(transactionManager);
        subject.setSupportsJoin(true);
        subject.setSupportsSuspend(true);
    }

    @Test
    public void afterPropertiesSet() throws Exception {
        XAResource xaResource = Mockito.mock(XAResource.class);
        XASession xaSession = Mockito.mock(XASession.class);
        XAConnection xaConnection = Mockito.mock(XAConnection.class);

        Mockito.when(xaConnectionFactory.createXAConnection()).thenReturn(xaConnection);
        Mockito.when(xaConnection.createXASession()).thenReturn(xaSession);
        Mockito.when(xaSession.getXAResource()).thenReturn(xaResource);
        Mockito.when(xaResource.recover(XAResource.TMENDRSCAN)).thenReturn(new Xid[]{});

        subject.afterPropertiesSet();

        Mockito.verify(xaConnectionFactory).createXAConnection();
        Mockito.verify(xaConnection).createXASession();
        Mockito.verify(xaSession).getXAResource();
        Mockito.verify(xaResource).recover(XAResource.TMENDRSCAN);

        Mockito.verifyNoMoreInteractions(xaConnectionFactory, xaConnection, xaSession, xaResource);
    }

    @Test
    public void createConnection() throws Exception {
        XAConnection xaConnection = Mockito.mock(XAConnection.class);
        Mockito.when(xaConnectionFactory.createXAConnection()).thenReturn(xaConnection);

        Connection connection = subject.createConnection();
        Assert.assertTrue(connection instanceof XAConnectionAdapter);
        Assert.assertEquals("testXaConnectionFactory", ReflectionTestUtils.getField(connection, "resourceManager"));
        Assert.assertEquals(true, ReflectionTestUtils.getField(connection, "supportsJoin"));
        Assert.assertEquals(true, ReflectionTestUtils.getField(connection, "supportsSuspend"));
        Assert.assertSame(xaConnection, ReflectionTestUtils.getField(connection, "xaConnection"));
        Assert.assertSame(transactionManager, ReflectionTestUtils.getField(connection, "transactionManager"));

        Mockito.verify(xaConnectionFactory).createXAConnection();
        Mockito.verifyNoMoreInteractions(xaConnectionFactory, xaConnection);
    }


    @Test
    public void createConnectionWithUsernameAndPassword() throws Exception {
        XAConnection xaConnection = Mockito.mock(XAConnection.class);
        Mockito.when(xaConnectionFactory.createXAConnection("user", "pass")).thenReturn(xaConnection);

        Connection connection = subject.createConnection("user", "pass");
        Assert.assertTrue(connection instanceof XAConnectionAdapter);
        Assert.assertEquals("testXaConnectionFactory", ReflectionTestUtils.getField(connection, "resourceManager"));
        Assert.assertEquals(true, ReflectionTestUtils.getField(connection, "supportsJoin"));
        Assert.assertSame(xaConnection, ReflectionTestUtils.getField(connection, "xaConnection"));
        Assert.assertSame(transactionManager, ReflectionTestUtils.getField(connection, "transactionManager"));

        Mockito.verify(xaConnectionFactory).createXAConnection("user", "pass");
        Mockito.verifyNoMoreInteractions(xaConnectionFactory, xaConnection);
    }
}
