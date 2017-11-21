package nl.futureedge.simple.jta.jdbc.xa;

import java.util.Properties;
import org.hsqldb.jdbc.pool.JDBCXADataSource;
import org.junit.Assert;
import org.junit.Test;

public class HsqldbXADataSourceSupplierTest {

    @Test
    public void test() throws Exception {
        final HsqldbXADataSourceSupplier subject = new HsqldbXADataSourceSupplier();
        Assert.assertEquals(JDBCXADataSource.class.getName(), subject.getDriver());

        final Properties additionalProperties = new Properties();
        additionalProperties.setProperty("other", "value");
        final JDBCXADataSource xaDataSource = (JDBCXADataSource) subject.getXaDataSource("host", 1234, "database", "user", "password", additionalProperties);

        Assert.assertNotNull(xaDataSource);
        Assert.assertEquals("jdbc:hsqldb:hsql://host:1234/database", xaDataSource.getUrl());
        Assert.assertEquals("user", xaDataSource.getUser());
    }

    @Test
    public void minimal() throws Exception {
        final HsqldbXADataSourceSupplier subject = new HsqldbXADataSourceSupplier();
        Assert.assertEquals(JDBCXADataSource.class.getName(), subject.getDriver());

        final JDBCXADataSource xaDataSource = (JDBCXADataSource) subject.getXaDataSource("host", null, "database", null, null, null);

        Assert.assertNotNull(xaDataSource);
        Assert.assertEquals("jdbc:hsqldb:hsql://host/database", xaDataSource.getUrl());
        Assert.assertEquals(null, xaDataSource.getUser());
    }
}
