package nl.futureedge.simple.jta;

import java.util.Arrays;
import java.util.Collections;
import javax.transaction.xa.Xid;
import nl.futureedge.simple.jta.xid.BranchJtaXid;
import nl.futureedge.simple.jta.xid.GlobalJtaXid;
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
    }

    @Test
    public void testFilter() {
        BranchJtaXid xid1 = new GlobalJtaXid("tm001", 1L).createBranchXid();
        BranchJtaXid xid2 = new GlobalJtaXid("tm001", 2L).createBranchXid();
        BranchJtaXid xid3 = new GlobalJtaXid("tm002", 1L).createBranchXid();

        Assert.assertEquals(Arrays.asList(xid1, xid2), BranchJtaXid.filterRecoveryXids(new Xid[]{xid1, xid2, xid3}, "tm001"));
        Assert.assertEquals(Arrays.asList(xid3), BranchJtaXid.filterRecoveryXids(new Xid[]{xid1, xid2, xid3}, "tm002"));
        Assert.assertEquals(Collections.emptyList(), BranchJtaXid.filterRecoveryXids(new Xid[]{xid1, xid2, xid3}, "tm003"));
    }

}
