package nl.futureedge.simple.jta.store.jdbc;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import nl.futureedge.simple.jta.store.JtaTransactionStoreException;

/**
 * JDBC utilities.
 */
final class JdbcHelper {

    private JdbcHelper() {
        throw new IllegalStateException("Class should not be instantiated");
    }

    /**
     * Execute in a connection.
     * @param pool pool to use if no connection was given
     * @param connectionToUse connection to use (can be null)
     * @param returnable code to execute
     * @param <T> return type
     * @return the result from the code to execute
     * @throws JtaTransactionStoreException Thrown if an unexpected error has occurred
     */
    static <T> T doInConnection(final JdbcConnectionPool pool, final Connection connectionToUse, final JdbcFunction<T> returnable)
            throws JtaTransactionStoreException {
        final Connection connection = connectionToUse == null ? pool.borrowConnection() : connectionToUse;
        try {
            try {
                final T result = returnable.apply(connection);
                connection.commit();
                return result;
            } catch (final Exception e) {
                try {
                    connection.rollback();
                } catch (final SQLException e2) {
                    // Ignore
                }
                throw new JtaTransactionStoreException("Could not execute SQL", e);
            }
        } finally {
            if (connectionToUse == null) {
                pool.returnConnection(connection);
            }
        }
    }

    /**
     * Execute within a statement.
     * @param connection connection to use
     * @param statementCallback code to execute
     * @throws SQLException Thrown if an unexpected error has occurred
     */
    static void doInStatement(final Connection connection, final JdbcStatementCallback statementCallback) throws SQLException {
        try (final Statement statement = connection.createStatement()) {
            statementCallback.apply(statement);
        }
    }

    /**
     * Prepare a statement and execute it as an update.
     * @param connection connection to use
     * @param sql sql to prepare
     * @param statementCallback code to set statement parameters
     * @return row count (result of {@link PreparedStatement#executeUpdate})
     * @throws SQLException Thrown if an unexpected error has occurred
     */
    static int prepareAndExecuteUpdate(final Connection connection, final String sql, final JdbcPreparedStatementCallback statementCallback)
            throws SQLException {
        try (final PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            statementCallback.apply(preparedStatement);
            return preparedStatement.executeUpdate();
        }
    }

    /**
     * Prepare a statement and execute it as a query.
     * @param connection connection to use
     * @param sql sql to prepare
     * @param statementCallback code to set statement parameters
     * @param resultSetCallback code to handle the result set
     * @param <T> result type
     * @return result of the code to handle the result set
     * @throws SQLException Thrown if an unexpected error has occurred
     */
    static <T> T prepareAndExecuteQuery(final Connection connection, final String sql, final JdbcPreparedStatementCallback statementCallback,
                                        final JdbcResultSetCallback<T> resultSetCallback) throws SQLException {
        try (final PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            statementCallback.apply(preparedStatement);
            try (final ResultSet resultSet = preparedStatement.executeQuery()) {
                return resultSetCallback.apply(resultSet);
            }
        }
    }

    public static ConnectionSupplier createConnectionSupplier(String driver, String url, String user, String password) throws ClassNotFoundException {
        // Load driver
        if (driver != null && !"".equals(driver)) {
            Class.forName(driver);
        }

        // Supplier
        if (user != null && !"".equals(user)) {
            return () -> DriverManager.getConnection(url, user, password);
        } else {
            return () -> DriverManager.getConnection(url);
        }
    }

    /**
     * Connection supplier.
     */
    @FunctionalInterface
    public interface ConnectionSupplier {
        Connection getConnection() throws SQLException;
    }

    /**
     * Code to execute in a connection.
     * @param <T> return type
     */
    @FunctionalInterface
    interface JdbcFunction<T> {
        T apply(final Connection connection) throws SQLException;
    }

    /**
     * Code to execute in a statement.
     */
    @FunctionalInterface
    interface JdbcStatementCallback {
        void apply(final Statement statement) throws SQLException;
    }

    /**
     * Code to set statement parameters.
     */
    @FunctionalInterface
    interface JdbcPreparedStatementCallback {
        void apply(final PreparedStatement preparedStatement) throws SQLException;
    }

    /**
     * Code to handle a result set.
     * @param <T> return type
     */
    @FunctionalInterface
    interface JdbcResultSetCallback<T> {
        T apply(final ResultSet resultSet) throws SQLException;
    }
}
