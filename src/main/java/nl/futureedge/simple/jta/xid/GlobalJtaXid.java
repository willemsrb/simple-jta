package nl.futureedge.simple.jta.xid;


import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Global Jta XID; not for external use!
 */
public final class GlobalJtaXid extends BaseJtaXid {

    private final AtomicLong branchSequence = new AtomicLong();

    /**
     * Constructor (for XID created by this transaction manager).
     * @param transactionManager transaction manager unique name
     * @param transactionId transaction id
     */
    public GlobalJtaXid(final String transactionManager, final long transactionId) {
        super(transactionManager, transactionId, null);
    }


    /**
     * Create a branch XID for this global transaction id.
     * @return a branch XID (using a new branch id)
     */
    public BranchJtaXid createBranchXid() {
        return new BranchJtaXid(getTransactionManager(), getTransactionId(), branchSequence.incrementAndGet());
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof GlobalJtaXid)) {
            return false;
        }
        final GlobalJtaXid jtaXid = (GlobalJtaXid) o;
        return getTransactionId() == jtaXid.getTransactionId() &&
                Objects.equals(getTransactionManager(), jtaXid.getTransactionManager());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getTransactionManager(), getTransactionId());
    }

    @Override
    public String toString() {
        return "GlobalJtaXid{" +
                "transactionManager='" + getTransactionManager() + '\'' +
                ", transactionId=" + getTransactionId() +
                '}';
    }

}
