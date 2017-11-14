package nl.futureedge.simple.jta.store.jdbc;

import javax.transaction.xa.XAException;
import nl.futureedge.simple.jta.xid.BranchJtaXid;
import nl.futureedge.simple.jta.xid.GlobalJtaXid;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JdbcTransactionStoreAllIT extends AbstractJdbcTransactionStoreIT {

    private static final Logger LOGGER = LoggerFactory.getLogger(JdbcTransactionStoreAllIT.class);

    @Override
    void setupSubject(JdbcTransactionStore subject) {
        subject.setStoreAll(true);
    }

    @Test
    public void testCommit() throws Exception {
        long transactionId = subject.nextTransactionId();
        final GlobalJtaXid globalXid = new GlobalJtaXid("test", transactionId);
        final BranchJtaXid branchXid = globalXid.createBranchXid();

        final String resource1 = "resourceOne";
        final String resource2 = "resourceTwo";

        Assert.assertEquals(null, selectStatus(transactionId));
        Assert.assertEquals(null, selectStatus(transactionId, resource1));
        Assert.assertEquals(null, selectStatus(transactionId, resource2));

        // ENLIST
        subject.active(globalXid);
        Assert.assertEquals("ACTIVE", selectStatus(transactionId));
        Assert.assertEquals(null, selectStatus(transactionId, resource1));
        Assert.assertEquals(null, selectStatus(transactionId, resource2));

        subject.active(branchXid, resource1);
        Assert.assertEquals("ACTIVE", selectStatus(transactionId));
        Assert.assertEquals("ACTIVE", selectStatus(transactionId, resource1));
        Assert.assertEquals(null, selectStatus(transactionId, resource2));

        subject.active(branchXid, resource2);
        Assert.assertEquals("ACTIVE", selectStatus(transactionId));
        Assert.assertEquals("ACTIVE", selectStatus(transactionId, resource1));
        Assert.assertEquals("ACTIVE", selectStatus(transactionId, resource2));

        // PREPARE
        subject.preparing(globalXid);
        Assert.assertEquals("PREPARING", selectStatus(transactionId));
        Assert.assertEquals("ACTIVE", selectStatus(transactionId, resource1));
        Assert.assertEquals("ACTIVE", selectStatus(transactionId, resource2));

        subject.preparing(branchXid, resource1);
        Assert.assertEquals("PREPARING", selectStatus(transactionId));
        Assert.assertEquals("PREPARING", selectStatus(transactionId, resource1));
        Assert.assertEquals("ACTIVE", selectStatus(transactionId, resource2));

        subject.prepared(branchXid, resource1);
        Assert.assertEquals("PREPARING", selectStatus(transactionId));
        Assert.assertEquals("PREPARED", selectStatus(transactionId, resource1));
        Assert.assertEquals("ACTIVE", selectStatus(transactionId, resource2));

        subject.preparing(branchXid, resource2);
        Assert.assertEquals("PREPARING", selectStatus(transactionId));
        Assert.assertEquals("PREPARED", selectStatus(transactionId, resource1));
        Assert.assertEquals("PREPARING", selectStatus(transactionId, resource2));

        subject.prepared(branchXid, resource2);
        Assert.assertEquals("PREPARING", selectStatus(transactionId));
        Assert.assertEquals("PREPARED", selectStatus(transactionId, resource1));
        Assert.assertEquals("PREPARED", selectStatus(transactionId, resource2));

        subject.prepared(globalXid);
        Assert.assertEquals("PREPARED", selectStatus(transactionId));
        Assert.assertEquals("PREPARED", selectStatus(transactionId, resource1));
        Assert.assertEquals("PREPARED", selectStatus(transactionId, resource2));


        // COMMIT
        subject.committing(globalXid);
        Assert.assertEquals("COMMITTING", selectStatus(transactionId));
        Assert.assertEquals("PREPARED", selectStatus(transactionId, resource1));
        Assert.assertEquals("PREPARED", selectStatus(transactionId, resource2));

        subject.committing(branchXid, resource1);
        Assert.assertEquals("COMMITTING", selectStatus(transactionId));
        Assert.assertEquals("COMMITTING", selectStatus(transactionId, resource1));
        Assert.assertEquals("PREPARED", selectStatus(transactionId, resource2));

        subject.committed(branchXid, resource1);
        Assert.assertEquals("COMMITTING", selectStatus(transactionId));
        Assert.assertEquals("COMMITTED", selectStatus(transactionId, resource1));
        Assert.assertEquals("PREPARED", selectStatus(transactionId, resource2));

        subject.committing(branchXid, resource2);
        Assert.assertEquals("COMMITTING", selectStatus(transactionId));
        Assert.assertEquals("COMMITTED", selectStatus(transactionId, resource1));
        Assert.assertEquals("COMMITTING", selectStatus(transactionId, resource2));

        subject.committed(branchXid, resource2);
        Assert.assertEquals("COMMITTING", selectStatus(transactionId));
        Assert.assertEquals("COMMITTED", selectStatus(transactionId, resource1));
        Assert.assertEquals("COMMITTED", selectStatus(transactionId, resource2));

        subject.committed(globalXid);
        Assert.assertEquals(null, selectStatus(transactionId));
        Assert.assertEquals(null, selectStatus(transactionId, resource1));
        Assert.assertEquals(null, selectStatus(transactionId, resource2));

        debugTables();
    }

    @Test
    public void testCommitFailed() throws Exception {
        long transactionId = subject.nextTransactionId();
        final GlobalJtaXid globalXid = new GlobalJtaXid("test", transactionId);
        final BranchJtaXid branchXid = globalXid.createBranchXid();

        final String resource1 = "resourceOne";
        final String resource2 = "resourceTwo";

        Assert.assertEquals(null, selectStatus(transactionId));
        Assert.assertEquals(null, selectStatus(transactionId, resource1));
        Assert.assertEquals(null, selectStatus(transactionId, resource2));

        // ENLIST
        subject.active(globalXid);
        subject.active(branchXid, resource1);
        subject.active(branchXid, resource2);

        // PREPARE
        subject.preparing(globalXid);
        subject.preparing(branchXid, resource1);
        subject.prepared(branchXid, resource1);
        subject.preparing(branchXid, resource2);
        subject.prepared(branchXid, resource2);

        // COMMIT
        subject.committing(globalXid);
        subject.committing(branchXid, resource1);
        subject.committed(branchXid, resource1);
        subject.committing(branchXid, resource2);
        Assert.assertEquals("COMMITTING", selectStatus(transactionId));
        Assert.assertEquals("COMMITTED", selectStatus(transactionId, resource1));
        Assert.assertEquals("COMMITTING", selectStatus(transactionId, resource2));

        subject.commitFailed(branchXid, resource2, new XAException("Test"));
        Assert.assertEquals("COMMITTING", selectStatus(transactionId));
        Assert.assertEquals("COMMITTED", selectStatus(transactionId, resource1));
        Assert.assertEquals("COMMIT_FAILED", selectStatus(transactionId, resource2));

        subject.commitFailed(globalXid);
        Assert.assertEquals("COMMIT_FAILED", selectStatus(transactionId));
        Assert.assertEquals("COMMITTED", selectStatus(transactionId, resource1));
        Assert.assertEquals("COMMIT_FAILED", selectStatus(transactionId, resource2));

        debugTables();
    }

    @Test
    public void testRollbackWithoutPrepare() throws Exception {
        long transactionId = subject.nextTransactionId();
        final GlobalJtaXid globalXid = new GlobalJtaXid("test", transactionId);
        final BranchJtaXid branchXid = globalXid.createBranchXid();

        final String resource1 = "resourceOne";
        final String resource2 = "resourceTwo";

        Assert.assertEquals(null, selectStatus(transactionId));
        Assert.assertEquals(null, selectStatus(transactionId, resource1));
        Assert.assertEquals(null, selectStatus(transactionId, resource2));

        // ENLIST
        subject.active(globalXid);
        Assert.assertEquals("ACTIVE", selectStatus(transactionId));
        Assert.assertEquals(null, selectStatus(transactionId, resource1));
        Assert.assertEquals(null, selectStatus(transactionId, resource2));

        subject.active(branchXid, resource1);
        Assert.assertEquals("ACTIVE", selectStatus(transactionId));
        Assert.assertEquals("ACTIVE", selectStatus(transactionId, resource1));
        Assert.assertEquals(null, selectStatus(transactionId, resource2));

        subject.active(branchXid, resource2);
        Assert.assertEquals("ACTIVE", selectStatus(transactionId));
        Assert.assertEquals("ACTIVE", selectStatus(transactionId, resource1));
        Assert.assertEquals("ACTIVE", selectStatus(transactionId, resource2));

        // ROLLBACK
        subject.rollingBack(globalXid);
        Assert.assertEquals("ROLLING_BACK", selectStatus(transactionId));
        Assert.assertEquals("ACTIVE", selectStatus(transactionId, resource1));
        Assert.assertEquals("ACTIVE", selectStatus(transactionId, resource2));

        subject.rollingBack(branchXid, resource1);
        Assert.assertEquals("ROLLING_BACK", selectStatus(transactionId));
        Assert.assertEquals("ROLLING_BACK", selectStatus(transactionId, resource1));
        Assert.assertEquals("ACTIVE", selectStatus(transactionId, resource2));

        subject.rolledBack(branchXid, resource1);
        Assert.assertEquals("ROLLING_BACK", selectStatus(transactionId));
        Assert.assertEquals("ROLLED_BACK", selectStatus(transactionId, resource1));
        Assert.assertEquals("ACTIVE", selectStatus(transactionId, resource2));

        subject.rollingBack(branchXid, resource2);
        Assert.assertEquals("ROLLING_BACK", selectStatus(transactionId));
        Assert.assertEquals("ROLLED_BACK", selectStatus(transactionId, resource1));
        Assert.assertEquals("ROLLING_BACK", selectStatus(transactionId, resource2));

        subject.rolledBack(branchXid, resource2);
        Assert.assertEquals("ROLLING_BACK", selectStatus(transactionId));
        Assert.assertEquals("ROLLED_BACK", selectStatus(transactionId, resource1));
        Assert.assertEquals("ROLLED_BACK", selectStatus(transactionId, resource2));

        subject.rolledBack(globalXid);
        Assert.assertEquals(null, selectStatus(transactionId));
        Assert.assertEquals(null, selectStatus(transactionId, resource1));
        Assert.assertEquals(null, selectStatus(transactionId, resource2));

        debugTables();
    }

    @Test
    public void testRollbackFailedWithoutPrepare() throws Exception {
        long transactionId = subject.nextTransactionId();
        final GlobalJtaXid globalXid = new GlobalJtaXid("test", transactionId);
        final BranchJtaXid branchXid = globalXid.createBranchXid();

        final String resource1 = "resourceOne";
        final String resource2 = "resourceTwo";

        Assert.assertEquals(null, selectStatus(transactionId));
        Assert.assertEquals(null, selectStatus(transactionId, resource1));
        Assert.assertEquals(null, selectStatus(transactionId, resource2));

        // ENLIST
        subject.active(globalXid);
        subject.active(branchXid, resource1);
        subject.active(branchXid, resource2);

        // ROLLBACK
        subject.rollingBack(globalXid);
        subject.rollingBack(branchXid, resource1);
        subject.rolledBack(branchXid, resource1);
        subject.rollingBack(branchXid, resource2);
        Assert.assertEquals("ROLLING_BACK", selectStatus(transactionId));
        Assert.assertEquals("ROLLED_BACK", selectStatus(transactionId, resource1));
        Assert.assertEquals("ROLLING_BACK", selectStatus(transactionId, resource2));

        subject.rollbackFailed(branchXid, resource2, new XAException("Test"));
        Assert.assertEquals("ROLLING_BACK", selectStatus(transactionId));
        Assert.assertEquals("ROLLED_BACK", selectStatus(transactionId, resource1));
        Assert.assertEquals("ROLLBACK_FAILED", selectStatus(transactionId, resource2));

        subject.rollbackFailed(globalXid);
        Assert.assertEquals("ROLLBACK_FAILED", selectStatus(transactionId));
        Assert.assertEquals("ROLLED_BACK", selectStatus(transactionId, resource1));
        Assert.assertEquals("ROLLBACK_FAILED", selectStatus(transactionId, resource2));

        debugTables();
    }

    @Test
    public void testRollbackWithPrepare() throws Exception {
        long transactionId = subject.nextTransactionId();
        final GlobalJtaXid globalXid = new GlobalJtaXid("test", transactionId);
        final BranchJtaXid branchXid = globalXid.createBranchXid();

        final String resource1 = "resourceOne";
        final String resource2 = "resourceTwo";

        Assert.assertEquals(null, selectStatus(transactionId));
        Assert.assertEquals(null, selectStatus(transactionId, resource1));
        Assert.assertEquals(null, selectStatus(transactionId, resource2));

        // ENLIST
        subject.active(globalXid);
        Assert.assertEquals("ACTIVE", selectStatus(transactionId));
        Assert.assertEquals(null, selectStatus(transactionId, resource1));
        Assert.assertEquals(null, selectStatus(transactionId, resource2));

        subject.active(branchXid, resource1);
        Assert.assertEquals("ACTIVE", selectStatus(transactionId));
        Assert.assertEquals("ACTIVE", selectStatus(transactionId, resource1));
        Assert.assertEquals(null, selectStatus(transactionId, resource2));

        subject.active(branchXid, resource2);
        Assert.assertEquals("ACTIVE", selectStatus(transactionId));
        Assert.assertEquals("ACTIVE", selectStatus(transactionId, resource1));
        Assert.assertEquals("ACTIVE", selectStatus(transactionId, resource2));

        // PREPARE
        subject.preparing(globalXid);
        Assert.assertEquals("PREPARING", selectStatus(transactionId));
        Assert.assertEquals("ACTIVE", selectStatus(transactionId, resource1));
        Assert.assertEquals("ACTIVE", selectStatus(transactionId, resource2));

        subject.preparing(branchXid, resource1);
        Assert.assertEquals("PREPARING", selectStatus(transactionId));
        Assert.assertEquals("PREPARING", selectStatus(transactionId, resource1));
        Assert.assertEquals("ACTIVE", selectStatus(transactionId, resource2));

        subject.prepared(branchXid, resource1);
        Assert.assertEquals("PREPARING", selectStatus(transactionId));
        Assert.assertEquals("PREPARED", selectStatus(transactionId, resource1));
        Assert.assertEquals("ACTIVE", selectStatus(transactionId, resource2));

        subject.preparing(branchXid, resource2);
        Assert.assertEquals("PREPARING", selectStatus(transactionId));
        Assert.assertEquals("PREPARED", selectStatus(transactionId, resource1));
        Assert.assertEquals("PREPARING", selectStatus(transactionId, resource2));

        // ROLLBACK
        subject.rollingBack(globalXid);
        Assert.assertEquals("ROLLING_BACK", selectStatus(transactionId));
        Assert.assertEquals("PREPARED", selectStatus(transactionId, resource1));
        Assert.assertEquals("PREPARING", selectStatus(transactionId, resource2));

        subject.rollingBack(branchXid, resource1);
        Assert.assertEquals("ROLLING_BACK", selectStatus(transactionId));
        Assert.assertEquals("ROLLING_BACK", selectStatus(transactionId, resource1));
        Assert.assertEquals("PREPARING", selectStatus(transactionId, resource2));

        subject.rolledBack(branchXid, resource1);
        Assert.assertEquals("ROLLING_BACK", selectStatus(transactionId));
        Assert.assertEquals("ROLLED_BACK", selectStatus(transactionId, resource1));
        Assert.assertEquals("PREPARING", selectStatus(transactionId, resource2));

        subject.rollingBack(branchXid, resource2);
        Assert.assertEquals("ROLLING_BACK", selectStatus(transactionId));
        Assert.assertEquals("ROLLED_BACK", selectStatus(transactionId, resource1));
        Assert.assertEquals("ROLLING_BACK", selectStatus(transactionId, resource2));

        subject.rolledBack(branchXid, resource2);
        Assert.assertEquals("ROLLING_BACK", selectStatus(transactionId));
        Assert.assertEquals("ROLLED_BACK", selectStatus(transactionId, resource1));
        Assert.assertEquals("ROLLED_BACK", selectStatus(transactionId, resource2));

        subject.rolledBack(globalXid);
        Assert.assertEquals(null, selectStatus(transactionId));
        Assert.assertEquals(null, selectStatus(transactionId, resource1));
        Assert.assertEquals(null, selectStatus(transactionId, resource2));

        debugTables();
    }

    @Test
    public void testRollbackFailedWithPrepare() throws Exception {
        long transactionId = subject.nextTransactionId();
        final GlobalJtaXid globalXid = new GlobalJtaXid("test", transactionId);
        final BranchJtaXid branchXid = globalXid.createBranchXid();

        final String resource1 = "resourceOne";
        final String resource2 = "resourceTwo";

        Assert.assertEquals(null, selectStatus(transactionId));
        Assert.assertEquals(null, selectStatus(transactionId, resource1));
        Assert.assertEquals(null, selectStatus(transactionId, resource2));

        // ENLIST
        subject.active(globalXid);
        subject.active(branchXid, resource1);
        subject.active(branchXid, resource2);

        // PREPARE
        subject.preparing(globalXid);
        subject.preparing(branchXid, resource1);
        subject.prepared(branchXid, resource1);
        subject.preparing(branchXid, resource2);

        // ROLLBACK
        subject.rollingBack(globalXid);
        subject.rollingBack(branchXid, resource1);
        subject.rolledBack(branchXid, resource1);
        subject.rollingBack(branchXid, resource2);
        Assert.assertEquals("ROLLING_BACK", selectStatus(transactionId));
        Assert.assertEquals("ROLLED_BACK", selectStatus(transactionId, resource1));
        Assert.assertEquals("ROLLING_BACK", selectStatus(transactionId, resource2));

        subject.rollbackFailed(branchXid, resource2, new XAException("Test"));
        Assert.assertEquals("ROLLING_BACK", selectStatus(transactionId));
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
