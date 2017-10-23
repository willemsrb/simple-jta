package nl.futureedge.simple.jta;

import java.util.Arrays;
import java.util.Collections;
import javax.transaction.xa.Xid;
import org.junit.Assert;
import org.junit.Test;

public class JtaXidTest {

    @Test
    public void test() {
        JtaXid xid = new JtaXid("tm001", 1L);
        Assert.assertEquals("tm001", xid.getTransactionManager());
        Assert.assertEquals(1L, xid.getTransactionId());
        Assert.assertNotNull(xid.getFormatId());
        Assert.assertNotNull(xid.getGlobalTransactionId());
        Assert.assertNotNull(xid.getBranchQualifier());
        Assert.assertNotNull(xid.toString());

        Assert.assertEquals(xid, xid);
        Assert.assertNotEquals(xid, new Object());
        Assert.assertEquals(xid, new JtaXid("tm001", 1L));
        Assert.assertEquals(xid.hashCode(), new JtaXid("tm001", 1L).hashCode());
        Assert.assertNotEquals(xid, new JtaXid("tm002", 1L));
        Assert.assertNotEquals(xid, new JtaXid("tm001", 2L));
    }

    @Test
    public void testFilter() {
        JtaXid xid1 = new JtaXid("tm001", 1L).createBranchXid();
        JtaXid xid2 = new JtaXid("tm001", 2L).createBranchXid();
        JtaXid xid3 = new JtaXid("tm002", 1L).createBranchXid();

        Assert.assertEquals(Arrays.asList(xid1, xid2), JtaXid.filterRecoveryXids(new Xid[]{xid1, xid2, xid3}, "tm001"));
        Assert.assertEquals(Arrays.asList(xid3), JtaXid.filterRecoveryXids(new Xid[]{xid1, xid2, xid3}, "tm002"));
        Assert.assertEquals(Collections.emptyList(), JtaXid.filterRecoveryXids(new Xid[]{xid1, xid2, xid3}, "tm003"));
    }

}
