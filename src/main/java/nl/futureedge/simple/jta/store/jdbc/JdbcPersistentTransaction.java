package nl.futureedge.simple.jta.store.jdbc;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.Date;
import java.sql.Types;
import nl.futureedge.simple.jta.store.JtaTransactionStoreException;
import nl.futureedge.simple.jta.store.impl.PersistentTransaction;
import nl.futureedge.simple.jta.store.impl.TransactionStatus;
import nl.futureedge.simple.jta.store.jdbc.sql.JdbcSqlTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JDBC persistent transaction information.
 */
final class JdbcPersistentTransaction implements PersistentTransaction {

    private static final Logger LOGGER = LoggerFactory.getLogger(JdbcPersistentTransaction.class);

    private final JdbcConnectionPool pool;
    private final JdbcSqlTemplate sqlTemplate;
    private final long transactionId;

    private Connection reservedConnection;
    private boolean hasSaved;

    public JdbcPersistentTransaction(final JdbcConnectionPool pool, final JdbcSqlTemplate sqlTemplate, final long transactionId) {
        this.pool = pool;
        this.sqlTemplate = sqlTemplate;
        this.transactionId = transactionId;
    }


    @Override
    public void save(final TransactionStatus status) throws JtaTransactionStoreException {
        LOGGER.debug("save(status={})", status);

        // From preparing the updates should come in quick succession; reserve a 'private' connection until the transaction is closed
        if (status == TransactionStatus.PREPARING) {
            reservedConnection = pool.borrowConnection();
        }

        hasSaved = true;
        final Date now = new Date(System.currentTimeMillis());

        JdbcHelper.doInConnection(pool, reservedConnection, connection -> {
            final int rows = JdbcHelper.prepareAndExecuteUpdate(connection, sqlTemplate.updateTransactionStatus(), updateStatement -> {
                updateStatement.setString(1, status.getText());
                updateStatement.setDate(2, now);
                updateStatement.setLong(3, transactionId);
            });

            if (rows == 0) {
                JdbcHelper.prepareAndExecuteUpdate(connection, sqlTemplate.insertTransactionStatus(), insertStatement -> {
                    insertStatement.setLong(1, transactionId);
                    insertStatement.setString(2, status.getText());
                    insertStatement.setDate(3, now);
                    insertStatement.setDate(4, now);
                });
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

        JdbcHelper.doInConnection(pool, reservedConnection, connection -> {
            final int rows = JdbcHelper.prepareAndExecuteUpdate(connection, sqlTemplate.updateResourceStatus(), updateStatement -> {
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
            });

            if (rows == 0) {
                JdbcHelper.prepareAndExecuteUpdate(connection, sqlTemplate.insertResourceStatus(), insertStatement -> {
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
                });
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
    public void close() {
        if (reservedConnection != null) {
            pool.releaseConnection(reservedConnection);
            reservedConnection = null;
        }
    }

    @Override
    public void remove() throws JtaTransactionStoreException {
        LOGGER.debug("remove()");
        if (hasSaved) {
            JdbcHelper.doInConnection(pool, reservedConnection, connection -> {
                JdbcHelper.prepareAndExecuteUpdate(
                        connection,
                        sqlTemplate.deleteResourceStatus(),
                        deleteResources -> deleteResources.setLong(1, transactionId)
                );
                JdbcHelper.prepareAndExecuteUpdate(
                        connection,
                        sqlTemplate.deleteTransactionStatus(),
                        deleteTransaction -> deleteTransaction.setLong(1, transactionId)
                );
                return null;
            });
        }
    }

    @Override
    public TransactionStatus getStatus() throws JtaTransactionStoreException {
        LOGGER.debug("getStatus()");
        return JdbcHelper.doInConnection(pool, reservedConnection, connection ->
                JdbcHelper.prepareAndExecuteQuery(
                        connection,
                        sqlTemplate.selectTransactionStatus(),
                        selectStatement -> selectStatement.setLong(1, transactionId),
                        selectResult -> {
                            if (selectResult.next()) {
                                return TransactionStatus.valueOf(selectResult.getString(1));
                            } else {
                                return null;
                            }
                        })
        );
    }
}
