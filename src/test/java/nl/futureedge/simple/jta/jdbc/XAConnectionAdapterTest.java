package nl.futureedge.simple.jta.jdbc;

import java.sql.Array;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.NClob;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Statement;
import java.sql.Struct;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executor;
import javax.sql.XAConnection;
import nl.futureedge.simple.jta.JtaTransactionManager;
import nl.futureedge.simple.jta.store.JtaTransactionStore;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class XAConnectionAdapterTest {

    private JtaTransactionManager transactionManager;

    private boolean connectionClosed;
    private Connection connection;
    private XAConnection xaConnection;

    private XAConnectionAdapter subject;

    @Before
    public void setup() throws Exception {
        JtaTransactionStore transactionStore = Mockito.mock(JtaTransactionStore.class);
        transactionManager = new JtaTransactionManager();
        transactionManager.setUniqueName("test");
        transactionManager.setJtaTransactionStore(transactionStore);
        transactionManager.afterPropertiesSet();

        connection = Mockito.mock(Connection.class);
        connectionClosed = false;
        Mockito.when(connection.isClosed()).thenAnswer(invocation -> connectionClosed);

        xaConnection = Mockito.mock(XAConnection.class);
        Mockito.doAnswer(invocation -> {
            connectionClosed = true;
            return null;
        }).when(xaConnection).close();

        Mockito.when(xaConnection.getConnection()).thenReturn(connection);

        subject = new XAConnectionAdapter(xaConnection);

        Mockito.verify(xaConnection).getConnection();
        Mockito.verifyNoMoreInteractions(xaConnection);
    }

    @Test
    public void testCloseOutsideTransaction() throws SQLException {
        subject.transactionCompleted(null);
        // Should have warning message
        Assert.assertTrue(subject.isClosed());
        Mockito.verify(xaConnection).close();
        Mockito.verifyNoMoreInteractions(connection, xaConnection);

        subject.close();
        Assert.assertTrue(subject.isClosed());
        Mockito.verifyNoMoreInteractions(connection, xaConnection);
    }

    @Test
    public void testCloseFailed() throws Exception {
        Mockito.doThrow(new SQLException("Test")).when(xaConnection).close();

        subject.transactionCompleted(null);
        Mockito.verify(xaConnection).close();
        Mockito.verifyNoMoreInteractions(connection, xaConnection);
    }

    @Test
    public void testCloseAndReopenInTransaction() throws Exception {
        transactionManager.begin();
        transactionManager.getRequiredTransaction().registerSystemCallback(subject);

        subject.close();
        Assert.assertTrue(subject.isClosed());
        Mockito.verifyNoMoreInteractions(xaConnection);

        Assert.assertTrue(subject.reopen());
        Assert.assertFalse(subject.isClosed());
        Mockito.verifyNoMoreInteractions(xaConnection);

        subject.close();
        Assert.assertTrue(subject.isClosed());
        Mockito.verifyNoMoreInteractions(xaConnection);

        transactionManager.rollback();
        Assert.assertTrue(subject.isClosed());
        Mockito.verify(xaConnection).close();
        Mockito.verifyNoMoreInteractions(xaConnection);

        Assert.assertFalse(subject.reopen());
    }

    @Test
    public void testIsWrapperFor() throws SQLException {
        Assert.assertFalse(subject.isWrapperFor(null));
        Mockito.verifyNoMoreInteractions(connection, xaConnection);
    }


    @Test
    public void testUnwrap() throws SQLException {
        try {
            subject.unwrap(Object.class);
            Assert.fail("SQLException expected");
        } catch (SQLException e) {
            // Expected
        }
        Mockito.verifyNoMoreInteractions(connection, xaConnection);
    }

    @Test
    public void testAbort() throws SQLException {
        Executor executor = Mockito.mock(Executor.class);
        subject.abort(executor);

        Mockito.verify(connection).abort(executor);
        Mockito.verifyNoMoreInteractions(connection, xaConnection);
    }

    private void testCallAndClosedCheck(Callable call, Callable verify) throws SQLException {
        call.call();

        Mockito.verify(connection).isClosed();
        verify.call();
        Mockito.verifyNoMoreInteractions(connection, xaConnection);

        subject.close();
        //Mockito.verify(xaConnection).close();
        Mockito.verifyNoMoreInteractions(connection, xaConnection);

        try {
            call.call();
            Assert.fail("SQLException expected");
        } catch (SQLException e) {
            // Expected as connection is closed
        }

        //Mockito.verify(connection, Mockito.times(2)).isClosed();
        Mockito.verifyNoMoreInteractions(connection, xaConnection);

        subject.transactionCompleted(null);
        Mockito.verify(xaConnection).close();
        Mockito.verifyNoMoreInteractions(connection, xaConnection);

    }

    private interface Callable {
        void call() throws SQLException;
    }

    @Test
    public void testClearWarning() throws SQLException {
        testCallAndClosedCheck(
                () -> subject.clearWarnings(),
                () -> Mockito.verify(connection).clearWarnings()
        );
    }

    @Test
    public void testCommit() throws SQLException {
        testCallAndClosedCheck(
                () -> subject.commit(),
                () -> Mockito.verify(connection).commit()
        );
    }

    @Test
    public void testCreateArrayOf() throws SQLException {
        final Array result = Mockito.mock(Array.class);
        Mockito.when(connection.createArrayOf("type", new Object[]{})).thenReturn(result);
        testCallAndClosedCheck(
                () -> Assert.assertSame(result, subject.createArrayOf("type", new Object[]{})),
                () -> Mockito.verify(connection).createArrayOf("type", new Object[]{})
        );
    }

    @Test
    public void testCreateBlob() throws SQLException {
        final Blob result = Mockito.mock(Blob.class);
        Mockito.when(connection.createBlob()).thenReturn(result);
        testCallAndClosedCheck(
                () -> Assert.assertSame(result, subject.createBlob()),
                () -> Mockito.verify(connection).createBlob()
        );
    }

    @Test
    public void testCreateClob() throws SQLException {
        final Clob result = Mockito.mock(Clob.class);
        Mockito.when(connection.createClob()).thenReturn(result);
        testCallAndClosedCheck(
                () -> Assert.assertSame(result, subject.createClob()),
                () -> Mockito.verify(connection).createClob()
        );
    }

    @Test
    public void testCreateNClob() throws SQLException {
        final NClob result = Mockito.mock(NClob.class);
        Mockito.when(connection.createNClob()).thenReturn(result);
        testCallAndClosedCheck(
                () -> Assert.assertSame(result, subject.createNClob()),
                () -> Mockito.verify(connection).createNClob()
        );
    }

    @Test
    public void testCreateSQLXML() throws SQLException {
        final SQLXML result = Mockito.mock(SQLXML.class);
        Mockito.when(connection.createSQLXML()).thenReturn(result);
        testCallAndClosedCheck(
                () -> Assert.assertSame(result, subject.createSQLXML()),
                () -> Mockito.verify(connection).createSQLXML()
        );
    }

    @Test
    public void testCreateStatement() throws SQLException {
        final Statement result = Mockito.mock(Statement.class);
        Mockito.when(connection.createStatement()).thenReturn(result);
        testCallAndClosedCheck(
                () -> Assert.assertSame(result, subject.createStatement()),
                () -> Mockito.verify(connection).createStatement()
        );
    }

    @Test
    public void testCreateStatementWithTypeAndConcurrency() throws SQLException {
        final Statement result = Mockito.mock(Statement.class);
        Mockito.when(connection.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)).thenReturn(result);
        testCallAndClosedCheck(
                () -> Assert.assertSame(result, subject.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)),
                () -> Mockito.verify(connection).createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)
        );
    }

    @Test
    public void testCreateStatementWithTypeAndConcurrencyAndHoldability() throws SQLException {
        final Statement result = Mockito.mock(Statement.class);
        Mockito.when(connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE, ResultSet.CLOSE_CURSORS_AT_COMMIT))
                .thenReturn(result);
        testCallAndClosedCheck(
                () -> Assert.assertSame(result,
                        subject.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE, ResultSet.CLOSE_CURSORS_AT_COMMIT)),
                () -> Mockito.verify(connection)
                        .createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE, ResultSet.CLOSE_CURSORS_AT_COMMIT)
        );
    }

    @Test
    public void testCreateStruct() throws SQLException {
        final Struct result = Mockito.mock(Struct.class);
        Mockito.when(connection.createStruct("name", new Object[]{})).thenReturn(result);
        testCallAndClosedCheck(
                () -> Assert.assertSame(result, subject.createStruct("name", new Object[]{})),
                () -> Mockito.verify(connection).createStruct("name", new Object[]{})
        );
    }

    @Test
    public void testGetAutocommit() throws SQLException {
        Mockito.when(connection.getAutoCommit()).thenReturn(true);
        testCallAndClosedCheck(
                () -> Assert.assertEquals(true, subject.getAutoCommit()),
                () -> Mockito.verify(connection).getAutoCommit()
        );
    }

    @Test
    public void testGetCatalog() throws SQLException {
        final String result = "catalog";
        Mockito.when(connection.getCatalog()).thenReturn(result);
        testCallAndClosedCheck(
                () -> Assert.assertSame(result, subject.getCatalog()),
                () -> Mockito.verify(connection).getCatalog()
        );
    }

    @Test
    public void testGetClientInfo() throws SQLException {
        final Properties result = new Properties();
        Mockito.when(connection.getClientInfo()).thenReturn(result);
        testCallAndClosedCheck(
                () -> Assert.assertSame(result, subject.getClientInfo()),
                () -> Mockito.verify(connection).getClientInfo()
        );
    }

    @Test
    public void testGetClientInfoWithName() throws SQLException {
        final String result = "result";
        Mockito.when(connection.getClientInfo("name")).thenReturn(result);
        testCallAndClosedCheck(
                () -> Assert.assertSame(result, subject.getClientInfo("name")),
                () -> Mockito.verify(connection).getClientInfo("name")
        );
    }

    @Test
    public void testGetHoldability() throws SQLException {
        final int result = ResultSet.HOLD_CURSORS_OVER_COMMIT;
        Mockito.when(connection.getHoldability()).thenReturn(result);
        testCallAndClosedCheck(
                () -> Assert.assertSame(result, subject.getHoldability()),
                () -> Mockito.verify(connection).getHoldability()
        );
    }

    @Test
    public void testGetMetaData() throws SQLException {
        final DatabaseMetaData result = Mockito.mock(DatabaseMetaData.class);
        Mockito.when(connection.getMetaData()).thenReturn(result);
        testCallAndClosedCheck(
                () -> Assert.assertSame(result, subject.getMetaData()),
                () -> Mockito.verify(connection).getMetaData()
        );
    }

    @Test
    public void testGetNetworkTimeout() throws SQLException {
        final int result = 2400;
        Mockito.when(connection.getNetworkTimeout()).thenReturn(result);
        testCallAndClosedCheck(
                () -> Assert.assertEquals(result, subject.getNetworkTimeout()),
                () -> Mockito.verify(connection).getNetworkTimeout()
        );
    }

    @Test
    public void testGetSchema() throws SQLException {
        final String result = "schema";
        Mockito.when(connection.getSchema()).thenReturn(result);
        testCallAndClosedCheck(
                () -> Assert.assertSame(result, subject.getSchema()),
                () -> Mockito.verify(connection).getSchema()
        );
    }

    @Test
    public void testGetTransactionIsolation() throws SQLException {
        final int result = Connection.TRANSACTION_REPEATABLE_READ;
        Mockito.when(connection.getTransactionIsolation()).thenReturn(result);
        testCallAndClosedCheck(
                () -> Assert.assertEquals(result, subject.getTransactionIsolation()),
                () -> Mockito.verify(connection).getTransactionIsolation()
        );
    }

    @Test
    public void testGetTypeMap() throws SQLException {
        final Map<String, Class<?>> result = new HashMap<>();
        Mockito.when(connection.getTypeMap()).thenReturn(result);
        testCallAndClosedCheck(
                () -> Assert.assertSame(result, subject.getTypeMap()),
                () -> Mockito.verify(connection).getTypeMap()
        );
    }


    @Test
    public void testGetWarnings() throws SQLException {
        final SQLWarning result = Mockito.mock(SQLWarning.class);
        Mockito.when(connection.getWarnings()).thenReturn(result);
        testCallAndClosedCheck(
                () -> Assert.assertSame(result, subject.getWarnings()),
                () -> Mockito.verify(connection).getWarnings()
        );
    }


    @Test
    public void testIsReadOnly() throws SQLException {
        final boolean result = true;
        Mockito.when(connection.isReadOnly()).thenReturn(result);
        testCallAndClosedCheck(
                () -> Assert.assertSame(result, subject.isReadOnly()),
                () -> Mockito.verify(connection).isReadOnly()
        );
    }

    @Test
    public void testIsValid() throws SQLException {
        final boolean result = true;
        Mockito.when(connection.isValid(1000)).thenReturn(result);
        testCallAndClosedCheck(
                () -> Assert.assertSame(result, subject.isValid(1000)),
                () -> Mockito.verify(connection).isValid(1000)
        );
    }


    @Test
    public void testNativeSQL() throws SQLException {
        final String result = "native";
        Mockito.when(connection.nativeSQL("sql")).thenReturn(result);
        testCallAndClosedCheck(
                () -> Assert.assertSame(result, subject.nativeSQL("sql")),
                () -> Mockito.verify(connection).nativeSQL("sql")
        );
    }

    @Test
    public void testPrepareCall() throws SQLException {
        final CallableStatement result = Mockito.mock(CallableStatement.class);
        Mockito.when(connection.prepareCall("sql")).thenReturn(result);
        testCallAndClosedCheck(
                () -> Assert.assertSame(result, subject.prepareCall("sql")),
                () -> Mockito.verify(connection).prepareCall("sql")
        );
    }

    @Test
    public void testPrepareCallWithTypeAndConcurrency() throws SQLException {
        final CallableStatement result = Mockito.mock(CallableStatement.class);
        Mockito.when(connection.prepareCall("sql", ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE)).thenReturn(result);
        testCallAndClosedCheck(
                () -> Assert.assertSame(result, subject.prepareCall("sql", ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE)),
                () -> Mockito.verify(connection).prepareCall("sql", ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE)
        );
    }

    @Test
    public void testPrepareCallWithTypeAndConcurrencyAndHoldability() throws SQLException {
        final CallableStatement result = Mockito.mock(CallableStatement.class);
        Mockito.when(connection.prepareCall("sql", ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE, ResultSet.HOLD_CURSORS_OVER_COMMIT))
                .thenReturn(result);
        testCallAndClosedCheck(
                () -> Assert.assertSame(result,
                        subject.prepareCall("sql", ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE, ResultSet.HOLD_CURSORS_OVER_COMMIT)),
                () -> Mockito.verify(connection).prepareCall("sql", ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE, ResultSet.HOLD_CURSORS_OVER_COMMIT)
        );
    }

    @Test
    public void testPrepareStatement() throws SQLException {
        final PreparedStatement result = Mockito.mock(PreparedStatement.class);
        Mockito.when(connection.prepareStatement("sql")).thenReturn(result);
        testCallAndClosedCheck(
                () -> Assert.assertSame(result, subject.prepareStatement("sql")),
                () -> Mockito.verify(connection).prepareStatement("sql")
        );
    }

    @Test
    public void testPrepareStatementWithKeys() throws SQLException {
        final PreparedStatement result = Mockito.mock(PreparedStatement.class);
        Mockito.when(connection.prepareStatement("sql", Statement.RETURN_GENERATED_KEYS)).thenReturn(result);
        testCallAndClosedCheck(
                () -> Assert.assertSame(result, subject.prepareStatement("sql", Statement.RETURN_GENERATED_KEYS)),
                () -> Mockito.verify(connection).prepareStatement("sql", Statement.RETURN_GENERATED_KEYS)
        );
    }

    @Test
    public void testPrepareStatementWithIndexes() throws SQLException {
        final PreparedStatement result = Mockito.mock(PreparedStatement.class);
        Mockito.when(connection.prepareStatement("sql", new int[]{})).thenReturn(result);
        testCallAndClosedCheck(
                () -> Assert.assertSame(result, subject.prepareStatement("sql", new int[]{})),
                () -> Mockito.verify(connection).prepareStatement("sql", new int[]{})
        );
    }

    @Test
    public void testPrepareStatementWithNames() throws SQLException {
        final PreparedStatement result = Mockito.mock(PreparedStatement.class);
        Mockito.when(connection.prepareStatement("sql", new String[]{})).thenReturn(result);
        testCallAndClosedCheck(
                () -> Assert.assertSame(result, subject.prepareStatement("sql", new String[]{})),
                () -> Mockito.verify(connection).prepareStatement("sql", new String[]{})
        );
    }

    @Test
    public void testPrepareStatementWithTypeAndConcurrency() throws SQLException {
        final PreparedStatement result = Mockito.mock(PreparedStatement.class);
        Mockito.when(connection.prepareStatement("sql", ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE))
                .thenReturn(result);
        testCallAndClosedCheck(
                () -> Assert.assertSame(result,
                        subject.prepareStatement("sql", ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE)),
                () -> Mockito.verify(connection)
                        .prepareStatement("sql", ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE)
        );
    }


    @Test
    public void testPrepareStatementWithTypeAndConcurrencyAndHoldability() throws SQLException {
        final PreparedStatement result = Mockito.mock(PreparedStatement.class);
        Mockito.when(connection.prepareStatement("sql", ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE, ResultSet.CLOSE_CURSORS_AT_COMMIT))
                .thenReturn(result);
        testCallAndClosedCheck(
                () -> Assert.assertSame(result,
                        subject.prepareStatement("sql", ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE, ResultSet.CLOSE_CURSORS_AT_COMMIT)),
                () -> Mockito.verify(connection)
                        .prepareStatement("sql", ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE, ResultSet.CLOSE_CURSORS_AT_COMMIT)
        );
    }

    @Test
    public void testReleaseSavepoint() throws SQLException {
        testCallAndClosedCheck(
                () -> subject.releaseSavepoint(null),
                () -> Mockito.verify(connection).releaseSavepoint(null)
        );
    }


    @Test
    public void testRollback() throws SQLException {
        testCallAndClosedCheck(
                () -> subject.rollback(),
                () -> Mockito.verify(connection).rollback()
        );
    }

    @Test
    public void testRollbackWithSavepoint() throws SQLException {
        testCallAndClosedCheck(
                () -> subject.rollback(null),
                () -> Mockito.verify(connection).rollback(null)
        );
    }

    @Test
    public void testSetAutoCommit() throws SQLException {
        testCallAndClosedCheck(
                () -> subject.setAutoCommit(true),
                () -> Mockito.verify(connection).setAutoCommit(true)
        );
    }

    @Test
    public void testSetCatalog() throws SQLException {
        testCallAndClosedCheck(
                () -> subject.setCatalog("catalog"),
                () -> Mockito.verify(connection).setCatalog("catalog")
        );
    }

    @Test
    public void testSetClientInfo() throws SQLException {
        Properties argument = new Properties();
        testCallAndClosedCheck(
                () -> subject.setClientInfo(argument),
                () -> Mockito.verify(connection).setClientInfo(argument)
        );
    }

    @Test
    public void testSetClientInfoWithNameAndValue() throws SQLException {
        testCallAndClosedCheck(
                () -> subject.setClientInfo("name", "value"),
                () -> Mockito.verify(connection).setClientInfo("name", "value")
        );
    }

    @Test
    public void testSetHoldability() throws SQLException {
        testCallAndClosedCheck(
                () -> subject.setHoldability(ResultSet.CLOSE_CURSORS_AT_COMMIT),
                () -> Mockito.verify(connection).setHoldability(ResultSet.CLOSE_CURSORS_AT_COMMIT)
        );
    }

    @Test
    public void testSetNetworkTimeout() throws SQLException {
        Executor executor = Mockito.mock(Executor.class);
        testCallAndClosedCheck(
                () -> subject.setNetworkTimeout(executor, 15000),
                () -> Mockito.verify(connection).setNetworkTimeout(executor, 15000)
        );
    }

    @Test
    public void testSetReadOnly() throws SQLException {
        testCallAndClosedCheck(
                () -> subject.setReadOnly(true),
                () -> Mockito.verify(connection).setReadOnly(true)
        );
    }

    @Test
    public void testSetSavepoint() throws SQLException {
        testCallAndClosedCheck(
                () -> subject.setSavepoint(),
                () -> Mockito.verify(connection).setSavepoint()
        );
    }

    @Test
    public void testSetSavepointWithName() throws SQLException {
        testCallAndClosedCheck(
                () -> subject.setSavepoint("name"),
                () -> Mockito.verify(connection).setSavepoint("name")
        );
    }

    @Test
    public void testSetSchema() throws SQLException {
        testCallAndClosedCheck(
                () -> subject.setSchema("name"),
                () -> Mockito.verify(connection).setSchema("name")
        );
    }

    @Test
    public void testSetTransactionIsolation() throws SQLException {
        testCallAndClosedCheck(
                () -> subject.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ),
                () -> Mockito.verify(connection).setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ)
        );
    }


    @Test
    public void testSetTypeMap() throws SQLException {
        final Map<String, Class<?>> map = new HashMap<>();
        testCallAndClosedCheck(
                () -> subject.setTypeMap(map),
                () -> Mockito.verify(connection).setTypeMap(map)
        );
    }
}

