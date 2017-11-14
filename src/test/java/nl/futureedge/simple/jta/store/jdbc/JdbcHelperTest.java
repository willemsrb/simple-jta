package nl.futureedge.simple.jta.store.jdbc;

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
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import nl.futureedge.simple.jta.ReflectionTestUtils;
import nl.futureedge.simple.jta.spring.config.SpringConfigParser;
import nl.futureedge.simple.jta.store.JtaTransactionStoreException;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;

public class JdbcHelperTest {

    @Test
    public void constructor() throws Exception {
        ReflectionTestUtils.testNotInstantiable(JdbcHelper.class);
    }

    @Test
    public void doInConnection() throws Exception {
        // Prepare
        Connection connection = Mockito.mock(Connection.class);
        JdbcHelper.JdbcFunction<String> callback = Mockito.mock(JdbcHelper.JdbcFunction.class);

        Mockito.when(callback.apply(connection)).thenReturn("result");

        // Execute
        Assert.assertEquals("result", JdbcHelper.doInConnection(null, connection, callback));

        // Verify
        InOrder inOrder = Mockito.inOrder(connection, callback);
        inOrder.verify(callback).apply(connection);
        inOrder.verify(connection).commit();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void doInConnectionCallbackFailed() throws Exception {
        // Prepare
        Connection connection = Mockito.mock(Connection.class);
        JdbcHelper.JdbcFunction<String> callback = Mockito.mock(JdbcHelper.JdbcFunction.class);

        Mockito.when(callback.apply(connection)).thenThrow(new SQLException("Test"));

        // Execute
        try {
            JdbcHelper.doInConnection(null, connection, callback);
            Assert.fail("JtaTransactionStoreException expected");
        } catch (JtaTransactionStoreException e) {
            //Expected
        }

        // Verify
        InOrder inOrder = Mockito.inOrder(connection, callback);
        inOrder.verify(callback).apply(connection);
        inOrder.verify(connection).rollback();
        inOrder.verifyNoMoreInteractions();
    }


    @Test
    public void doInConnectionRollbackFailed() throws Exception {
        // Prepare
        Connection connection = Mockito.mock(Connection.class);
        JdbcHelper.JdbcFunction<String> callback = Mockito.mock(JdbcHelper.JdbcFunction.class);

        Mockito.when(callback.apply(connection)).thenThrow(new SQLException("Test"));
        Mockito.doThrow(new SQLException("Test")).when(connection).rollback();

        // Execute
        try {
            JdbcHelper.doInConnection(null, connection, callback);
            Assert.fail("JtaTransactionStoreException expected");
        } catch (JtaTransactionStoreException e) {
            //Expected
        }

        // Verify
        InOrder inOrder = Mockito.inOrder(connection, callback);
        inOrder.verify(callback).apply(connection);
        inOrder.verify(connection).rollback();
        inOrder.verifyNoMoreInteractions();
    }


    @Test
    public void doInConnectionWithPool() throws Exception {
        // Prepare - sql driver
        Driver testDriver = Mockito.mock(Driver.class);
        Mockito.when(testDriver.acceptsURL(Mockito.anyString())).thenAnswer(invocation -> ((String) invocation.getArgument(0)).startsWith("jdbc:test:"));
        Connection connection = Mockito.mock(Connection.class);
        Mockito.when(testDriver.connect(Mockito.anyString(), Mockito.any())).thenReturn(connection);

        // Prepare
        JdbcHelper.JdbcFunction<String> callback = Mockito.mock(JdbcHelper.JdbcFunction.class);

        // Execute
        try {
            DriverManager.registerDriver(testDriver);
            JdbcConnectionPool pool = new JdbcConnectionPool(null, "jdbc:test:test", null, null);
            Assert.assertEquals(1, ((List<?>) ReflectionTestUtils.getField(pool, "available")).size());

            Mockito.when(callback.apply(connection)).then(invocation -> {
                Assert.assertEquals(0, ((List<?>) ReflectionTestUtils.getField(pool, "available")).size());
                return "result";
            });

            // Execute - really
            Assert.assertEquals("result", JdbcHelper.doInConnection(pool, null, callback));

            Assert.assertEquals(1, ((List<?>) ReflectionTestUtils.getField(pool, "available")).size());
            pool.close();
        } finally {
            DriverManager.deregisterDriver(testDriver);
        }

        // Verify
        InOrder inOrder = Mockito.inOrder(testDriver, connection, callback);
        inOrder.verify(testDriver).connect(Mockito.eq("jdbc:test:test"), Mockito.any());
        inOrder.verify(callback).apply(connection);
        inOrder.verify(connection).commit();
        inOrder.verify(connection).close();
        inOrder.verifyNoMoreInteractions();
    }


    @Test
    public void doInStatement() throws Exception {
        Connection connection = Mockito.mock(Connection.class);
        Statement statement = Mockito.mock(Statement.class);
        Mockito.when(connection.createStatement()).thenReturn(statement);

        JdbcHelper.JdbcStatementCallback callback = Mockito.mock(JdbcHelper.JdbcStatementCallback.class);

        JdbcHelper.doInStatement(connection, callback);
        InOrder inOrder = Mockito.inOrder(connection, statement, callback);
        inOrder.verify(connection).createStatement();
        inOrder.verify(callback).apply(statement);
        inOrder.verify(statement).close();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void doInStatementCallbackFailed() throws Exception {
        Connection connection = Mockito.mock(Connection.class);
        Statement statement = Mockito.mock(Statement.class);
        Mockito.when(connection.createStatement()).thenReturn(statement);

        JdbcHelper.JdbcStatementCallback callback = Mockito.mock(JdbcHelper.JdbcStatementCallback.class);
        Mockito.doThrow(new SQLException("Test")).when(callback).apply(statement);

        try {
            JdbcHelper.doInStatement(connection, callback);
            Assert.fail("SQLException expected");
        } catch (SQLException e) {
            //Expected
        }
        InOrder inOrder = Mockito.inOrder(connection, statement, callback);
        inOrder.verify(connection).createStatement();
        inOrder.verify(callback).apply(statement);
        inOrder.verify(statement).close();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void prepareAndExecuteUpdate() throws Exception {
        Connection connection = Mockito.mock(Connection.class);
        PreparedStatement statement = Mockito.mock(PreparedStatement.class);
        Mockito.when(connection.prepareStatement("sql to execute")).thenReturn(statement);
        Mockito.when(statement.executeUpdate()).thenReturn(42);

        JdbcHelper.JdbcPreparedStatementCallback callback = Mockito.mock(JdbcHelper.JdbcPreparedStatementCallback.class);

        // Execute
        Assert.assertEquals(42, JdbcHelper.prepareAndExecuteUpdate(connection, "sql to execute", callback));

        // Verify
        InOrder inOrder = Mockito.inOrder(connection, statement, callback);
        inOrder.verify(connection).prepareStatement("sql to execute");
        inOrder.verify(callback).apply(statement);
        inOrder.verify(statement).executeUpdate();
        inOrder.verify(statement).close();
        inOrder.verifyNoMoreInteractions();
    }


    @Test
    public void prepareAndExecuteUpdateCallbackFailed() throws Exception {
        Connection connection = Mockito.mock(Connection.class);
        PreparedStatement statement = Mockito.mock(PreparedStatement.class);
        Mockito.when(connection.prepareStatement("sql to execute")).thenReturn(statement);

        JdbcHelper.JdbcPreparedStatementCallback callback = Mockito.mock(JdbcHelper.JdbcPreparedStatementCallback.class);
        Mockito.doThrow(new SQLException("Test")).when(callback).apply(statement);

        // Execute
        try {
            JdbcHelper.prepareAndExecuteUpdate(connection, "sql to execute", callback);
            Assert.fail("SQLException expected");
        } catch (SQLException e) {
            // Expected
        }

        // Verify
        InOrder inOrder = Mockito.inOrder(connection, statement, callback);
        inOrder.verify(connection).prepareStatement("sql to execute");
        inOrder.verify(callback).apply(statement);
        inOrder.verify(statement).close();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void prepareAndExecuteQuery() throws Exception {
        Connection connection = Mockito.mock(Connection.class);
        PreparedStatement statement = Mockito.mock(PreparedStatement.class);
        Mockito.when(connection.prepareStatement("sql to execute")).thenReturn(statement);
        ResultSet resultSet = Mockito.mock(ResultSet.class);
        Mockito.when(statement.executeQuery()).thenReturn(resultSet);

        JdbcHelper.JdbcPreparedStatementCallback statementCallback = Mockito.mock(JdbcHelper.JdbcPreparedStatementCallback.class);
        JdbcHelper.JdbcResultSetCallback resultSetCallback = Mockito.mock(JdbcHelper.JdbcResultSetCallback.class);

        // Execute
        JdbcHelper.prepareAndExecuteQuery(connection, "sql to execute", statementCallback, resultSetCallback);

        // Verify
        InOrder inOrder = Mockito.inOrder(connection, statement, statementCallback, resultSet, resultSetCallback);
        inOrder.verify(connection).prepareStatement("sql to execute");
        inOrder.verify(statementCallback).apply(statement);
        inOrder.verify(statement).executeQuery();
        inOrder.verify(resultSetCallback).apply(resultSet);
        inOrder.verify(resultSet).close();
        inOrder.verify(statement).close();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void prepareAndExecuteQueryStatementCallbackFailed() throws Exception {
        Connection connection = Mockito.mock(Connection.class);
        PreparedStatement statement = Mockito.mock(PreparedStatement.class);
        Mockito.when(connection.prepareStatement("sql to execute")).thenReturn(statement);
        ResultSet resultSet = Mockito.mock(ResultSet.class);
        Mockito.when(statement.executeQuery()).thenReturn(resultSet);

        JdbcHelper.JdbcPreparedStatementCallback statementCallback = Mockito.mock(JdbcHelper.JdbcPreparedStatementCallback.class);
        Mockito.doThrow(new SQLException("Test")).when(statementCallback).apply(statement);
        JdbcHelper.JdbcResultSetCallback resultSetCallback = Mockito.mock(JdbcHelper.JdbcResultSetCallback.class);

        // Execute
        try {
            JdbcHelper.prepareAndExecuteQuery(connection, "sql to execute", statementCallback, resultSetCallback);
            Assert.fail("SQLException expected");
        } catch (SQLException e) {
            // Expected
        }

        // Verify
        InOrder inOrder = Mockito.inOrder(connection, statement, statementCallback, resultSet, resultSetCallback);
        inOrder.verify(connection).prepareStatement("sql to execute");
        inOrder.verify(statementCallback).apply(statement);
        inOrder.verify(statement).close();
        inOrder.verifyNoMoreInteractions();
    }


    @Test
    public void prepareAndExecuteQueryResultSetCallbackFailed() throws Exception {
        Connection connection = Mockito.mock(Connection.class);
        PreparedStatement statement = Mockito.mock(PreparedStatement.class);
        Mockito.when(connection.prepareStatement("sql to execute")).thenReturn(statement);
        ResultSet resultSet = Mockito.mock(ResultSet.class);
        Mockito.when(statement.executeQuery()).thenReturn(resultSet);

        JdbcHelper.JdbcPreparedStatementCallback statementCallback = Mockito.mock(JdbcHelper.JdbcPreparedStatementCallback.class);
        JdbcHelper.JdbcResultSetCallback resultSetCallback = Mockito.mock(JdbcHelper.JdbcResultSetCallback.class);
        Mockito.doThrow(new SQLException("Test")).when(resultSetCallback).apply(resultSet);

        // Execute
        try {
            JdbcHelper.prepareAndExecuteQuery(connection, "sql to execute", statementCallback, resultSetCallback);
            Assert.fail("SQLException expected");
        } catch (SQLException e) {
            // Expected
        }

        // Verify
        InOrder inOrder = Mockito.inOrder(connection, statement, statementCallback, resultSet, resultSetCallback);
        inOrder.verify(connection).prepareStatement("sql to execute");
        inOrder.verify(statementCallback).apply(statement);
        inOrder.verify(statement).executeQuery();
        inOrder.verify(resultSetCallback).apply(resultSet);
        inOrder.verify(resultSet).close();
        inOrder.verify(statement).close();
        inOrder.verifyNoMoreInteractions();
    }
}
