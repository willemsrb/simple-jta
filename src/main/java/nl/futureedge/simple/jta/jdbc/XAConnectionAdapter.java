package nl.futureedge.simple.jta.jdbc;

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

import java.sql.Array;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.NClob;
import java.sql.PreparedStatement;
import java.sql.SQLClientInfoException;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Savepoint;
import java.sql.Statement;
import java.sql.Struct;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executor;
import javax.sql.XAConnection;
import nl.futureedge.simple.jta.JtaSystemCallback;
import nl.futureedge.simple.jta.JtaTransaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * XA Connection adapter; delegates (almost all) calls to the connection of the wrapped xa connection.
 *
 * The {@link #close()} method checks if a transaction is active. If a transaction is active, the wrapped connection will not be closed but 'registered' as
 * closed. When the transaction is completed {@link JtaTransaction#registerSynchronization} the connection will be closed.
 */
final class XAConnectionAdapter implements Connection, JtaSystemCallback {

    private static final Logger LOGGER = LoggerFactory.getLogger(XAConnectionAdapter.class);

    private final XAConnection xaConnection;
    private final Connection connection;

    private boolean connectionClosed = false;

    /**
     * Constructor.
     * @param xaConnection xa connection
     * @throws SQLException when the underlying connection of the xa connection could not be retrieved
     */
    XAConnectionAdapter(final XAConnection xaConnection) throws SQLException {
        this.xaConnection = xaConnection;
        connection = xaConnection.getConnection();
    }

    /* ******************************************************** */
    /* ******************************************************** */
    /* ******************************************************** */

    @Override
    public void transactionCompleted(final JtaTransaction transaction) {
        if (!connectionClosed) {
            LOGGER.warn("Transaction completed, but connection not closed! This probably indicates a programming error/connection leak!");
            connectionClosed = true;
        }

        try {
            LOGGER.debug("Closing connection after completion of transaction");
            xaConnection.close();
        } catch (final SQLException e) {
            LOGGER.warn("Could not close connection after completion of transaction", e);
        }
    }

    @Override
    public void close() throws SQLException {
        connectionClosed = true;
    }

    /* ******************************************************** */
    /* ******************************************************** */
    /* ******************************************************** */

    @Override
    public boolean isClosed() throws SQLException {
        LOGGER.trace("isClosed()");
        boolean result = connectionClosed || connection.isClosed();
        LOGGER.trace("isClosed() -> {}", result);
        return result;
    }

    private void checkNotClosed() throws SQLException {
        if (isClosed()) {
            throw new SQLException("Connection is closed");
        }
    }

    private void checkNotClosedForClientInfo() throws SQLClientInfoException {
        try {
            if (isClosed()) {
                throw new SQLClientInfoException("Connection is closed", null);
            }
        } catch (SQLException e) {
            throw new SQLClientInfoException("Could not determine connection status", null);
        }
    }

    /**
     * Reopen this connection if possible; a connection can be reopened if it was closed but not yet committed.
     * @return true, if and only if this connection was reopend
     * @throws SQLException Thrown if the connection encountered an exception
     */
    public boolean reopen() throws SQLException {
        if (connection.isClosed()) {
            return false;
        }

        if (connectionClosed) {
            connectionClosed = false;
            return true;
        }

        return false;
    }


    /* ******************************************************** */
    /* ******************************************************** */
    /* ******************************************************** */

    @Override
    public boolean isWrapperFor(final Class<?> iface) throws SQLException {
        LOGGER.trace("isWrapperFor(iface={})", iface);
        return false;
    }

    @Override
    public <T> T unwrap(final Class<T> iface) throws SQLException {
        LOGGER.trace("unwrap(iface={})", iface);
        throw new SQLException("unwrap not supported");
    }

    @Override
    public void abort(final Executor executor) throws SQLException {
        LOGGER.trace("abort(executor={})", executor);
        connection.abort(executor);
    }

    @Override
    public void clearWarnings() throws SQLException {
        LOGGER.trace("clearWarnings()");
        checkNotClosed();
        connection.clearWarnings();
    }

    @Override
    public void commit() throws SQLException {
        LOGGER.trace("commit()");
        checkNotClosed();
        connection.commit();
    }

    @Override
    public Array createArrayOf(final String typeName, final Object[] elements) throws SQLException {
        LOGGER.trace("createArrayOf(typeName={},elements={})", typeName, elements);
        checkNotClosed();
        return connection.createArrayOf(typeName, elements);
    }

    @Override
    public Blob createBlob() throws SQLException {
        LOGGER.trace("createBlob()");
        checkNotClosed();
        return connection.createBlob();
    }

    @Override
    public Clob createClob() throws SQLException {
        LOGGER.trace("createClob()");
        checkNotClosed();
        return connection.createClob();
    }

    @Override
    public NClob createNClob() throws SQLException {
        LOGGER.trace("createNClob()");
        checkNotClosed();
        return connection.createNClob();
    }

    @Override
    public SQLXML createSQLXML() throws SQLException {
        LOGGER.trace("createSQLXML()");
        checkNotClosed();
        return connection.createSQLXML();
    }

    @Override
    public Statement createStatement() throws SQLException {
        LOGGER.trace("createStatement()");
        checkNotClosed();
        return connection.createStatement();
    }

    @Override
    public Statement createStatement(final int resultSetType, final int resultSetConcurrency) throws SQLException {
        LOGGER.trace("createStatement(resultSetType={},resultSetConcurrency={})", resultSetType, resultSetConcurrency);
        checkNotClosed();
        return connection.createStatement(resultSetType, resultSetConcurrency);
    }

    @Override
    public Statement createStatement(final int resultSetType, final int resultSetConcurrency, final int resultSetHoldability)
            throws SQLException {
        LOGGER.trace("createStatement(resultSetType={},resultSetConcurrency={},resultSetHoldability={})", resultSetType, resultSetConcurrency,
                resultSetHoldability);
        checkNotClosed();
        return connection.createStatement(resultSetType, resultSetConcurrency, resultSetHoldability);
    }

    @Override
    public Struct createStruct(final String typeName, final Object[] attributes) throws SQLException {
        LOGGER.trace("createStruct(typeName={},attributes={})", typeName, attributes);
        checkNotClosed();
        return connection.createStruct(typeName, attributes);
    }

    @Override
    public boolean getAutoCommit() throws SQLException {
        LOGGER.trace("getAutoCommit()");
        checkNotClosed();
        return connection.getAutoCommit();
    }

    @Override
    public String getCatalog() throws SQLException {
        LOGGER.trace("getCatalog()");
        checkNotClosed();
        return connection.getCatalog();
    }

    @Override
    public Properties getClientInfo() throws SQLException {
        LOGGER.trace("getClientInfo()");
        checkNotClosed();
        return connection.getClientInfo();
    }

    @Override
    public String getClientInfo(final String name) throws SQLException {
        LOGGER.trace("getClientInfo(name={})", name);
        checkNotClosed();
        return connection.getClientInfo(name);
    }

    @Override
    public int getHoldability() throws SQLException {
        LOGGER.trace("getHoldability()");
        checkNotClosed();
        return connection.getHoldability();
    }

    @Override
    public DatabaseMetaData getMetaData() throws SQLException {
        LOGGER.trace("getMetaData()");
        checkNotClosed();
        return connection.getMetaData();
    }

    @Override
    public int getNetworkTimeout() throws SQLException {
        LOGGER.trace("getNetworkTimeout()");
        checkNotClosed();
        return connection.getNetworkTimeout();
    }

    @Override
    public String getSchema() throws SQLException {
        LOGGER.trace("getSchema()");
        checkNotClosed();
        return connection.getSchema();
    }

    @Override
    public int getTransactionIsolation() throws SQLException {
        LOGGER.trace("getTransactionIsolation()");
        checkNotClosed();
        return connection.getTransactionIsolation();
    }

    @Override
    public Map<String, Class<?>> getTypeMap() throws SQLException {
        LOGGER.trace("getTypeMap()");
        checkNotClosed();
        return connection.getTypeMap();
    }

    @Override
    public SQLWarning getWarnings() throws SQLException {
        LOGGER.trace("getWarnings()");
        checkNotClosed();
        return connection.getWarnings();
    }

    @Override
    public boolean isReadOnly() throws SQLException {
        LOGGER.trace("isReadOnly()");
        checkNotClosed();
        return connection.isReadOnly();
    }

    @Override
    public boolean isValid(final int timeout) throws SQLException {
        LOGGER.trace("isValid(timeout={})", timeout);
        checkNotClosed();
        return connection.isValid(timeout);
    }

    @Override
    public String nativeSQL(final String sql) throws SQLException {
        LOGGER.trace("nativeSQL(sql={})", sql);
        checkNotClosed();
        return connection.nativeSQL(sql);
    }

    @Override
    public CallableStatement prepareCall(final String sql) throws SQLException {
        LOGGER.trace("prepareCall(sql={})", sql);
        checkNotClosed();
        return connection.prepareCall(sql);
    }

    @Override
    public CallableStatement prepareCall(final String sql, final int resultSetType, final int resultSetConcurrency) throws SQLException {
        LOGGER.trace("prepareCall(sql={},resultSetType={},resultSetConcurrency={})", sql, resultSetType, resultSetConcurrency);
        checkNotClosed();
        return connection.prepareCall(sql, resultSetType, resultSetConcurrency);
    }

    @Override
    public CallableStatement prepareCall(final String sql, final int resultSetType, final int resultSetConcurrency,
                                         final int resultSetHoldability) throws SQLException {
        LOGGER.trace("prepareCall(sql={},resultSetType={},resultSetConcurrency={},resultSetHoldability={})", sql, resultSetType, resultSetConcurrency,
                resultSetHoldability);
        checkNotClosed();
        return connection.prepareCall(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
    }

    @Override
    public PreparedStatement prepareStatement(final String sql) throws SQLException {
        LOGGER.trace("prepareStatement(sql={})", sql);
        checkNotClosed();
        return connection.prepareStatement(sql);
    }

    @Override
    public PreparedStatement prepareStatement(final String sql, final int autoGeneratedKeys) throws SQLException {
        LOGGER.trace("prepareStatement(sql={},autoGeneratedKeys={})", sql, autoGeneratedKeys);
        checkNotClosed();
        return connection.prepareStatement(sql, autoGeneratedKeys);
    }

    @Override
    public PreparedStatement prepareStatement(final String sql, final int[] columnIndexes) throws SQLException {
        LOGGER.trace("prepareStatement(sql={},columnIndexes={})", sql, columnIndexes);
        checkNotClosed();
        return connection.prepareStatement(sql, columnIndexes);
    }

    @Override
    public PreparedStatement prepareStatement(final String sql, final String[] columnNames) throws SQLException {
        LOGGER.trace("prepareStatement(sql={},columnIndexes={})", sql, columnNames);
        checkNotClosed();
        return connection.prepareStatement(sql, columnNames);
    }

    @Override
    public PreparedStatement prepareStatement(final String sql, final int resultSetType, final int resultSetConcurrency)
            throws SQLException {
        LOGGER.trace("prepareStatement(sql={},resultSetType={},resultSetConcurrency={})", sql, resultSetType, resultSetConcurrency);
        checkNotClosed();
        return connection.prepareStatement(sql, resultSetType, resultSetConcurrency);
    }

    @Override
    public PreparedStatement prepareStatement(final String sql, final int resultSetType, final int resultSetConcurrency,
                                              final int resultSetHoldability) throws SQLException {
        LOGGER.trace("prepareStatement(sql={},resultSetType={},resultSetConcurrency={},resultSetHoldability={})", sql, resultSetType, resultSetConcurrency,
                resultSetHoldability);
        checkNotClosed();
        return connection.prepareStatement(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
    }

    @Override
    public void releaseSavepoint(final Savepoint savepoint) throws SQLException {
        LOGGER.trace("releaseSavepoint(savepoint={})", savepoint);
        checkNotClosed();
        connection.releaseSavepoint(savepoint);
    }

    @Override
    public void rollback() throws SQLException {
        LOGGER.trace("rollback()");
        checkNotClosed();
        connection.rollback();
    }

    @Override
    public void rollback(final Savepoint savepoint) throws SQLException {
        LOGGER.trace("rollback(savepoint={})", savepoint);
        checkNotClosed();
        connection.rollback(savepoint);
    }

    @Override
    public void setAutoCommit(final boolean autoCommit) throws SQLException {
        LOGGER.trace("setAutoCommit(autoCommit={})", autoCommit);
        checkNotClosed();
        connection.setAutoCommit(autoCommit);
    }

    @Override
    public void setCatalog(final String catalog) throws SQLException {
        LOGGER.trace("setCatalog(catalog={})", catalog);
        checkNotClosed();
        connection.setCatalog(catalog);
    }

    @Override
    public void setClientInfo(final Properties properties) throws SQLClientInfoException {
        LOGGER.trace("setClientInfo(properties={})", properties);
        checkNotClosedForClientInfo();
        connection.setClientInfo(properties);
    }

    @Override
    public void setClientInfo(final String name, final String value) throws SQLClientInfoException {
        LOGGER.trace("setClientInfo(name={},value={})", name, value);
        checkNotClosedForClientInfo();
        connection.setClientInfo(name, value);
    }

    @Override
    public void setHoldability(final int holdability) throws SQLException {
        LOGGER.trace("setHoldability(holdability={})", holdability);
        checkNotClosed();
        connection.setHoldability(holdability);
    }

    @Override
    public void setNetworkTimeout(final Executor executor, final int milliseconds) throws SQLException {
        LOGGER.trace("setNetworkTimeout(executor={},milliseconds={})", executor, milliseconds);
        checkNotClosed();
        connection.setNetworkTimeout(executor, milliseconds);
    }

    @Override
    public void setReadOnly(final boolean readOnly) throws SQLException {
        LOGGER.trace("setReadOnly(readOnly={})", readOnly);
        checkNotClosed();
        connection.setReadOnly(readOnly);
    }

    @Override
    public Savepoint setSavepoint() throws SQLException {
        LOGGER.trace("setSavepoint()");
        checkNotClosed();
        return connection.setSavepoint();
    }

    @Override
    public Savepoint setSavepoint(final String name) throws SQLException {
        LOGGER.trace("setSavepoint(name={})", name);
        checkNotClosed();
        return connection.setSavepoint(name);
    }

    @Override
    public void setSchema(final String schema) throws SQLException {
        LOGGER.trace("setSchema(schema={})", schema);
        checkNotClosed();
        connection.setSchema(schema);
    }

    @Override
    public void setTransactionIsolation(final int level) throws SQLException {
        LOGGER.trace("setTransactionIsolation(level={})", level);
        checkNotClosed();
        connection.setTransactionIsolation(level);
    }

    @Override
    public void setTypeMap(final Map<String, Class<?>> map) throws SQLException {
        LOGGER.trace("setTypeMap(map={})", map);
        checkNotClosed();
        connection.setTypeMap(map);
    }

}
