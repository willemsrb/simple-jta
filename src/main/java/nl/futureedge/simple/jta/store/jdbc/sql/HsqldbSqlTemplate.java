package nl.futureedge.simple.jta.store.jdbc.sql;

/**
 * HSQLDB specific sql template.
 */
public class HsqldbSqlTemplate extends DefaultSqlTemplate {

    /**
     * Constructor.
     */
    public HsqldbSqlTemplate() {
        setSelectNextTransactionId("call next value for transaction_seq");
    }
}
