package nl.futureedge.simple.jta.store.jdbc;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import nl.futureedge.simple.jta.store.JtaTransactionStoreException;

final class JdbcHelper {

    private final String jdbcDriver;
    private final String jdbcUrl;
    private final String jdbcUser;
    private final String jdbcPassword;

    private Connection connection;

    JdbcHelper(final String jdbcDriver, final String jdbcUrl, final String jdbcUser, final String jdbcPassword) {
        this.jdbcDriver = jdbcDriver;
        this.jdbcUrl = jdbcUrl;
        this.jdbcUser = jdbcUser;
        this.jdbcPassword = jdbcPassword;
    }

    <T> T doInConnection(final JdbcFunction<T> returnable) throws JtaTransactionStoreException {
        synchronized (connection) {
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
        }
    }

    void executeInConnection(final JdbcStatementCallback statementCallback) throws JtaTransactionStoreException {
        doInConnection(theConnection -> {
            try (final Statement statement = theConnection.createStatement()) {
                statementCallback.apply(statement);
                return null;
            }
        });
    }

    int prepareAndExecuteUpdate(final Connection theConnection, final String sql, final JdbcPreparedStatementCallback statementCallback) throws SQLException {
        try (final PreparedStatement preparedStatement = theConnection.prepareStatement(sql)) {
            statementCallback.apply(preparedStatement);
            return preparedStatement.executeUpdate();
        }
    }

    <T> T prepareAndExecuteQuery(final Connection theConnection, final String sql, final JdbcPreparedStatementCallback statementCallback,
                                 final JdbcResultSetCallback<T> resultSetCallback) throws SQLException {
        try (final PreparedStatement preparedStatement = theConnection.prepareStatement(sql)) {
            statementCallback.apply(preparedStatement);
            try (final ResultSet resultSet = preparedStatement.executeQuery()) {
                return resultSetCallback.apply(resultSet);
            }
        }
    }

    void open() throws JtaTransactionStoreException {
        try {
            if (jdbcDriver != null && !"".equals(jdbcDriver)) {
                Class.forName(jdbcDriver);
            }
            if (jdbcUser != null && !"".equals(jdbcUser)) {
                connection = DriverManager.getConnection(jdbcUrl, jdbcUser, jdbcPassword);
            } else {
                connection = DriverManager.getConnection(jdbcUrl);
            }
            connection.setAutoCommit(false);
        } catch (ClassNotFoundException | SQLException e) {
            throw new JtaTransactionStoreException("Could not open connection for transaction logs", e);
        }
    }

    void close() throws JtaTransactionStoreException {
        try {
            connection.close();
        } catch (SQLException e) {
            throw new JtaTransactionStoreException("Could not close connection for transaction logs");
        }
        connection = null;
    }


    @FunctionalInterface
    public interface JdbcFunction<T> {
        T apply(final Connection connection) throws SQLException;
    }

    @FunctionalInterface
    public interface JdbcPreparedStatementCallback {
        void apply(final PreparedStatement preparedStatement) throws SQLException;
    }

    @FunctionalInterface
    public interface JdbcStatementCallback {
        void apply(final Statement statement) throws SQLException;
    }

    @FunctionalInterface
    public interface JdbcResultSetCallback<T> {
        T apply(final ResultSet resultSet) throws SQLException;
    }
}
