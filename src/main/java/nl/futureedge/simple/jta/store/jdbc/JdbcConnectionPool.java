package nl.futureedge.simple.jta.store.jdbc;

import com.mchange.v2.c3p0.ComboPooledDataSource;
import com.mchange.v2.c3p0.ConnectionCustomizer;
import java.beans.PropertyVetoException;
import java.sql.Connection;
import java.sql.SQLException;
import nl.futureedge.simple.jta.store.JtaTransactionStoreException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Connection pool.
 */
final class JdbcConnectionPool {

    private static final Logger LOGGER = LoggerFactory.getLogger(JdbcConnectionPool.class);

    private final ComboPooledDataSource cpds;


    /**
     * Constructor.
     * @param driver jdbc driver class name
     * @param url jdbc url
     * @param user username
     * @param password password
     * @throws JtaTransactionStoreException thrown, when the driver could not be loaded or the connection could not be made
     */
    JdbcConnectionPool(final String driver, final String url, final String user, final String password) throws JtaTransactionStoreException {
        cpds = new ComboPooledDataSource();

        // Connection
        try {
            cpds.setDriverClass("".equals(driver) ? null : driver);
        } catch (PropertyVetoException e) {
            throw new JtaTransactionStoreException("Could not load JDBC driver class", e);
        }
        cpds.setJdbcUrl(url);
        if (user != null && !"".equals(user)) {
            cpds.setUser(user);
            cpds.setPassword(password);
        }

        // Enable statement caching
        cpds.setMaxStatementsPerConnection(30);

        // Try to acquire connections for 30 seconds
        cpds.setAcquireIncrement(5);
        cpds.setAcquireRetryAttempts(30);
        cpds.setAcquireRetryDelay(100);

        // Pool
        cpds.setInitialPoolSize(5);
        cpds.setMaxPoolSize(25);
        cpds.setMinPoolSize(5);
        cpds.setMaxIdleTimeExcessConnections(300);

        // Connection testing
        cpds.setTestConnectionOnCheckout(false);
        cpds.setTestConnectionOnCheckin(true);
        cpds.setIdleConnectionTestPeriod(30);

        // Set autocommit off
        cpds.setConnectionCustomizerClassName(PoolConnectionCustomizer.class.getName());
    }

    /**
     * Close all connections; does not check if connections have been returned.
     */
    void close() {
        cpds.close();
    }

    /**
     * Borrow a connection from the pool.
     * @return connection
     * @throws JtaTransactionStoreException Thrown when a new connection could not be made
     */
    Connection borrowConnection() throws JtaTransactionStoreException {
        try {
            return cpds.getConnection();
        } catch (final SQLException e) {
            throw new JtaTransactionStoreException("Could not get connection", e);
        }
    }

    /**
     * Return a connection to the pool.
     * @param connection connection
     */
    void returnConnection(final Connection connection) {
        try {
            connection.close();
        } catch (final SQLException e) {
            LOGGER.warn("Could not return connection to pool", e);
        }
    }

    public static class PoolConnectionCustomizer implements ConnectionCustomizer {

        @Override
        public void onAcquire(Connection connection, String parentDataSourceIdentityToken) throws SQLException {
            // Nothing
        }

        @Override
        public void onDestroy(Connection connection, String parentDataSourceIdentityToken) {
            // Nothing
        }

        @Override
        public void onCheckOut(Connection connection, String parentDataSourceIdentityToken) throws SQLException {
            connection.setAutoCommit(false);
        }

        @Override
        public void onCheckIn(Connection connection, String parentDataSourceIdentityToken) {
            // Nothing
        }
    }
}
