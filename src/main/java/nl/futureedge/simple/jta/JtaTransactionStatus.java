package nl.futureedge.simple.jta;

import javax.transaction.Status;

public enum JtaTransactionStatus {

    ACTIVE(Status.STATUS_ACTIVE),

    SUSPENDED(Status.STATUS_NO_TRANSACTION),

    PREPARING(Status.STATUS_PREPARING),
    PREPARED(Status.STATUS_PREPARED),

    COMMITTING(Status.STATUS_COMMITTING),
    COMMITTED(Status.STATUS_COMMITTED),
    COMMIT_FAILED(Status.STATUS_UNKNOWN),

    MARKED_ROLLBACK(Status.STATUS_MARKED_ROLLBACK),

    ROLLING_BACK(Status.STATUS_ROLLING_BACK),
    ROLLED_BACK(Status.STATUS_ROLLEDBACK),
    ROLLBACK_FAILED(Status.STATUS_UNKNOWN);

    private final int jtaStatus;

    JtaTransactionStatus(int jtaStatus) {
        this.jtaStatus = jtaStatus;
    }


    int getJtaStatus() {
        return jtaStatus;
    }

}
