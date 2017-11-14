package nl.futureedge.simple.jta.store.jdbc;

/*-
 * #%L
 * Simple JTA
 * %%
 * Copyright (C) 2017 Future Edge IT
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
import javax.transaction.xa.XAException;
import nl.futureedge.simple.jta.ReflectionTestUtils;
import nl.futureedge.simple.jta.store.jdbc.sql.HsqldbSqlTemplate;
import nl.futureedge.simple.jta.xid.BranchJtaXid;
import nl.futureedge.simple.jta.xid.GlobalJtaXid;
import org.junit.Assert;
import org.junit.Test;

public class JdbcTransactionStoreIT extends AbstractJdbcTransactionStoreIT {

    @Override
    void setupSubject(JdbcTransactionStore subject) {
        // subject.setStoreAll(false); // false is the default
    }

    @Test
    public void determineSqlTemplate() throws Exception {
        Assert.assertNotNull(ReflectionTestUtils.getField(subject, "pool"));
        Assert.assertTrue(ReflectionTestUtils.getField(subject, "sqlTemplate") instanceof HsqldbSqlTemplate);
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

    @Test
    public void testCommit() throws Exception {
        long transactionId = subject.nextTransactionId();
        final GlobalJtaXid globalXid = new GlobalJtaXid("test", transactionId);
        final BranchJtaXid branchXid = globalXid.createBranchXid();

        final String resource1 = "resourceOne";
        final String resource2 = "resourceTwo";

        Assert.assertFalse(subject.isCommitting(branchXid));
        Assert.assertEquals(null, selectStatus(transactionId));
        Assert.assertEquals(null, selectStatus(transactionId, resource1));
        Assert.assertEquals(null, selectStatus(transactionId, resource2));

        // ENLIST
        subject.active(globalXid);
        Assert.assertFalse(subject.isCommitting(branchXid));
        Assert.assertEquals(null, selectStatus(transactionId));
        Assert.assertEquals(null, selectStatus(transactionId, resource1));
        Assert.assertEquals(null, selectStatus(transactionId, resource2));

        subject.active(branchXid, resource1);
        Assert.assertEquals(null, selectStatus(transactionId));
        Assert.assertEquals(null, selectStatus(transactionId, resource1));
        Assert.assertEquals(null, selectStatus(transactionId, resource2));

        subject.active(branchXid, resource2);
        Assert.assertEquals(null, selectStatus(transactionId));
        Assert.assertEquals(null, selectStatus(transactionId, resource1));
        Assert.assertEquals(null, selectStatus(transactionId, resource2));

        // PREPARE
        subject.preparing(globalXid);
        Assert.assertFalse(subject.isCommitting(branchXid));
        Assert.assertEquals("PREPARING", selectStatus(transactionId));
        Assert.assertEquals(null, selectStatus(transactionId, resource1));
        Assert.assertEquals(null, selectStatus(transactionId, resource2));

        subject.preparing(branchXid, resource1);
        Assert.assertEquals("PREPARING", selectStatus(transactionId));
        Assert.assertEquals(null, selectStatus(transactionId, resource1));
        Assert.assertEquals(null, selectStatus(transactionId, resource2));

        subject.prepared(branchXid, resource1);
        Assert.assertEquals("PREPARING", selectStatus(transactionId));
        Assert.assertEquals("PREPARED", selectStatus(transactionId, resource1));
        Assert.assertEquals(null, selectStatus(transactionId, resource2));

        subject.preparing(branchXid, resource2);
        Assert.assertEquals("PREPARING", selectStatus(transactionId));
        Assert.assertEquals("PREPARED", selectStatus(transactionId, resource1));
        Assert.assertEquals(null, selectStatus(transactionId, resource2));

        subject.prepared(branchXid, resource2);
        Assert.assertFalse(subject.isCommitting(branchXid));
        Assert.assertEquals("PREPARING", selectStatus(transactionId));
        Assert.assertEquals("PREPARED", selectStatus(transactionId, resource1));
        Assert.assertEquals("PREPARED", selectStatus(transactionId, resource2));

        subject.prepared(globalXid);
        Assert.assertEquals("PREPARING", selectStatus(transactionId));
        Assert.assertEquals("PREPARED", selectStatus(transactionId, resource1));
        Assert.assertEquals("PREPARED", selectStatus(transactionId, resource2));

        // COMMIT
        subject.committing(globalXid);
        Assert.assertTrue(subject.isCommitting(branchXid));
        Assert.assertEquals("COMMITTING", selectStatus(transactionId));
        Assert.assertEquals("PREPARED", selectStatus(transactionId, resource1));
        Assert.assertEquals("PREPARED", selectStatus(transactionId, resource2));

        subject.committing(branchXid, resource1);
        Assert.assertEquals("COMMITTING", selectStatus(transactionId));
        Assert.assertEquals("PREPARED", selectStatus(transactionId, resource1));
        Assert.assertEquals("PREPARED", selectStatus(transactionId, resource2));

        subject.committed(branchXid, resource1);
        Assert.assertEquals("COMMITTING", selectStatus(transactionId));
        Assert.assertEquals("COMMITTED", selectStatus(transactionId, resource1));
        Assert.assertEquals("PREPARED", selectStatus(transactionId, resource2));

        subject.committing(branchXid, resource2);
        Assert.assertEquals("COMMITTING", selectStatus(transactionId));
        Assert.assertEquals("COMMITTED", selectStatus(transactionId, resource1));
        Assert.assertEquals("PREPARED", selectStatus(transactionId, resource2));

        subject.committed(branchXid, resource2);
        Assert.assertEquals("COMMITTING", selectStatus(transactionId));
        Assert.assertEquals("COMMITTED", selectStatus(transactionId, resource1));
        Assert.assertEquals("COMMITTED", selectStatus(transactionId, resource2));

        subject.committed(globalXid);
        Assert.assertFalse(subject.isCommitting(branchXid));
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
        subject.prepared(globalXid);

        // COMMIT
        subject.committing(globalXid);
        subject.committing(branchXid, resource1);
        subject.committed(branchXid, resource1);
        subject.committing(branchXid, resource2);
        Assert.assertEquals("COMMITTING", selectStatus(transactionId));
        Assert.assertEquals("COMMITTED", selectStatus(transactionId, resource1));
        Assert.assertEquals("PREPARED", selectStatus(transactionId, resource2));

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
        Assert.assertEquals(null, selectStatus(transactionId));
        Assert.assertEquals(null, selectStatus(transactionId, resource1));
        Assert.assertEquals(null, selectStatus(transactionId, resource2));

        subject.active(branchXid, resource1);
        Assert.assertEquals(null, selectStatus(transactionId));
        Assert.assertEquals(null, selectStatus(transactionId, resource1));
        Assert.assertEquals(null, selectStatus(transactionId, resource2));

        subject.active(branchXid, resource2);
        Assert.assertEquals(null, selectStatus(transactionId));
        Assert.assertEquals(null, selectStatus(transactionId, resource1));
        Assert.assertEquals(null, selectStatus(transactionId, resource2));

        // ROLLBACK
        subject.rollingBack(globalXid);
        Assert.assertEquals(null, selectStatus(transactionId));
        Assert.assertEquals(null, selectStatus(transactionId, resource1));
        Assert.assertEquals(null, selectStatus(transactionId, resource2));

        subject.rollingBack(branchXid, resource1);
        Assert.assertEquals(null, selectStatus(transactionId));
        Assert.assertEquals(null, selectStatus(transactionId, resource1));
        Assert.assertEquals(null, selectStatus(transactionId, resource2));

        subject.rolledBack(branchXid, resource1);
        Assert.assertEquals(null, selectStatus(transactionId));
        Assert.assertEquals(null, selectStatus(transactionId, resource1));
        Assert.assertEquals(null, selectStatus(transactionId, resource2));

        subject.rollingBack(branchXid, resource2);
        Assert.assertEquals(null, selectStatus(transactionId));
        Assert.assertEquals(null, selectStatus(transactionId, resource1));
        Assert.assertEquals(null, selectStatus(transactionId, resource2));

        subject.rolledBack(branchXid, resource2);
        Assert.assertEquals(null, selectStatus(transactionId));
        Assert.assertEquals(null, selectStatus(transactionId, resource1));
        Assert.assertEquals(null, selectStatus(transactionId, resource2));

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
        Assert.assertEquals(null, selectStatus(transactionId));
        Assert.assertEquals(null, selectStatus(transactionId, resource1));
        Assert.assertEquals(null, selectStatus(transactionId, resource2));

        subject.rollbackFailed(branchXid, resource2, new XAException("Test"));
        Assert.assertEquals(null, selectStatus(transactionId));
        Assert.assertEquals(null, selectStatus(transactionId, resource1));
        Assert.assertEquals("ROLLBACK_FAILED", selectStatus(transactionId, resource2));

        subject.rollbackFailed(globalXid);
        Assert.assertEquals("ROLLBACK_FAILED", selectStatus(transactionId));
        Assert.assertEquals(null, selectStatus(transactionId, resource1));
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
        Assert.assertEquals(null, selectStatus(transactionId));
        Assert.assertEquals(null, selectStatus(transactionId, resource1));
        Assert.assertEquals(null, selectStatus(transactionId, resource2));

        subject.active(branchXid, resource1);
        Assert.assertEquals(null, selectStatus(transactionId));
        Assert.assertEquals(null, selectStatus(transactionId, resource1));
        Assert.assertEquals(null, selectStatus(transactionId, resource2));

        subject.active(branchXid, resource2);
        Assert.assertEquals(null, selectStatus(transactionId));
        Assert.assertEquals(null, selectStatus(transactionId, resource1));
        Assert.assertEquals(null, selectStatus(transactionId, resource2));

        // PREPARE
        subject.preparing(globalXid);
        Assert.assertEquals("PREPARING", selectStatus(transactionId));
        Assert.assertEquals(null, selectStatus(transactionId, resource1));
        Assert.assertEquals(null, selectStatus(transactionId, resource2));

        subject.preparing(branchXid, resource1);
        Assert.assertEquals("PREPARING", selectStatus(transactionId));
        Assert.assertEquals(null, selectStatus(transactionId, resource1));
        Assert.assertEquals(null, selectStatus(transactionId, resource2));

        subject.prepared(branchXid, resource1);
        Assert.assertEquals("PREPARING", selectStatus(transactionId));
        Assert.assertEquals("PREPARED", selectStatus(transactionId, resource1));
        Assert.assertEquals(null, selectStatus(transactionId, resource2));

        subject.preparing(branchXid, resource2);
        Assert.assertEquals("PREPARING", selectStatus(transactionId));
        Assert.assertEquals("PREPARED", selectStatus(transactionId, resource1));
        Assert.assertEquals(null, selectStatus(transactionId, resource2));

        // ROLLBACK
        subject.rollingBack(globalXid);
        Assert.assertEquals("ROLLING_BACK", selectStatus(transactionId));
        Assert.assertEquals("PREPARED", selectStatus(transactionId, resource1));
        Assert.assertEquals(null, selectStatus(transactionId, resource2));

        subject.rollingBack(branchXid, resource1);
        Assert.assertEquals("ROLLING_BACK", selectStatus(transactionId));
        Assert.assertEquals("PREPARED", selectStatus(transactionId, resource1));
        Assert.assertEquals(null, selectStatus(transactionId, resource2));

        subject.rolledBack(branchXid, resource1);
        Assert.assertEquals("ROLLING_BACK", selectStatus(transactionId));
        Assert.assertEquals("ROLLED_BACK", selectStatus(transactionId, resource1));
        Assert.assertEquals(null, selectStatus(transactionId, resource2));

        subject.rollingBack(branchXid, resource2);
        Assert.assertEquals("ROLLING_BACK", selectStatus(transactionId));
        Assert.assertEquals("ROLLED_BACK", selectStatus(transactionId, resource1));
        Assert.assertEquals(null, selectStatus(transactionId, resource2));

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
        Assert.assertEquals(null, selectStatus(transactionId, resource2));

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

}
