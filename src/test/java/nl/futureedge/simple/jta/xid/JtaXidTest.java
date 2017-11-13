package nl.futureedge.simple.jta.xid;

import java.util.Arrays;
import java.util.Collections;
import javax.transaction.xa.Xid;
import org.junit.Assert;
import org.junit.Test;

public class JtaXidTest {

    @Test
    public void test() {
        GlobalJtaXid xid = new GlobalJtaXid("tm001", 1L);
        Assert.assertEquals("tm001", xid.getTransactionManager());
        Assert.assertEquals(1L, xid.getTransactionId());
        Assert.assertNotNull(xid.getFormatId());
        Assert.assertNotNull(xid.getGlobalTransactionId());
        Assert.assertNotNull(xid.getBranchQualifier());
        Assert.assertNotNull(xid.toString());

        Assert.assertEquals(xid, xid);
        Assert.assertNotEquals(xid, new Object());
        Assert.assertEquals(xid, new GlobalJtaXid("tm001", 1L));
        Assert.assertEquals(xid.hashCode(), new GlobalJtaXid("tm001", 1L).hashCode());
        Assert.assertNotEquals(xid, new GlobalJtaXid("tm002", 1L));
        Assert.assertNotEquals(xid, new GlobalJtaXid("tm001", 2L));

        BranchJtaXid branchXid = xid.createBranchXid();
        Assert.assertEquals("tm001", branchXid.getTransactionManager());
        Assert.assertEquals(1L, branchXid.getTransactionId());
        Assert.assertNotNull(branchXid.getBranchId());
        Assert.assertNotNull(branchXid.toString());

        Assert.assertEquals(branchXid, branchXid);
        Assert.assertNotEquals(branchXid, new Object());
        Assert.assertEquals(branchXid, new BranchJtaXid("tm001", 1L, branchXid.getBranchId()));
        Assert.assertEquals(branchXid.hashCode(), new BranchJtaXid("tm001", 1L, branchXid.getBranchId()).hashCode());

        Assert.assertNotEquals(branchXid, new BranchJtaXid("tm002", 1L, branchXid.getBranchId()));
        Assert.assertNotEquals(branchXid, new BranchJtaXid("tm001", 2L, branchXid.getBranchId()));
        Assert.assertNotEquals(branchXid, new BranchJtaXid("tm001", 1L, branchXid.getBranchId() + 1));
    }

    @Test
    public void testFilter() {
        BranchJtaXid xid1 = new GlobalJtaXid("tm001", 1L).createBranchXid();
        BranchJtaXid xid2 = new GlobalJtaXid("tm001", 2L).createBranchXid();
        BranchJtaXid xid3 = new GlobalJtaXid("tm002", 1L).createBranchXid();

        Xid xid4 = new TestXid(BaseJtaXid.SIMPLE_JTA_FORMAT, null, null);
        Xid xid5 = new TestXid(BaseJtaXid.SIMPLE_JTA_FORMAT, new byte[4], null);
        Xid xid6 = new TestXid(BaseJtaXid.SIMPLE_JTA_FORMAT, xid1.getGlobalTransactionId(), null);
        Xid xid7 = new TestXid(BaseJtaXid.SIMPLE_JTA_FORMAT, xid1.getGlobalTransactionId(), new byte[7]);
        Xid xid8 = new TestXid(34534, xid1.getGlobalTransactionId(), xid1.getBranchQualifier());
        Xid xid9 = new TestXid(BaseJtaXid.SIMPLE_JTA_FORMAT, xid1.getGlobalTransactionId(), xid1.getBranchQualifier());

        Assert.assertEquals(Arrays.asList(xid1, xid2, xid1),
                BranchJtaXid.filterRecoveryXids(new Xid[]{xid1, xid2, xid3, xid4, xid5, xid6, xid7, xid8, xid9}, "tm001"));
        Assert.assertEquals(Arrays.asList(xid3), BranchJtaXid.filterRecoveryXids(new Xid[]{xid1, xid2, xid3}, "tm002"));
        Assert.assertEquals(Collections.emptyList(), BranchJtaXid.filterRecoveryXids(new Xid[]{xid1, xid2, xid3}, "tm003"));
    }

    private static final class TestXid implements Xid {

        private int formatId;
        private byte[] globalId;
        private byte[] branchQualifier;

        public TestXid(int formatId, byte[] globalId, byte[] branchQualifier) {
            this.formatId = formatId;
            this.globalId = globalId;
            this.branchQualifier = branchQualifier;
        }

        @Override
        public int getFormatId() {
            return formatId;
        }

        @Override
        public byte[] getGlobalTransactionId() {
            return globalId;
        }

        @Override
        public byte[] getBranchQualifier() {
            return branchQualifier;
        }
    }

}
