package nl.futureedge.simple.jta;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import javax.transaction.xa.Xid;

/**
 * Jta XID; not for external use!
 */
public final class JtaXid implements Xid {

    private static final int SIMPLE_JTA_FORMAT = 0x1ee3;

    private static final Charset UTF8 = Charset.forName("UTF-8");

    private final AtomicLong BRANCH_SEQUENCE = new AtomicLong();

    private String transactionManager;
    private long transactionId;
    private Long branchId;

    private final int format;
    private final byte[] globalTransactionId;
    private final byte[] branchQualifier;

    /**
     * Constructor (for XID created by this transaction manager).
     * @param transactionManager transaction manager unique name
     * @param transactionId transaction id
     */
    public JtaXid(final String transactionManager, final long transactionId) {
        this(transactionManager, transactionId, null);
    }

    private JtaXid(final String transactionManager, final long transactionId, final Long branchId) {
        this.transactionManager = transactionManager;
        this.transactionId = transactionId;
        this.branchId = branchId;

        this.format = SIMPLE_JTA_FORMAT;
        this.globalTransactionId = createGlobalTransactionId(this.transactionManager, this.transactionId);
        this.branchQualifier = createBranchId(this.branchId);
    }

    private static byte[] createGlobalTransactionId(final String transactionManager, final long transactionId) {
        final ByteBuffer buffer = ByteBuffer.allocate(64);

        // Add transaction manager
        final byte[] tmBytes = transactionManager.getBytes(UTF8);
        int tmLength = tmBytes.length;
        buffer.putInt(tmLength);
        buffer.put(tmBytes, 0, Math.min(52, tmLength));

        // Add transaction id
        buffer.position(56);
        buffer.putLong(transactionId);

        return buffer.array();
    }

    private static byte[] createBranchId(final Long branchId) {
        if (branchId == null) {
            return new byte[]{};
        }

        final ByteBuffer buffer = ByteBuffer.allocate(8);
        buffer.putLong(branchId);

        return buffer.array();
    }

    /**
     * @return the transaction manager unique name
     */
    public String getTransactionManager() {
        return transactionManager;
    }

    /**
     * @return transaction id
     */
    public long getTransactionId() {
        return transactionId;
    }

    @Override
    public int getFormatId() {
        return format;
    }

    @Override
    public byte[] getGlobalTransactionId() {
        return globalTransactionId;
    }

    @Override
    public byte[] getBranchQualifier() {
        return branchQualifier;
    }

    /**
     * Create a branch XID for this global transaction id.
     */
    public JtaXid createBranchXid() {
        assert branchId == null;
        return new JtaXid(this.transactionManager, this.transactionId, BRANCH_SEQUENCE.incrementAndGet());
    }

    /**
     * Filter the given list of XID's to retrieve the XID's for this transaction manager.
     * @param xids list of XID's
     * @param transactionManager transaction manager unique name
     */
    public static List<JtaXid> filterRecoveryXids(final Xid[] xids, final String transactionManager) {
        final List<JtaXid> result = new ArrayList<>();
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

                result.add(new JtaXid(transactionManager, transactionId, branchId));
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

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final JtaXid jtaXid = (JtaXid) o;
        return transactionId == jtaXid.transactionId &&
                Objects.equals(transactionManager, jtaXid.transactionManager) &&
                Objects.equals(branchId, jtaXid.branchId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(transactionManager, transactionId, branchId);
    }

    @Override
    public String toString() {
        return "JtaXid{" +
                "transactionManager='" + transactionManager + '\'' +
                ", transactionId=" + transactionId +
                ", branchId=" + branchId +
                '}';
    }
}
