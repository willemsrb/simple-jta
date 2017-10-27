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
import nl.futureedge.simple.jta.ReflectionTestUtils;
import nl.futureedge.simple.jta.store.impl.TransactionStatus;
import nl.futureedge.simple.jta.store.jdbc.JdbcTransactionStore;
import nl.futureedge.simple.jta.store.jdbc.sql.HsqldbSqlTemplate;
import nl.futureedge.simple.jta.xid.BranchJtaXid;
import nl.futureedge.simple.jta.xid.GlobalJtaXid;
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
        final GlobalJtaXid xid1 = new GlobalJtaXid("test", transactionId1);

        long transactionId2 = subject.nextTransactionId();
        LOGGER.debug("Transaction ID (2): " + transactionId2);
        final GlobalJtaXid xid2 = new GlobalJtaXid("test", transactionId2);

        // Empty
        Assert.assertEquals(null, selectStatus(transactionId1));
        Assert.assertEquals(null, selectStatus(transactionId2));
        Assert.assertFalse(subject.isCommitting(xid1.createBranchXid()));
        Assert.assertFalse(subject.isCommitting(xid2.createBranchXid()));

        subject.active(xid1);
        Assert.assertEquals("ACTIVE", selectStatus(transactionId1));
        Assert.assertEquals(null, selectStatus(transactionId2));
        Assert.assertFalse(subject.isCommitting(xid1.createBranchXid()));
        Assert.assertFalse(subject.isCommitting(xid2.createBranchXid()));

        subject.preparing(xid1);
        Assert.assertEquals("PREPARING", selectStatus(transactionId1));
        Assert.assertEquals(null, selectStatus(transactionId2));
        Assert.assertFalse(subject.isCommitting(xid1.createBranchXid()));
        Assert.assertFalse(subject.isCommitting(xid2.createBranchXid()));

        subject.prepared(xid1);
        Assert.assertEquals("PREPARED", selectStatus(transactionId1));
        Assert.assertEquals(null, selectStatus(transactionId2));
        Assert.assertFalse(subject.isCommitting(xid1.createBranchXid()));
        Assert.assertFalse(subject.isCommitting(xid2.createBranchXid()));

        subject.active(xid2);
        Assert.assertEquals("PREPARED", selectStatus(transactionId1));
        Assert.assertEquals("ACTIVE", selectStatus(transactionId2));
        Assert.assertFalse(subject.isCommitting(xid1.createBranchXid()));
        Assert.assertFalse(subject.isCommitting(xid2.createBranchXid()));

        subject.rollingBack(xid2);
        Assert.assertEquals("PREPARED", selectStatus(transactionId1));
        Assert.assertEquals("ROLLING_BACK", selectStatus(transactionId2));
        Assert.assertFalse(subject.isCommitting(xid1.createBranchXid()));
        Assert.assertFalse(subject.isCommitting(xid2.createBranchXid()));

        subject.rolledBack(xid2);
        Assert.assertEquals("PREPARED", selectStatus(transactionId1));
        Assert.assertEquals(null, selectStatus(transactionId2));
        Assert.assertFalse(subject.isCommitting(xid1.createBranchXid()));
        Assert.assertFalse(subject.isCommitting(xid2.createBranchXid()));

        subject.committing(xid1);
        Assert.assertEquals("COMMITTING", selectStatus(transactionId1));
        Assert.assertEquals(null, selectStatus(transactionId2));
        Assert.assertTrue(subject.isCommitting(xid1.createBranchXid()));
        Assert.assertFalse(subject.isCommitting(xid2.createBranchXid()));

        debugTables();

        subject.committed(xid1);
        Assert.assertEquals(null, selectStatus(transactionId1));
        Assert.assertEquals(null, selectStatus(transactionId2));
        Assert.assertFalse(subject.isCommitting(xid1.createBranchXid()));
        Assert.assertFalse(subject.isCommitting(xid2.createBranchXid()));
    }

    @Test
    public void testCommitResource() throws Exception {
        long transactionId = subject.nextTransactionId();
        final GlobalJtaXid globalXid = new GlobalJtaXid("test", transactionId);
        final BranchJtaXid branchXid = globalXid.createBranchXid();

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
        final GlobalJtaXid globalXid = new GlobalJtaXid("test", transactionId);
        final BranchJtaXid branchXid = globalXid.createBranchXid();

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

    @Test
    public void cleanup() throws Exception {
        long transactionId1 = subject.nextTransactionId();
        final GlobalJtaXid globalXid1 = new GlobalJtaXid("test", transactionId1);
        final BranchJtaXid branchXid1a = globalXid1.createBranchXid();
        final BranchJtaXid branchXid1b = globalXid1.createBranchXid();

        long transactionId2 = subject.nextTransactionId();
        final GlobalJtaXid globalXid2 = new GlobalJtaXid("test", transactionId2);
        final BranchJtaXid branchXid2a = globalXid2.createBranchXid();
        final BranchJtaXid branchXid2b = globalXid2.createBranchXid();

        long transactionId3 = subject.nextTransactionId();
        final GlobalJtaXid globalXid3 = new GlobalJtaXid("test", transactionId3);
        final BranchJtaXid branchXid3 = globalXid3.createBranchXid();

        long transactionId4 = subject.nextTransactionId();
        final GlobalJtaXid globalXid4 = new GlobalJtaXid("test", transactionId4);
        final BranchJtaXid branchXid4a = globalXid4.createBranchXid();
        final BranchJtaXid branchXid4b = globalXid4.createBranchXid();

        subject.active(globalXid1);
        subject.active(branchXid1a, "resource");
        subject.rollingBack(branchXid1b, "resource");
        Assert.assertEquals("ACTIVE", selectStatus(transactionId1));

        subject.preparing(globalXid2);
        subject.prepared(branchXid2a, "resource");
        subject.committed(branchXid2b, "resource");
        Assert.assertEquals("PREPARING", selectStatus(transactionId2));

        subject.committing(globalXid3);
        subject.prepared(branchXid3, "resource");
        Assert.assertEquals("COMMITTING", selectStatus(transactionId3));

        subject.rollingBack(globalXid4);
        subject.prepared(branchXid4a, "resource");
        subject.rolledBack(branchXid4b, "resource");
        Assert.assertEquals("ROLLING_BACK", selectStatus(transactionId4));

        LOGGER.debug("BEFORE(1)");
        debugTables();
        subject.cleanup();
        LOGGER.debug("AFTER(1)");
        debugTables();

        Assert.assertEquals("ACTIVE", selectStatus(transactionId1));
        Assert.assertEquals("PREPARING", selectStatus(transactionId2));
        Assert.assertEquals("COMMITTING", selectStatus(transactionId3));
        Assert.assertEquals("ROLLING_BACK", selectStatus(transactionId4));

        subject.rolledBack(branchXid1b, "resource");
        LOGGER.debug("BEFORE(2)");
        debugTables();
        subject.cleanup();
        LOGGER.debug("AFTER(2)");
        Assert.assertEquals(null, selectStatus(transactionId1));
        Assert.assertEquals("PREPARING", selectStatus(transactionId2));
        Assert.assertEquals("COMMITTING", selectStatus(transactionId3));
        Assert.assertEquals("ROLLING_BACK", selectStatus(transactionId4));

        subject.rolledBack(branchXid2a, "resource");
        subject.committed(branchXid3, "resource");
        subject.rolledBack(branchXid4a, "resource");
        LOGGER.debug("BEFORE(3)");
        debugTables();
        subject.cleanup();
        LOGGER.debug("AFTER(3)");
        Assert.assertEquals(null, selectStatus(transactionId1));
        Assert.assertEquals(null, selectStatus(transactionId2));
        Assert.assertEquals(null, selectStatus(transactionId3));
        Assert.assertEquals(null, selectStatus(transactionId4));
    }

}
