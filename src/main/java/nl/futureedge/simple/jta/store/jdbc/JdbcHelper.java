package nl.futureedge.simple.jta.store.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import nl.futureedge.simple.jta.store.JtaTransactionStoreException;

/**
 * JDBC utilities.
 */
final class JdbcHelper {


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
                pool.releaseConnection(connection);
            }
        }
    }

    static void doInStatement(final Connection connection, final JdbcStatementCallback statementCallback) throws SQLException {
        try (final Statement statement = connection.createStatement()) {
            statementCallback.apply(statement);
        }
    }


    static int prepareAndExecuteUpdate(final Connection theConnection, final String sql, final JdbcPreparedStatementCallback statementCallback)
            throws SQLException {
        try (final PreparedStatement preparedStatement = theConnection.prepareStatement(sql)) {
            statementCallback.apply(preparedStatement);
            return preparedStatement.executeUpdate();
        }
    }

    static <T> T prepareAndExecuteQuery(final Connection theConnection, final String sql, final JdbcPreparedStatementCallback statementCallback,
                                        final JdbcResultSetCallback<T> resultSetCallback) throws SQLException {
        try (final PreparedStatement preparedStatement = theConnection.prepareStatement(sql)) {
            statementCallback.apply(preparedStatement);
            try (final ResultSet resultSet = preparedStatement.executeQuery()) {
                return resultSetCallback.apply(resultSet);
            }
        }
    }

    @FunctionalInterface
    public interface JdbcFunction<T> {
        T apply(final Connection connection) throws SQLException;
    }


    @FunctionalInterface
    public interface JdbcStatementCallback {
        void apply(final Statement sStatement) throws SQLException;
    }


    @FunctionalInterface
    public interface JdbcPreparedStatementCallback {
        void apply(final PreparedStatement preparedStatement) throws SQLException;
    }


    @FunctionalInterface
    public interface JdbcResultSetCallback<T> {
        T apply(final ResultSet resultSet) throws SQLException;
    }
}
