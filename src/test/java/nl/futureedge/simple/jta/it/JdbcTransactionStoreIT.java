package nl.futureedge.simple.jta.it;

import java.io.IOException;
import java.net.ServerSocket;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;
import javax.sql.DataSource;
import javax.transaction.xa.XAException;
import nl.futureedge.simple.jta.JtaXid;
import nl.futureedge.simple.jta.ReflectionTestUtils;
import nl.futureedge.simple.jta.store.impl.TransactionStatus;
import nl.futureedge.simple.jta.store.jdbc.JdbcTransactionStore;
import nl.futureedge.simple.jta.store.jdbc.sql.HsqldbSqlTemplate;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.GenericXmlApplicationContext;
import org.springframework.core.env.PropertiesPropertySource;

public class JdbcTransactionStoreIT {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractIT.class);

    private static Properties portProperties = new Properties();
    private static GenericXmlApplicationContext databaseContext;

    private JdbcTransactionStore subject;

    @BeforeClass
    public static void startDependencies() throws IOException {
        try (ServerSocket databasePort = new ServerSocket(0)) {
            LOGGER.info("Configuring database to port: {}", databasePort.getLocalPort());
            portProperties.setProperty("test.database.port", Integer.toString(databasePort.getLocalPort()));
        }

        // Start DB
        databaseContext = new GenericXmlApplicationContext();
        databaseContext.load("classpath:embedded-database.xml");
        databaseContext.getEnvironment().getPropertySources().addLast(new PropertiesPropertySource("configuration", portProperties));
        databaseContext.refresh();
    }

    @AfterClass
    public static void stopTestContext() {
        if (databaseContext != null) {
            try {
                databaseContext.close();
            } catch (final Exception e) {
                LOGGER.warn("Problem closing DATABASE context", e);
            }
        }
    }

    @Before
    public void setup() throws Exception {
        subject = new JdbcTransactionStore();
        subject.setCreate(true);
        subject.setDriver(null);
        subject.setUrl("jdbc:hsqldb:hsql://localhost:" + portProperties.getProperty("test.database.port") + "/trans");
        subject.setUser("sa");
        subject.setPassword("");
        subject.setSqlTemplate(null);

        subject.afterPropertiesSet();
    }

    @After
    public void destroy() throws Exception {
        subject.destroy();
    }

    @Test
    public void determineSqlTemplate() throws Exception {
        Assert.assertNotNull(ReflectionTestUtils.getField(subject, "jdbc"));
        Assert.assertTrue(ReflectionTestUtils.getField(subject, "sqlTemplate") instanceof HsqldbSqlTemplate);
    }

    @Test
    public void testTransactionStatus() {
        Assert.assertEquals(null, TransactionStatus.fromText("BLA"));
        Assert.assertEquals(TransactionStatus.ACTIVE, TransactionStatus.fromText("ACTIVE"));
    }

    @Test
    public void nextTransactionId() throws Exception {
        long id1 = subject.nextTransactionId();
        long id2 = subject.nextTransactionId();
        long id3 = subject.nextTransactionId();
        Assert.assertNotEquals(id1, id2);
        Assert.assertNotEquals(id1, id3);
        Assert.assertNotEquals(id2, id3);
    }

    private void debugTables() throws SQLException {
        final DataSource dataSource = databaseContext.getBean("transDataSource", DataSource.class);
        try (final Connection connection = dataSource.getConnection()) {
            LOGGER.debug("TRANSACTIONS");
            try (final PreparedStatement statement = connection.prepareStatement("select id, status, created, updated from transactions order by id");
                 final ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    LOGGER.debug(String.format("| %s | %s | %s | %s |", result.getLong(1), result.getString(2), result.getDate(3), result.getDate(4)));
                }
            }

            LOGGER.debug("TRANSACTION_RESOURCES");
            try (final PreparedStatement statement = connection
                    .prepareStatement("select transaction_id, name, status, cause, created, updated from transaction_resources order by transaction_id, name");
                 final ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    LOGGER.debug(
                            String.format("| %s | %s | %s | %s | %s | %s |", result.getLong(1), result.getString(2), result.getString(3), result.getString(4),
                                    result.getDate(5), result.getDate(6)));
                }
            }
        }
    }

    private String selectStatus(long transactionId) throws SQLException {
        final DataSource dataSource = databaseContext.getBean("transDataSource", DataSource.class);
        try (final Connection connection = dataSource.getConnection()) {
            final PreparedStatement statement = connection.prepareStatement("select status from transactions where id = ?");
            statement.setLong(1, transactionId);

            final ResultSet result = statement.executeQuery();
            if (result.next()) {
                return result.getString(1);
            } else {
                return null;
            }
        }
    }

    private String selectStatus(long transactionId, String resource) throws SQLException {
        final DataSource dataSource = databaseContext.getBean("transDataSource", DataSource.class);
        try (final Connection connection = dataSource.getConnection()) {
            final PreparedStatement statement = connection.prepareStatement("select status from transaction_resources where transaction_id = ? and name = ?");
            statement.setLong(1, transactionId);
            statement.setString(2, resource);

            final ResultSet result = statement.executeQuery();
            if (result.next()) {
                return result.getString(1);
            } else {
                return null;
            }
        }
    }

    @Test
    public void testGlobal() throws Exception {
        long transactionId1 = subject.nextTransactionId();
        LOGGER.debug("Transaction ID (1): " + transactionId1);
        final JtaXid xid1 = new JtaXid("test", transactionId1);

        long transactionId2 = subject.nextTransactionId();
        LOGGER.debug("Transaction ID (2): " + transactionId2);
        final JtaXid xid2 = new JtaXid("test", transactionId2);

        // Empty
        Assert.assertEquals(null, selectStatus(transactionId1));
        Assert.assertEquals(null, selectStatus(transactionId2));
        Assert.assertFalse(subject.isCommitting(xid1));
        Assert.assertFalse(subject.isCommitting(xid2));

        subject.active(xid1);
        Assert.assertEquals("ACTIVE", selectStatus(transactionId1));
        Assert.assertEquals(null, selectStatus(transactionId2));
        Assert.assertFalse(subject.isCommitting(xid1));
        Assert.assertFalse(subject.isCommitting(xid2));

        subject.preparing(xid1);
        Assert.assertEquals("PREPARING", selectStatus(transactionId1));
        Assert.assertEquals(null, selectStatus(transactionId2));
        Assert.assertFalse(subject.isCommitting(xid1));
        Assert.assertFalse(subject.isCommitting(xid2));

        subject.prepared(xid1);
        Assert.assertEquals("PREPARED", selectStatus(transactionId1));
        Assert.assertEquals(null, selectStatus(transactionId2));
        Assert.assertFalse(subject.isCommitting(xid1));
        Assert.assertFalse(subject.isCommitting(xid2));

        subject.active(xid2);
        Assert.assertEquals("PREPARED", selectStatus(transactionId1));
        Assert.assertEquals("ACTIVE", selectStatus(transactionId2));
        Assert.assertFalse(subject.isCommitting(xid1));
        Assert.assertFalse(subject.isCommitting(xid2));

        subject.rollingBack(xid2);
        Assert.assertEquals("PREPARED", selectStatus(transactionId1));
        Assert.assertEquals("ROLLING_BACK", selectStatus(transactionId2));
        Assert.assertFalse(subject.isCommitting(xid1));
        Assert.assertFalse(subject.isCommitting(xid2));

        subject.rolledBack(xid2);
        Assert.assertEquals("PREPARED", selectStatus(transactionId1));
        Assert.assertEquals(null, selectStatus(transactionId2));
        Assert.assertFalse(subject.isCommitting(xid1));
        Assert.assertFalse(subject.isCommitting(xid2));

        subject.committing(xid1);
        Assert.assertEquals("COMMITTING", selectStatus(transactionId1));
        Assert.assertEquals(null, selectStatus(transactionId2));
        Assert.assertTrue(subject.isCommitting(xid1));
        Assert.assertFalse(subject.isCommitting(xid2));

        debugTables();

        subject.committed(xid1);
        Assert.assertEquals(null, selectStatus(transactionId1));
        Assert.assertEquals(null, selectStatus(transactionId2));
        Assert.assertFalse(subject.isCommitting(xid1));
        Assert.assertFalse(subject.isCommitting(xid2));
    }

    @Test
    public void testCommitResource() throws Exception {
        long transactionId = subject.nextTransactionId();
        final JtaXid globalXid = new JtaXid("test", transactionId);
        final JtaXid branchXid = globalXid.createBranchXid();

        final String resource1 = "resourceOne";
        final String resource2 = "resourceTwo";

        Assert.assertEquals(null, selectStatus(transactionId));
        Assert.assertEquals(null, selectStatus(transactionId, resource1));
        Assert.assertEquals(null, selectStatus(transactionId, resource2));

        subject.active(globalXid);
        Assert.assertEquals("ACTIVE", selectStatus(transactionId));
        Assert.assertEquals(null, selectStatus(transactionId, resource1));

        subject.active(branchXid, resource1);
        Assert.assertEquals("ACTIVE", selectStatus(transactionId));
        Assert.assertEquals("ACTIVE", selectStatus(transactionId, resource1));

        subject.preparing(branchXid, resource1);
        Assert.assertEquals("ACTIVE", selectStatus(transactionId));
        Assert.assertEquals("PREPARING", selectStatus(transactionId, resource1));
        Assert.assertEquals(null, selectStatus(transactionId, resource2));

        subject.active(branchXid, resource2);
        Assert.assertEquals("ACTIVE", selectStatus(transactionId));
        Assert.assertEquals("PREPARING", selectStatus(transactionId, resource1));
        Assert.assertEquals("ACTIVE", selectStatus(transactionId, resource2));

        subject.prepared(branchXid, resource1);
        Assert.assertEquals("ACTIVE", selectStatus(transactionId));
        Assert.assertEquals("PREPARED", selectStatus(transactionId, resource1));
        Assert.assertEquals("ACTIVE", selectStatus(transactionId, resource2));

        subject.committing(branchXid, resource1);
        Assert.assertEquals("ACTIVE", selectStatus(transactionId));
        Assert.assertEquals("COMMITTING", selectStatus(transactionId, resource1));
        Assert.assertEquals("ACTIVE", selectStatus(transactionId, resource2));

        subject.committed(branchXid, resource1);
        Assert.assertEquals("ACTIVE", selectStatus(transactionId));
        Assert.assertEquals("COMMITTED", selectStatus(transactionId, resource1));
        Assert.assertEquals("ACTIVE", selectStatus(transactionId, resource2));

        subject.commitFailed(branchXid, resource2, new XAException("Test"));
        Assert.assertEquals("ACTIVE", selectStatus(transactionId));
        Assert.assertEquals("COMMITTED", selectStatus(transactionId, resource1));
        Assert.assertEquals("COMMIT_FAILED", selectStatus(transactionId, resource2));

        subject.commitFailed(globalXid);
        Assert.assertEquals("COMMIT_FAILED", selectStatus(transactionId));
        Assert.assertEquals("COMMITTED", selectStatus(transactionId, resource1));
        Assert.assertEquals("COMMIT_FAILED", selectStatus(transactionId, resource2));

        debugTables();
    }


    @Test
    public void testRollbackResource() throws Exception {
        long transactionId = subject.nextTransactionId();
        final JtaXid globalXid = new JtaXid("test", transactionId);
        final JtaXid branchXid = globalXid.createBranchXid();

        final String resource1 = "resourceOne";
        final String resource2 = "resourceTwo";

        Assert.assertEquals(null, selectStatus(transactionId));
        Assert.assertEquals(null, selectStatus(transactionId, resource1));
        Assert.assertEquals(null, selectStatus(transactionId, resource2));

        subject.active(globalXid);
        Assert.assertEquals("ACTIVE", selectStatus(transactionId));
        Assert.assertEquals(null, selectStatus(transactionId, resource1));

        subject.active(branchXid, resource1);
        Assert.assertEquals("ACTIVE", selectStatus(transactionId));
        Assert.assertEquals("ACTIVE", selectStatus(transactionId, resource1));

        subject.preparing(branchXid, resource1);
        Assert.assertEquals("ACTIVE", selectStatus(transactionId));
        Assert.assertEquals("PREPARING", selectStatus(transactionId, resource1));
        Assert.assertEquals(null, selectStatus(transactionId, resource2));

        subject.active(branchXid, resource2);
        Assert.assertEquals("ACTIVE", selectStatus(transactionId));
        Assert.assertEquals("PREPARING", selectStatus(transactionId, resource1));
        Assert.assertEquals("ACTIVE", selectStatus(transactionId, resource2));

        subject.prepared(branchXid, resource1);
        Assert.assertEquals("ACTIVE", selectStatus(transactionId));
        Assert.assertEquals("PREPARED", selectStatus(transactionId, resource1));
        Assert.assertEquals("ACTIVE", selectStatus(transactionId, resource2));

        subject.rollingBack(branchXid, resource1);
        Assert.assertEquals("ACTIVE", selectStatus(transactionId));
        Assert.assertEquals("ROLLING_BACK", selectStatus(transactionId, resource1));
        Assert.assertEquals("ACTIVE", selectStatus(transactionId, resource2));

        subject.rolledBack(branchXid, resource1);
        Assert.assertEquals("ACTIVE", selectStatus(transactionId));
        Assert.assertEquals("ROLLED_BACK", selectStatus(transactionId, resource1));
        Assert.assertEquals("ACTIVE", selectStatus(transactionId, resource2));

        subject.rollbackFailed(branchXid, resource2, new XAException("Test"));
        Assert.assertEquals("ACTIVE", selectStatus(transactionId));
        Assert.assertEquals("ROLLED_BACK", selectStatus(transactionId, resource1));
        Assert.assertEquals("ROLLBACK_FAILED", selectStatus(transactionId, resource2));

        subject.rollbackFailed(globalXid);
        Assert.assertEquals("ROLLBACK_FAILED", selectStatus(transactionId));
        Assert.assertEquals("ROLLED_BACK", selectStatus(transactionId, resource1));
        Assert.assertEquals("ROLLBACK_FAILED", selectStatus(transactionId, resource2));

        debugTables();
    }

}
