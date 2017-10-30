package nl.futureedge.simple.jta.store.jdbc;

import nl.futureedge.simple.jta.store.JtaTransactionStoreException;
import org.junit.Test;

public class JdbcHelperIT {

    @Test
    public void openOk() throws Exception {
        final JdbcHelper subject = new JdbcHelper(null, "jdbc:hsqldb:mem:trans", null, null);
        try {
            subject.open();
        } finally {
            subject.close();
        }
    }

    @Test
    public void openOkWithDriver() throws Exception {
        final JdbcHelper subject = new JdbcHelper("org.hsqldb.jdbc.JDBCDriver", "jdbc:hsqldb:mem:trans", "", "");
        try {
            subject.open();
        } finally {
            subject.close();
        }
    }


    @Test
    public void openOkWithUsernameAndPassword() throws Exception {
        final JdbcHelper subject = new JdbcHelper("", "jdbc:hsqldb:mem:trans", "sa", "");
        try {
            subject.open();
        } finally {
            subject.close();
        }
    }

    @Test(expected = JtaTransactionStoreException.class)
    public void invalid() throws Exception {
        new JdbcHelper("", "jdbc:unknown:protocol", "sa", "").open();
    }
}
