package nl.futureedge.simple.jta.store.jdbc.sql;

/**
 * PostgreSQL specific sql template.
 */
public class PostgresqlSqlTemplate extends DefaultSqlTemplate {

    /**
     * Constructor.
     */
    public PostgresqlSqlTemplate() {
        super();
        setCreateResourceTable(createResourceTable().replaceAll("clob", "text"));
        setSelectNextTransactionId("select nextval('transaction_seq')");
    }
}
