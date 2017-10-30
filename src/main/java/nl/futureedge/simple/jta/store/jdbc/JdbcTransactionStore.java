package nl.futureedge.simple.jta.store.jdbc;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import nl.futureedge.simple.jta.store.JtaTransactionStoreException;
import nl.futureedge.simple.jta.store.impl.BaseTransactionStore;
import nl.futureedge.simple.jta.store.impl.PersistentTransaction;
import nl.futureedge.simple.jta.store.impl.TransactionStatus;
import nl.futureedge.simple.jta.store.jdbc.sql.DefaultSqlTemplate;
import nl.futureedge.simple.jta.store.jdbc.sql.HsqldbSqlTemplate;
import nl.futureedge.simple.jta.store.jdbc.sql.JdbcSqlTemplate;
import nl.futureedge.simple.jta.xid.JtaXid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Required;

public final class JdbcTransactionStore extends BaseTransactionStore implements InitializingBean, DisposableBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(JdbcTransactionStore.class);

    private static final Pattern JDBC_URL_PATTERN = Pattern.compile("^jdbc:([a-z]+):.*");

    private boolean create = false;

    private String jdbcDriver;
    private String jdbcUrl;
    private String jdbcUser;
    private String jdbcPassword;

    private JdbcHelper jdbc;
    private JdbcSqlTemplate sqlTemplate;

    public void setCreate(final boolean create) {
        this.create = create;
    }

    public void setDriver(final String jdbcDriver) {
        this.jdbcDriver = jdbcDriver;
    }

    @Required
    public void setUrl(final String jdbcUrl) {
        this.jdbcUrl = jdbcUrl;
    }

    public void setUser(final String jdbcUser) {
        this.jdbcUser = jdbcUser;
    }

    public void setPassword(final String jdbcPassword) {
        this.jdbcPassword = jdbcPassword;
    }

    public void setSqlTemplate(final JdbcSqlTemplate sqlTemplate) {
        this.sqlTemplate = sqlTemplate;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        jdbc = new JdbcHelper(jdbcDriver, jdbcUrl, jdbcUser, jdbcPassword);
        jdbc.open();

        if (sqlTemplate == null) {
            sqlTemplate = determineSqlTemplate(jdbcUrl);
        }

        if (create) {
            LOGGER.debug("Creating tables");
            try {
                jdbc.executeInConnection(statement -> {
                    statement.execute(sqlTemplate.createTransactionIdSequence());
                    statement.execute(sqlTemplate.createTransactionTable());
                    statement.execute(sqlTemplate.createResourceTable());
                });
            } catch (JtaTransactionStoreException e) {
                LOGGER.info("Could not create transaction tables; ignoring exception...", e.getCause());
            }
        }
    }

    @Override
    public void destroy() throws Exception {
        jdbc.close();
    }

    private static JdbcSqlTemplate determineSqlTemplate(final String url) {
        final Matcher urlMatcher = JDBC_URL_PATTERN.matcher(url);
        final String driver = urlMatcher.matches() ? urlMatcher.group(1) : "unknown";

        switch (driver) {
            case "hsqldb":
                LOGGER.info("Using HSQLDB SQL template (url detected)");
                return new HsqldbSqlTemplate();
            default:
                LOGGER.info("Using default SQL template");
                return new DefaultSqlTemplate();
        }
    }

    /* ************************** */
    /* *** CLEANUP ************** */
    /* ************************** */

    @Override
    public void cleanup() throws JtaTransactionStoreException {
        jdbc.doInConnection(connection -> {
            jdbc.prepareAndExecuteQuery(
                    connection,
                    sqlTemplate.selectTransactionIdAndStatus(),
                    transactionsStatement -> {},
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
        return jdbc.prepareAndExecuteQuery(
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
        jdbc.prepareAndExecuteUpdate(
                connection,
                sqlTemplate.deleteResourceStatus(),
                deleteResources -> deleteResources.setLong(1, transactionId)
        );
        jdbc.prepareAndExecuteUpdate(
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
        return jdbc.doInConnection(
                connection -> jdbc.prepareAndExecuteQuery(connection,
                        sqlTemplate.selectNextTransactionId(),
                        ps -> {},
                        resultSet -> {
                            if (!resultSet.next()) {
                                throw new SQLException("No row returned from sequence select statement");
                            }
                            return resultSet.getLong(1);
                        })
        );
    }

    @Override
    protected PersistentTransaction getPersistentTransaction(final JtaXid xid) throws JtaTransactionStoreException {
        return new JdbcPersistentTransaction(jdbc, sqlTemplate, xid.getTransactionId());
    }

}
