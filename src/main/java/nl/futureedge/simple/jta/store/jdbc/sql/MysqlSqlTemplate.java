package nl.futureedge.simple.jta.store.jdbc.sql;

/**
 * MySQL specific sql template.
 */
public class MysqlSqlTemplate extends DefaultSqlTemplate {

    /**
     * Constructor.
     */
    public MysqlSqlTemplate() {
        setCreateResourceTable(createResourceTable().replaceAll("clob", "text"));
    }
}
