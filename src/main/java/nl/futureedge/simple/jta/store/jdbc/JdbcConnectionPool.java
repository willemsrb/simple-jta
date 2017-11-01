package nl.futureedge.simple.jta.store.jdbc;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import nl.futureedge.simple.jta.store.JtaTransactionStoreException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class JdbcConnectionPool {

    private static final Logger LOGGER = LoggerFactory.getLogger(JdbcConnectionPool.class);

    private final ConnectionSupplier connectionSupplier;

    private final List<Connection> all = new ArrayList<>();
    private final List<Connection> available = new ArrayList<>();


    public JdbcConnectionPool(final String driver, final String url, final String user, final String password) throws JtaTransactionStoreException {
        // Load driver
        if (driver != null && !"".equals(driver)) {
            try {
                Class.forName(driver);
            } catch (ClassNotFoundException e) {
                throw new JtaTransactionStoreException("Could not load transaction store driver", e);
            }
        }

        // Supplier
        if (user != null && !"".equals(user)) {
            connectionSupplier = () -> DriverManager.getConnection(url, user, password);
        } else {
            connectionSupplier = () -> DriverManager.getConnection(url);
        }

        available.add(createConnection());
    }

    public void close() {
        synchronized (available) {
            for (Connection connection : all) {
                try {
                    connection.close();
                } catch (SQLException e) {
                    LOGGER.warn("Could not close transaction store connection", e);
                }
            }
        }
    }


    public void releaseConnection(final Connection connection) {
        synchronized (available) {
            available.add(connection);
        }
    }

    public Connection borrowConnection() throws JtaTransactionStoreException {
        synchronized (available) {
            if (available.isEmpty()) {
                return createConnection();
            } else {
                return available.remove(0);
            }
        }
    }

    private Connection createConnection() throws JtaTransactionStoreException {
        try {
            final Connection connection = connectionSupplier.getConnection();
            connection.setAutoCommit(false);
            all.add(connection);
            return connection;
        } catch (SQLException e) {
            throw new JtaTransactionStoreException("Could not open connection for transaction logs", e);
        }
    }

    @FunctionalInterface
    private interface ConnectionSupplier {
        Connection getConnection() throws SQLException;
    }
}
