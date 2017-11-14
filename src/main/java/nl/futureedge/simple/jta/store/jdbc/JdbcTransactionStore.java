package nl.futureedge.simple.jta.store.jdbc;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;
import nl.futureedge.simple.jta.store.JtaTransactionStoreException;
import nl.futureedge.simple.jta.store.impl.BaseTransactionStore;
import nl.futureedge.simple.jta.store.impl.PersistentTransaction;
import nl.futureedge.simple.jta.store.impl.TransactionStatus;
import nl.futureedge.simple.jta.store.jdbc.sql.JdbcSqlTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Required;

/**
 * JDBC back transaction store.
 */
public final class JdbcTransactionStore extends BaseTransactionStore implements InitializingBean, DisposableBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(JdbcTransactionStore.class);

    private boolean create = false;

    private String jdbcDriver;
    private String jdbcUrl;
    private String jdbcUser;
    private String jdbcPassword;

    private JdbcConnectionPool pool;
    private JdbcSqlTemplate sqlTemplate;

    /**
     * Enables execution of DDL to create database objects during startup (default disabled).
     * @param create true, to create database objects during startup
     */
    public void setCreate(final boolean create) {
        this.create = create;
    }

    /**
     * Set the classname of the JDBC database driver to load (can be left empty for JDBC 4.0+ drivers).
     * @param jdbcDriver JDBC driver class name
     */
    public void setDriver(final String jdbcDriver) {
        this.jdbcDriver = jdbcDriver;
    }

    /**
     * Set the JDBC url.
     * @param jdbcUrl JDBC url
     */
    @Required
    public void setUrl(final String jdbcUrl) {
        this.jdbcUrl = jdbcUrl;
    }

    /**
     * Set the username to connect.
     * @param jdbcUser username
     */
    public void setUser(final String jdbcUser) {
        this.jdbcUser = jdbcUser;
    }

    /**
     * Set the password to connect (only used if username is filled).
     * @param jdbcPassword password
     */
    public void setPassword(final String jdbcPassword) {
        this.jdbcPassword = jdbcPassword;
    }

    /**
     * Set the SQL template to use (autodetect based on JDBC url if left empty).
     * @param sqlTemplate SQL template
     */
    public void setSqlTemplate(final JdbcSqlTemplate sqlTemplate) {
        this.sqlTemplate = sqlTemplate;
    }

    /* ************************** */
    /* *** STARTUP/SHUTDOWN ***** */
    /* ************************** */

    @Override
    public void afterPropertiesSet() throws Exception {
        pool = new JdbcConnectionPool(jdbcDriver, jdbcUrl, jdbcUser, jdbcPassword);

        if (sqlTemplate == null) {
            sqlTemplate = JdbcSqlTemplate.determineSqlTemplate(jdbcUrl);
        }

        if (create) {
            LOGGER.debug("Creating tables");
            try {
                JdbcHelper.doInConnection(pool, null,
                        connection -> {
                            JdbcDatabaseInitializer.create(connection, sqlTemplate);
                            return null;
                        }
                );
            } catch (JtaTransactionStoreException e) {
                LOGGER.info("Could not create transaction tables; ignoring exception...", e.getCause());
            }
        }
    }

    @Override
    public void doDestroy() {
        pool.close();
    }

    /* ************************** */
    /* *** CLEANUP ************** */
    /* ************************** */

    @Override
    public void cleanup() throws JtaTransactionStoreException {
        JdbcHelper.doInConnection(pool, null, connection -> {
            JdbcHelper.prepareAndExecuteQuery(
                    connection,
                    sqlTemplate.selectTransactionIdAndStatus(),
                    transactionsStatement -> { /* No statement parameters */ },
                    transactionsResult -> {
                        while (transactionsResult.next()) {
                            final Long transactionId = transactionsResult.getLong(1);
                            final TransactionStatus transactionStatus = TransactionStatus.valueOf(transactionsResult.getString(2));

                            if (CLEANABLE.containsKey(transactionStatus) && isCleanable(connection, transactionId, CLEANABLE.get(transactionStatus))) {
                                cleanTransaction(connection, transactionId);
                            }
                        }
                        return null;
                    });
            return null;
        });
    }

    private boolean isCleanable(final Connection connection, final Long transactionId, final Collection<TransactionStatus> allowedResourceStatuses)
            throws SQLException {
        return JdbcHelper.prepareAndExecuteQuery(
                connection,
                sqlTemplate.selectResourceStatus(),
                resourcesStatement -> resourcesStatement.setLong(1, transactionId),
                resourcesResult -> {
                    while (resourcesResult.next()) {
                        final TransactionStatus resourceStatus = TransactionStatus.valueOf(resourcesResult.getString(1));
                        if (!allowedResourceStatuses.contains(resourceStatus)) {
                            return false;
                        }
                    }
                    return true;
                });
    }

    private void cleanTransaction(final Connection connection, final Long transactionId) throws SQLException {
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
    }

    /* ************************** */
    /* *** PERSISTENCE ********** */
    /* ************************** */

    @Override
    public long nextTransactionId() throws JtaTransactionStoreException {
        LOGGER.debug("nextTransactionId()");
        return JdbcHelper.doInConnection(pool, null,
                connection -> JdbcHelper.prepareAndExecuteQuery(connection,
                        sqlTemplate.selectNextTransactionId(),
                        ps -> { /* No statement parameters */ },
                        resultSet -> {
                            if (!resultSet.next()) {
                                throw new SQLException("No row returned from sequence select statement");
                            }
                            return resultSet.getLong(1);
                        })
        );
    }

    @Override
    protected PersistentTransaction createPersistentTransaction(final long transactionId) throws JtaTransactionStoreException {
        return new JdbcPersistentTransaction(pool, sqlTemplate, transactionId);
    }
}
