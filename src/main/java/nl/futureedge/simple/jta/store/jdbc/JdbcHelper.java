package nl.futureedge.simple.jta.store.jdbc;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import nl.futureedge.simple.jta.store.JtaTransactionStoreException;

final class JdbcHelper {

    private String jdbcDriver;
    private String jdbcUrl;
    private String jdbcUser;
    private String jdbcPassword;

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
                T result = returnable.apply(connection);
                connection.commit();
                return result;
            } catch (Exception e) {
                try {
                    connection.rollback();
                } catch (SQLException e2) {
                    // Ignore
                }
                throw new JtaTransactionStoreException("Could not execute SQL", e);
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
}
