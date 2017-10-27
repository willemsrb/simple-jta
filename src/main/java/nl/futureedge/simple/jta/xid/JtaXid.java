package nl.futureedge.simple.jta.xid;

import javax.transaction.xa.Xid;

/**
 * Jta XID; not for external use!
 */
public interface JtaXid extends Xid {

    /**
     * @return the transaction manager unique name
     */
    public String getTransactionManager();

    /**
     * @return transaction id
     */
    public long getTransactionId();

}
