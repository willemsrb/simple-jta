package nl.futureedge.simple.jta.xid;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import javax.transaction.xa.Xid;

/**
 * Branch Jta XID; not for external use!
 */
public final class BranchJtaXid extends BaseJtaXid {

    BranchJtaXid(final String transactionManager, final long transactionId, final Long branchId) {
        super(transactionManager, transactionId, branchId);
    }

    /**
     * @return return branch id (null if this is the global transaction)
     */
    public Long getBranchId() {
        return super.getBranchId();
    }


    /**
     * Filter the given list of XID's to retrieve the XID's for this transaction manager.
     * @param xids list of XID's
     * @param transactionManager transaction manager unique name
     * @return list of XID's that this transaction manager should recover
     */
    public static List<BranchJtaXid> filterRecoveryXids(final Xid[] xids, final String transactionManager) {
        final List<BranchJtaXid> result = new ArrayList<>();
        final byte[] globalTransactionId = createGlobalTransactionId(transactionManager, 0);

        for (final Xid xid : xids) {
            // Format should be the same,
            // global transaction ID should have the same transaction manager and
            // branch qualifier should contain a branch id (long)
            if (SIMPLE_JTA_FORMAT == xid.getFormatId()
                    && globalTransactionIdMatches(globalTransactionId, xid.getGlobalTransactionId())
                    && xid.getBranchQualifier().length == 8
                    ) {
                final ByteBuffer globalBuffer = ByteBuffer.wrap(xid.getGlobalTransactionId());
                final long transactionId = globalBuffer.getLong(56);

                final ByteBuffer branchBuffer = ByteBuffer.wrap(xid.getBranchQualifier());
                final long branchId = branchBuffer.getLong();

                result.add(new BranchJtaXid(transactionManager, transactionId, branchId));
            }
        }

        return result;
    }

    private static boolean globalTransactionIdMatches(final byte[] partial, final byte[] toValidate) {
        assert partial.length == 64;
        if (toValidate.length != 64) {
            return false;
        }

        // Compare up to byte 56 after which to transaction id comess
        for (int index = 0; index < 56; index++) {
            if (partial[index] != toValidate[index]) {
                return false;
            }
        }
        return true;
    }

}
