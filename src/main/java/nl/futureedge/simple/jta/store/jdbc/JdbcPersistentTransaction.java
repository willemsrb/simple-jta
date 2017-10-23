package nl.futureedge.simple.jta.store.jdbc;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Types;
import nl.futureedge.simple.jta.store.JtaTransactionStoreException;
import nl.futureedge.simple.jta.store.impl.PersistentTransaction;
import nl.futureedge.simple.jta.store.impl.TransactionStatus;
import nl.futureedge.simple.jta.store.jdbc.sql.JdbcSqlTemplate;

final class JdbcPersistentTransaction implements PersistentTransaction {

    private final JdbcHelper jdbc;
    private final JdbcSqlTemplate sqlTemplate;
    private final long transactionId;

    public JdbcPersistentTransaction(final JdbcHelper jdbc, final JdbcSqlTemplate sqlTemplate, final long transactionId) {
        this.jdbc = jdbc;
        this.sqlTemplate = sqlTemplate;
        this.transactionId = transactionId;
    }

    @Override
    public void save(final TransactionStatus status) throws JtaTransactionStoreException {
        final Date now = new Date(System.currentTimeMillis());

        jdbc.doInConnection(connection -> {
            final PreparedStatement updateStatement = connection.prepareStatement(sqlTemplate.updateTransactionStatus());
            updateStatement.setString(1, status.getText());
            updateStatement.setDate(2, now);
            updateStatement.setLong(3, transactionId);

            final int rows = updateStatement.executeUpdate();
            if (rows == 0) {
                final PreparedStatement insertStatement = connection.prepareStatement(sqlTemplate.insertTransactionStatus());
                insertStatement.setLong(1, transactionId);
                insertStatement.setString(2, status.getText());
                insertStatement.setDate(3, now);
                insertStatement.setDate(4, now);
            }

            return null;
        });
    }

    @Override
    public void save(TransactionStatus status, String resourceManager) throws JtaTransactionStoreException {
        save(status, resourceManager, null);
    }

    @Override
    public void save(final TransactionStatus status, final String resourceManager, final Exception cause) throws JtaTransactionStoreException {
        final Date now = new Date(System.currentTimeMillis());
        final String stackTrace = printStackTrace(cause);

        jdbc.doInConnection(connection -> {
            final PreparedStatement updateStatement = connection.prepareStatement(sqlTemplate.updateResourceStatus());
            updateStatement.setString(1, status.getText());
            if (stackTrace == null) {
                updateStatement.setNull(2, Types.CLOB);
            } else {
                updateStatement.setString(2, stackTrace);
            }
            updateStatement.setDate(3, now);
            updateStatement.setLong(4, transactionId);
            updateStatement.setString(5, resourceManager);

            final int rows = updateStatement.executeUpdate();
            if (rows == 0) {
                final PreparedStatement insertStatement = connection.prepareStatement(sqlTemplate.insertResourceStatus());
                insertStatement.setLong(1, transactionId);
                insertStatement.setString(2, resourceManager);
                insertStatement.setString(3, status.getText());
                if (stackTrace == null) {
                    insertStatement.setNull(4, Types.CLOB);
                } else {
                    insertStatement.setString(4, stackTrace);
                }
                insertStatement.setDate(5, now);
                insertStatement.setDate(6, now);
            }

            return null;
        });
    }

    private String printStackTrace(final Exception cause) {
        if (cause == null) {
            return null;
        }
        final StringWriter result = new StringWriter();
        try (final PrintWriter writer = new PrintWriter(result)) {
            cause.printStackTrace(writer);
        }
        return result.toString();
    }

    @Override
    public void remove() throws JtaTransactionStoreException {
        jdbc.doInConnection(connection -> {
            final PreparedStatement deleteResources = connection.prepareStatement(sqlTemplate.deleteResourceStatus());
            deleteResources.setLong(1, transactionId);
            deleteResources.executeUpdate();
            final PreparedStatement deleteTrans = connection.prepareStatement(sqlTemplate.deleteTransactionStatus());
            deleteTrans.setLong(1, transactionId);
            deleteTrans.executeUpdate();
            return null;
        });
    }

    @Override
    public TransactionStatus getStatus() throws JtaTransactionStoreException {
        return jdbc.doInConnection(connection -> {
            final PreparedStatement select = connection.prepareStatement(sqlTemplate.selectTransactionStatus());
            final ResultSet resultSet = select.executeQuery();
            if (resultSet.next()) {
                return TransactionStatus.fromText(resultSet.getString(1));
            } else {
                return null;
            }
        });
    }
}
