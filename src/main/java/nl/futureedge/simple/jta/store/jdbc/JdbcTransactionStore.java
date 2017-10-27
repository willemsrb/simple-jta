package nl.futureedge.simple.jta.store.jdbc;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
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
                jdbc.doInConnection(connection -> {
                    try (final Statement statement = connection.createStatement()) {
                        statement.execute(sqlTemplate.createTransactionIdSequence());
                        statement.execute(sqlTemplate.createTransactionTable());
                        statement.execute(sqlTemplate.createResourceTable());
                    }
                    return null;
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

    private static final Map<TransactionStatus, List<TransactionStatus>> CLEANABLE = new EnumMap<>(TransactionStatus.class);

    static {
        // ACTIVE; should only contain ACTIVE (and ROLLED_BACK from recovery)
        CLEANABLE.put(TransactionStatus.ACTIVE, Arrays.asList(TransactionStatus.ACTIVE, TransactionStatus.ROLLED_BACK));

        // PREPARING/PREPARED; can be cleaned when PREPARED no longer exists (COMMITTING should not exist)
        CLEANABLE.put(TransactionStatus.PREPARING,
                Arrays.asList(TransactionStatus.ACTIVE, TransactionStatus.PREPARING, TransactionStatus.COMMITTED, TransactionStatus.ROLLED_BACK));
        CLEANABLE.put(TransactionStatus.PREPARED,
                Arrays.asList(TransactionStatus.ACTIVE, TransactionStatus.PREPARING, TransactionStatus.COMMITTED, TransactionStatus.ROLLED_BACK));

        // COMMITTING/COMMITTED; can only be cleaned when everything is COMMITTED!
        CLEANABLE.put(TransactionStatus.COMMITTING, Arrays.asList(TransactionStatus.COMMITTED));
        CLEANABLE.put(TransactionStatus.COMMITTED, Arrays.asList(TransactionStatus.COMMITTED));

        // Do not clean TransactionStatus.COMMIT_FAILED

        // ROLLING_BACK/ROLLED_BACK; can be cleaned when PREPARED no longer exists
        CLEANABLE.put(TransactionStatus.ROLLING_BACK,
                Arrays.asList(TransactionStatus.ACTIVE, TransactionStatus.PREPARING, TransactionStatus.COMMITTED, TransactionStatus.ROLLED_BACK));
        CLEANABLE.put(TransactionStatus.ROLLED_BACK,
                Arrays.asList(TransactionStatus.ACTIVE, TransactionStatus.PREPARING, TransactionStatus.COMMITTED, TransactionStatus.ROLLED_BACK));

        // Do not clean TransactionStatus.ROLLBACK_FAILED
    }

    ;

    @Override
    public void cleanup() throws JtaTransactionStoreException {
        jdbc.doInConnection(connection -> {
            try (final PreparedStatement transactionsStatement = connection.prepareStatement(sqlTemplate.selectTransactionIdAndStatus());
                 final ResultSet transactionsResult = transactionsStatement.executeQuery()) {
                while (transactionsResult.next()) {
                    Long transactionId = transactionsResult.getLong(1);
                    TransactionStatus transactionStatus = TransactionStatus.valueOf(transactionsResult.getString(2));

                    if (CLEANABLE.containsKey(transactionStatus)) {
                        boolean cleanable = true;

                        try (final PreparedStatement resourcesStatement = connection.prepareStatement(sqlTemplate.selectResourceStatus())) {
                            resourcesStatement.setLong(1, transactionId);
                            try (final ResultSet resourcesResult = resourcesStatement.executeQuery()) {
                                while (resourcesResult.next()) {
                                    final TransactionStatus resourceStatus = TransactionStatus.valueOf(resourcesResult.getString(1));
                                    if (!CLEANABLE.get(transactionStatus).contains(resourceStatus)) {
                                        cleanable = false;
                                        break;
                                    }
                                }
                            }
                        }

                        if (cleanable) {
                            try (final PreparedStatement deleteResources = connection.prepareStatement(sqlTemplate.deleteResourceStatus())) {
                                deleteResources.setLong(1, transactionId);
                                deleteResources.executeUpdate();
                            }
                            try (final PreparedStatement deleteTransaction = connection.prepareStatement(sqlTemplate.deleteTransactionStatus())) {
                                deleteTransaction.setLong(1, transactionId);
                                deleteTransaction.executeUpdate();
                            }
                        }
                    }
                }
            }

            return null;
        });
    }

    /* ************************** */
    /* *** PERSISTENCE ********** */
    /* ************************** */

    @Override
    public long nextTransactionId() throws JtaTransactionStoreException {
        LOGGER.debug("nextTransactionId()");
        return jdbc.doInConnection(connection -> {
            try (final PreparedStatement statement = connection.prepareStatement(sqlTemplate.selectNextTransactionId());
                 final ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new SQLException("No row returned from sequence select statement");
                }
                return resultSet.getLong(1);
            }
        });
    }

    @Override
    protected PersistentTransaction getPersistentTransaction(final JtaXid xid) throws JtaTransactionStoreException {
        return new JdbcPersistentTransaction(jdbc, sqlTemplate, xid.getTransactionId());
    }

}
