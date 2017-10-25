package nl.futureedge.simple.jta.store.jdbc.sql;

public class DefaultSqlTemplate implements JdbcSqlTemplate {

    private String createTransactionIdSequence = "create sequence transaction_seq";

    private String selectNextTransactionId = "select nextval('transaction_seq')";

    private String createTransactionTable = "create table transactions(\n"
            + "    id bigint not null,\n"
            + "    status varchar(30) not null,\n"
            + "    created timestamp not null,\n"
            + "    updated timestamp not null\n"
            + ")";

    private String updateTransactionStatus = "update transactions set status=?, updated=? where id=?";

    private String insertTransactionStatus = "insert into transactions(id, status, created, updated) values (?, ?, ?, ?)";

    private String selectTransactionStatus = "select status from transactions where id=?";

    private String createResourceTable = "create table transaction_resources(\n"
            + "    transaction_id bigint not null,\n"
            + "    name varchar(30) not null,\n"
            + "    status varchar(30) not null,\n"
            + "    cause text,\n"
            + "    created timestamp not null,\n"
            + "    updated timestamp not null\n"
            + ")";

    private String updateResourceStatus = "update transaction_resources set status=?, cause=?, updated=? where transaction_id=? and name=?";

    private String insertResourceStatus = "insert into transaction_resources(transaction_id, name, status, cause, created, updated) values (?,?, ?, ?, ?, ?)";

    private String deleteResourceStatus = "delete from transaction_resources where transaction_id=?";

    private String deleteTransactionStatus = "delete from transactions where id=?";

    public final void setCreateTransactionIdSequence(final String createTransactionIdSequence) {
        this.createTransactionIdSequence = createTransactionIdSequence;
    }

    public final void setSelectNextTransactionId(final String selectNextTransactionId) {
        this.selectNextTransactionId = selectNextTransactionId;
    }

    public final void setCreateTransactionTable(final String createTransactionTable) {
        this.createTransactionTable = createTransactionTable;
    }

    public final void setUpdateTransactionStatus(final String updateTransactionStatus) {
        this.updateTransactionStatus = updateTransactionStatus;
    }

    public final void setInsertTransactionStatus(final String insertTransactionStatus) {
        this.insertTransactionStatus = insertTransactionStatus;
    }

    public final void setSelectTransactionStatus(final String selectTransactionStatus) {
        this.selectTransactionStatus = selectTransactionStatus;
    }

    public final void setCreateResourceTable(final String createResourceTable) {
        this.createResourceTable = createResourceTable;
    }

    public final void setUpdateResourceStatus(final String updateResourceStatus) {
        this.updateResourceStatus = updateResourceStatus;
    }

    public final void setInsertResourceStatus(final String insertResourceStatus) {
        this.insertResourceStatus = insertResourceStatus;
    }

    public final void setDeleteResourceStatus(final String deleteResourceStatus) {
        this.deleteResourceStatus = deleteResourceStatus;
    }

    public final void setDeleteTransactionStatus(final String deleteTransactionStatus) {
        this.deleteTransactionStatus = deleteTransactionStatus;
    }

    @Override
    public final String createTransactionIdSequence() {
        return createTransactionIdSequence;
    }

    @Override
    public final String selectNextTransactionId() {
        return selectNextTransactionId;
    }

    @Override
    public final String createTransactionTable() {
        return createTransactionTable;
    }

    @Override
    public final String updateTransactionStatus() {
        return updateTransactionStatus;
    }

    @Override
    public final String insertTransactionStatus() {
        return insertTransactionStatus;
    }

    @Override
    public final String selectTransactionStatus() {
        return selectTransactionStatus;
    }

    @Override
    public final String createResourceTable() {
        return createResourceTable;
    }

    @Override
    public final String updateResourceStatus() {
        return updateResourceStatus;
    }

    @Override
    public final String insertResourceStatus() {
        return insertResourceStatus;
    }

    @Override
    public final String deleteResourceStatus() {
        return deleteResourceStatus;
    }

    @Override
    public final String deleteTransactionStatus() {
        return deleteTransactionStatus;
    }
}
