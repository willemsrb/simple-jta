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
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import nl.futureedge.simple.jta.store.JtaTransactionStoreException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Very simplistic connection pool.
 */
final class JdbcConnectionPool {

    private static final Logger LOGGER = LoggerFactory.getLogger(JdbcConnectionPool.class);

    private final ConnectionSupplier connectionSupplier;

    private final List<Connection> all = new ArrayList<>();
    private final List<Connection> available = new ArrayList<>();

    /**
     * Constructor.
     * @param driver jdbc driver class name
     * @param url jdbc url
     * @param user username
     * @param password password
     * @throws JtaTransactionStoreException thrown, when the driver could not be loaded or the connection could not be made
     */
    JdbcConnectionPool(final String driver, final String url, final String user, final String password) throws JtaTransactionStoreException {
        // Load driver
        if (driver != null && !"".equals(driver)) {
            try {
                Class.forName(driver);
            } catch (final ClassNotFoundException e) {
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

    /**
     * Close all connections; does not check if connections have been returned.
     */
    void close() {
        synchronized (available) {
            for (final Connection connection : all) {
                try {
                    connection.close();
                } catch (final SQLException e) {
                    LOGGER.warn("Could not close transaction store connection", e);
                }
            }
        }
    }

    /**
     * Borrow a connection from the pool.
     * @return connection
     * @throws JtaTransactionStoreException Thrown when a new connection could not be made
     */
    Connection borrowConnection() throws JtaTransactionStoreException {
        final Connection result;
        synchronized (available) {
            if (available.isEmpty()) {
                result = null;
            } else {
                result = available.remove(0);
            }
        }

        return result == null ? createConnection() : result;
    }

    /**
     * Return a connection to the pool.
     * @param connection connection
     */
    void returnConnection(final Connection connection) {
        synchronized (available) {
            available.add(connection);
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
