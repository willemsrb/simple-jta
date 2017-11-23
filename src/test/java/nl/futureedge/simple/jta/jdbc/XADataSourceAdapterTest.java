package nl.futureedge.simple.jta.jdbc;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.XAConnection;
import javax.sql.XADataSource;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;
import nl.futureedge.simple.jta.JtaTransactionManager;
import nl.futureedge.simple.jta.ReflectionTestUtils;
import nl.futureedge.simple.jta.store.JtaTransactionStore;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class XADataSourceAdapterTest {

    private JtaTransactionManager transactionManager;
    private XADataSource xaDataSource;
    private XADataSourceAdapter subject;

    @Before
    public void setup() throws Exception {
        JtaTransactionStore transactionStore = Mockito.mock(JtaTransactionStore.class);
        transactionManager = new JtaTransactionManager();
        transactionManager.setUniqueName("test");
        transactionManager.setJtaTransactionStore(transactionStore);
        transactionManager.afterPropertiesSet();

        xaDataSource = Mockito.mock(XADataSource.class);

        subject = new XADataSourceAdapter();
        subject.setUniqueName("testXaDataSource");
        subject.setXaDataSource(xaDataSource);
        subject.setJtaTransactionManager(transactionManager);
        subject.setSupportsJoin(true);
        subject.setSupportsSuspend(true);
    }


    @Test
    public void afterPropertiesSet() throws Exception {
        XAResource xaResource = Mockito.mock(XAResource.class);
        XAConnection xaConnection = Mockito.mock(XAConnection.class);
        Mockito.when(xaConnection.getXAResource()).thenReturn(xaResource);
        Mockito.when(xaDataSource.getXAConnection()).thenReturn(xaConnection);
        Mockito.when(xaResource.recover(XAResource.TMENDRSCAN)).thenReturn(new Xid[]{});

        subject.afterPropertiesSet();

        Mockito.verify(xaDataSource).getXAConnection();
        Mockito.verify(xaConnection).getXAResource();
        Mockito.verify(xaResource).recover(XAResource.TMENDRSCAN);

        Mockito.verify(xaConnection).close();

        Mockito.verifyNoMoreInteractions(xaDataSource, xaConnection, xaResource);
    }

    @Test
    public void getLogWriter() throws Exception {
        PrintWriter result = new PrintWriter(new StringWriter());
        Mockito.when(xaDataSource.getLogWriter()).thenReturn(result);
        Assert.assertSame(result, subject.getLogWriter());
        Mockito.verify(xaDataSource).getLogWriter();
        Mockito.verifyNoMoreInteractions(xaDataSource);
    }

    @Test
    public void getLoginTimeout() throws Exception {
        int result = 3400;
        Mockito.when(xaDataSource.getLoginTimeout()).thenReturn(result);
        Assert.assertEquals(result, subject.getLoginTimeout());
        Mockito.verify(xaDataSource).getLoginTimeout();
        Mockito.verifyNoMoreInteractions(xaDataSource);
    }

    @Test
    public void getParentLogger() throws Exception {
        java.util.logging.Logger result = Mockito.mock(java.util.logging.Logger.class);
        Mockito.when(xaDataSource.getParentLogger()).thenReturn(result);
        Assert.assertEquals(result, subject.getParentLogger());
        Mockito.verify(xaDataSource).getParentLogger();
        Mockito.verifyNoMoreInteractions(xaDataSource);
    }

    @Test
    public void setLogWriter() throws Exception {
        PrintWriter argument = new PrintWriter(new StringWriter());
        subject.setLogWriter(argument);
        Mockito.verify(xaDataSource).setLogWriter(argument);
        Mockito.verifyNoMoreInteractions(xaDataSource);
    }

    @Test
    public void setLoginTimeout() throws Exception {
        subject.setLoginTimeout(123);
        Mockito.verify(xaDataSource).setLoginTimeout(123);
        Mockito.verifyNoMoreInteractions(xaDataSource);
    }

    @Test
    public void isWrapperFor() throws Exception {
        Assert.assertFalse(subject.isWrapperFor(null));
    }

    @Test(expected = SQLException.class)
    public void unwrap() throws Exception {
        subject.unwrap(XADataSource.class);
    }

    @Test
    public void getConnectionOutsideTransactionYes() throws Exception {
        subject.setAllowNonTransactedConnections("yes");

        Connection connection = Mockito.mock(Connection.class);
        XAConnection xaConnection = Mockito.mock(XAConnection.class);
        Mockito.when(xaConnection.getConnection()).thenReturn(connection);
        Mockito.when(xaDataSource.getXAConnection()).thenReturn(xaConnection);

        Assert.assertNotNull(subject.getConnection());

        Mockito.verify(xaDataSource).getXAConnection();
        Mockito.verify(xaConnection).getConnection();
        Mockito.verifyNoMoreInteractions(xaDataSource, xaConnection, connection);
    }

    @Test
    public void getConnectionOutsideTransactionWarn() throws Exception {
        subject.setAllowNonTransactedConnections("warn");

        Connection connection = Mockito.mock(Connection.class);
        XAConnection xaConnection = Mockito.mock(XAConnection.class);
        Mockito.when(xaConnection.getConnection()).thenReturn(connection);
        Mockito.when(xaDataSource.getXAConnection()).thenReturn(xaConnection);

        Assert.assertNotNull(subject.getConnection());

        Mockito.verify(xaDataSource).getXAConnection();
        Mockito.verify(xaConnection).getConnection();
        Mockito.verifyNoMoreInteractions(xaDataSource, xaConnection, connection);
    }

    @Test
    public void getConnectionOutsideTransactionNotAllowed() throws Exception {
        subject.setAllowNonTransactedConnections("no");

        try {
            subject.getConnection();
            Assert.fail("SQLException expected");
        } catch (SQLException e) {
            // Expected
        }

        Mockito.verifyNoMoreInteractions(xaDataSource);
    }

    @Test
    public void getConnectionInTransaction() throws Exception {
        transactionManager.begin();

        Connection connection = Mockito.mock(Connection.class);
        XAResource xaResource = Mockito.mock(XAResource.class);
        XAConnection xaConnection = Mockito.mock(XAConnection.class);
        Mockito.when(xaConnection.getConnection()).thenReturn(connection);
        Mockito.when(xaConnection.getXAResource()).thenReturn(xaResource);
        Mockito.when(xaDataSource.getXAConnection()).thenReturn(xaConnection);

        Connection result = subject.getConnection();
        Assert.assertTrue(result instanceof XAConnectionAdapter);

        Mockito.verify(xaDataSource).getXAConnection();
        Mockito.verify(xaConnection).getXAResource();
        Mockito.verify(xaResource).start(Mockito.any(), Mockito.eq(XAResource.TMNOFLAGS));
        Mockito.verify(xaConnection).getConnection();
        Mockito.verifyNoMoreInteractions(xaDataSource, xaConnection, xaResource, connection);
    }


    @Test
    public void getConnectionInTransactionWithCredentials() throws Exception {
        transactionManager.begin();

        Connection connection = Mockito.mock(Connection.class);
        XAResource xaResource = Mockito.mock(XAResource.class);
        XAConnection xaConnection = Mockito.mock(XAConnection.class);
        Mockito.when(xaConnection.getConnection()).thenReturn(connection);
        Mockito.when(xaConnection.getXAResource()).thenReturn(xaResource);
        Mockito.when(xaDataSource.getXAConnection("user", "pass")).thenReturn(xaConnection);

        Connection result = subject.getConnection("user", "pass");
        Assert.assertTrue(result instanceof XAConnectionAdapter);

        Mockito.verify(xaDataSource).getXAConnection("user", "pass");
        Mockito.verify(xaConnection).getXAResource();
        Mockito.verify(xaResource).start(Mockito.any(), Mockito.eq(XAResource.TMNOFLAGS));
        Mockito.verify(xaConnection).getConnection();
        Mockito.verifyNoMoreInteractions(xaDataSource, xaConnection, xaResource, connection);
    }

    @Test
    public void getConnectionInTransactionEnlistFailed() throws Exception {
        transactionManager.begin();

        Connection connection = Mockito.mock(Connection.class);
        XAResource xaResource = Mockito.mock(XAResource.class);
        XAConnection xaConnection = Mockito.mock(XAConnection.class);
        Mockito.when(xaConnection.getConnection()).thenReturn(connection);
        Mockito.when(xaConnection.getXAResource()).thenReturn(xaResource);
        Mockito.when(xaDataSource.getXAConnection()).thenReturn(xaConnection);
        Mockito.doThrow(new XAException("Test")).when(xaResource).start(Mockito.any(), Mockito.eq(XAResource.TMNOFLAGS));

        try {
            subject.getConnection();
            Assert.fail("SQLException expected");
        } catch (SQLException e) {
            // Expected
        }

        Mockito.verify(xaDataSource).getXAConnection();
        Mockito.verify(xaConnection).getXAResource();
        Mockito.verify(xaResource).start(Mockito.any(), Mockito.eq(XAResource.TMNOFLAGS));
        Mockito.verifyNoMoreInteractions(xaDataSource, xaConnection, xaResource, connection);
    }

    @Test
    public void testReopen() throws Exception {
        transactionManager.begin();

        Connection connection = Mockito.mock(Connection.class);
        XAResource xaResource = Mockito.mock(XAResource.class);
        XAConnection xaConnection = Mockito.mock(XAConnection.class);
        Mockito.when(xaConnection.getConnection()).thenReturn(connection);
        Mockito.when(xaConnection.getXAResource()).thenReturn(xaResource);

        Connection connection2 = Mockito.mock(Connection.class);
        XAResource xaResource2 = Mockito.mock(XAResource.class);
        XAConnection xaConnection2 = Mockito.mock(XAConnection.class);
        Mockito.when(xaConnection2.getConnection()).thenReturn(connection2);
        Mockito.when(xaConnection2.getXAResource()).thenReturn(xaResource2);

        Mockito.when(xaDataSource.getXAConnection()).thenReturn(xaConnection, xaConnection2);

        Connection result = subject.getConnection();
        Assert.assertTrue(result instanceof XAConnectionAdapter);
        Assert.assertSame(xaConnection, ReflectionTestUtils.getField(result, "xaConnection"));

        Connection result2 = subject.getConnection();
        Assert.assertTrue(result2 instanceof XAConnectionAdapter);
        Assert.assertSame(xaConnection2, ReflectionTestUtils.getField(result2, "xaConnection"));

        result.close();

        Connection result3 = subject.getConnection();
        Assert.assertSame(result, result3);

    }

}
