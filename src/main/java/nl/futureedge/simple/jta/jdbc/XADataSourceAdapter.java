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

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.List;
import javax.sql.DataSource;
import javax.sql.XAConnection;
import javax.sql.XADataSource;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import nl.futureedge.simple.jta.JtaTransaction;
import nl.futureedge.simple.jta.JtaTransactionManager;
import nl.futureedge.simple.jta.xa.XAResourceAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Required;

/**
 * XADataSource adapter; delegates all calls to the wrapped xa datasource.
 *
 * Override the {@link #getConnection()} and {@link #getConnection(String, String)} methods to retrieve a xa connection, enlist the XAResource to the
 * transaction and return the (wrapped) connection.
 */
public final class XADataSourceAdapter implements DataSource, InitializingBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(XADataSourceAdapter.class);

    private String uniqueName;
    private XADataSource xaDataSource;
    private JtaTransactionManager jtaTransactionManager;

    private boolean supportsJoin = false;
    private boolean supportsSuspend = false;
    private AllowNonTransactedConnections allowNonTransactedConnections = AllowNonTransactedConnections.WARN;


    /**
     * Set unique name to use for this xa resource (manager).
     *
     * This must be unique with the set of xa resources that are used with a transaction manager . This must be consistent between session for recovery (commit
     * or rollback after crash) to work.
     * @param uniqueName unique name
     */
    @Required
    public void setUniqueName(final String uniqueName) {
        this.uniqueName = uniqueName;
    }

    /**
     * Set the xa datasource to wrap.
     * @param xaDataSource xa datasource
     */
    @Required
    public void setXaDataSource(final XADataSource xaDataSource) {
        this.xaDataSource = xaDataSource;
    }

    /**
     * Set the jta transaction manager to use.
     * @param jtaTransactionManager jta transaction manager
     */
    @Required
    @Autowired
    public void setJtaTransactionManager(final JtaTransactionManager jtaTransactionManager) {
        this.jtaTransactionManager = jtaTransactionManager;
    }

    /**
     * Enables support for the JTA resource joining (default disabled).
     * @param supportsJoin true, if the resource correctly supports joining
     */
    public void setSupportsJoin(final boolean supportsJoin) {
        this.supportsJoin = supportsJoin;
    }

    /**
     * Enables support for the JTA resource suspension (default disabled).
     * @param supportsSuspend true, if the resource correctly supports suspend/resume
     */
    public void setSupportsSuspend(final boolean supportsSuspend) {
        this.supportsSuspend = supportsSuspend;
    }

    /**
     * Determines if connections outside a transaction are allowed (yes, no, warn).
     * @param allowNonTransactedConnections allowed non-transacted connections
     */
    public void setAllowNonTransactedConnections(final String allowNonTransactedConnections) {
        this.allowNonTransactedConnections = allowNonTransactedConnections == null ? AllowNonTransactedConnections.WARN :
                AllowNonTransactedConnections.valueOf(allowNonTransactedConnections.toUpperCase());
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        // Execute recovery
        jtaTransactionManager.recover(new XAResourceAdapter(uniqueName, false, false, xaDataSource.getXAConnection().getXAResource()));
    }

    /* ******************************************************** */
    /* ******************************************************** */
    /* ******************************************************** */

    @Override
    public PrintWriter getLogWriter() throws SQLException {
        LOGGER.trace("getLogWriter()");
        return xaDataSource.getLogWriter();
    }

    @Override
    public int getLoginTimeout() throws SQLException {
        LOGGER.trace("getLoginTimeout()");
        return xaDataSource.getLoginTimeout();
    }

    @Override
    public java.util.logging.Logger getParentLogger() throws SQLFeatureNotSupportedException {
        LOGGER.trace("getParentLogger()");
        return xaDataSource.getParentLogger();
    }

    @Override
    public void setLogWriter(final PrintWriter out) throws SQLException {
        LOGGER.trace("setLogWriter(out={})", out);
        xaDataSource.setLogWriter(out);
    }

    @Override
    public void setLoginTimeout(final int seconds) throws SQLException {
        LOGGER.trace("setLoginTimeout(seconds={})", seconds);
        xaDataSource.setLoginTimeout(seconds);
    }

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

    /* ******************************************************** */
    /* ******************************************************** */
    /* ******************************************************** */

    private Object createConnectionKey(final String username) {
        return "user-" + username;
    }

    private XAConnectionAdapter reopenConnectionIfPossible(final List<XAConnectionAdapter> connections) throws SQLException {
        if (connections != null) {
            for (final XAConnectionAdapter connection : connections) {
                if (connection.reopen()) {
                    return connection;
                }
            }
        }
        return null;
    }

    @Override
    public synchronized Connection getConnection() throws SQLException {
        LOGGER.trace("getConnection()");
        return getConnection(createConnectionKey(null), () -> xaDataSource.getXAConnection());
    }

    @Override
    public synchronized Connection getConnection(final String username, final String password) throws SQLException {
        LOGGER.trace("getConnection(username={}, password not logged)", username, password);
        return getConnection(createConnectionKey(username), () -> xaDataSource.getXAConnection(username, password));
    }

    private Connection getConnection(final Object connectionKey, final XaConnectionSupplier xaConnectionSupplier) throws SQLException {
        final JtaTransaction transaction = jtaTransactionManager.getTransaction();

        if (transaction == null) {
            if (AllowNonTransactedConnections.NO == allowNonTransactedConnections) {
                throw new SQLException("Connection outside transaction not allowed");
            } else {
                if (AllowNonTransactedConnections.WARN == allowNonTransactedConnections) {
                    LOGGER.warn("XADataSource returned connection outside transaction");
                } else {
                    LOGGER.debug("XADataSource returned connection outside transaction");
                }
                return xaConnectionSupplier.getXAConnection().getConnection();
            }
        }

        final List<XAConnectionAdapter> connections = transaction.getConnections(connectionKey);
        final XAConnectionAdapter reopened = reopenConnectionIfPossible(connections);

        if (reopened != null) {
            LOGGER.debug("XADataSource returned previously closed (but not committed) connection");
            return reopened;
        }

        // Create a new XA connection
        final XAConnection xaConnection = xaConnectionSupplier.getXAConnection();

        // Enlist the xa resource in the current transaction
        try {
            transaction.enlistResource(new XAResourceAdapter(uniqueName, supportsJoin, supportsSuspend, xaConnection.getXAResource()));
        } catch (IllegalStateException | RollbackException | SystemException e) {
            LOGGER.debug("Could not enlist connection to transaction", e);
            throw new SQLException("Could not enlist connection to transaction", e);
        }

        // Wrap and register connection
        final XAConnectionAdapter connection = new XAConnectionAdapter(xaConnection);
        transaction.registerSystemCallback(connection);
        transaction.registerConnection(connectionKey, connection);

        return connection;
    }

    /**
     * XA Connection supplier.
     */
    @FunctionalInterface
    private interface XaConnectionSupplier {

        /**
         * @return xa connection
         * @throws SQLException when the supplier encounters an unexpected condition
         */
        XAConnection getXAConnection() throws SQLException;
    }

}
