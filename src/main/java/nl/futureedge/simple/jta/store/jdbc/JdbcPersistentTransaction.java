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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class JdbcPersistentTransaction implements PersistentTransaction {

    private static final Logger LOGGER = LoggerFactory.getLogger(JdbcPersistentTransaction.class);

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
        LOGGER.debug("save(status={})", status);
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
                insertStatement.executeUpdate();
            }

            return null;
        });
    }

    @Override
    public void save(final TransactionStatus status, final long branchId, final String resourceManager) throws JtaTransactionStoreException {
        save(status, branchId, resourceManager, null);
    }

    @Override
    public void save(final TransactionStatus status, final long branchId, final String resourceManager, final Exception cause)
            throws JtaTransactionStoreException {
        LOGGER.debug("save(status={}, resourceManager={})", status, resourceManager, cause);
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
            updateStatement.setLong(5, branchId);
            updateStatement.setString(6, resourceManager);

            final int rows = updateStatement.executeUpdate();
            if (rows == 0) {
                final PreparedStatement insertStatement = connection.prepareStatement(sqlTemplate.insertResourceStatus());
                insertStatement.setLong(1, transactionId);
                insertStatement.setLong(2, branchId);
                insertStatement.setString(3, resourceManager);
                insertStatement.setString(4, status.getText());
                if (stackTrace == null) {
                    insertStatement.setNull(5, Types.CLOB);
                } else {
                    insertStatement.setString(5, stackTrace);
                }
                insertStatement.setDate(6, now);
                insertStatement.setDate(7, now);
                insertStatement.executeUpdate();
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
        LOGGER.debug("remove()");
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
        LOGGER.debug("getStatus()");
        return jdbc.doInConnection(connection -> {
            final PreparedStatement select = connection.prepareStatement(sqlTemplate.selectTransactionStatus());
            select.setLong(1, transactionId);
            final ResultSet resultSet = select.executeQuery();
            if (resultSet.next()) {
                return TransactionStatus.valueOf(resultSet.getString(1));
            } else {
                return null;
            }
        });
    }
}
