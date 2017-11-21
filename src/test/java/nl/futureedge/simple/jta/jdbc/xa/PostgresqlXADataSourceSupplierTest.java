package nl.futureedge.simple.jta.jdbc.xa;

import java.util.Properties;
import org.junit.Assert;
import org.junit.Test;
import org.postgresql.xa.PGXADataSource;

public class PostgresqlXADataSourceSupplierTest {

    @Test
    public void test() throws Exception {
        final PostgresqlXADataSourceSupplier subject = new PostgresqlXADataSourceSupplier();
        Assert.assertEquals(PGXADataSource.class.getName(), subject.getDriver());

        final Properties additionalProperties = new Properties();
        additionalProperties.setProperty("sslmode", "value");
        final PGXADataSource xaDataSource = (PGXADataSource) subject.getXaDataSource("host", 1234, "database", "user", "password", additionalProperties);

        Assert.assertNotNull(xaDataSource);
        Assert.assertEquals("host", xaDataSource.getServerName());
        Assert.assertEquals(1234, xaDataSource.getPortNumber());
        Assert.assertEquals("database", xaDataSource.getDatabaseName());
        Assert.assertEquals("user", xaDataSource.getUser());
        Assert.assertEquals("password", xaDataSource.getPassword());
        Assert.assertEquals("value",xaDataSource.getSslMode());
    }

    @Test
    public void minimal() throws Exception {
        final PostgresqlXADataSourceSupplier subject = new PostgresqlXADataSourceSupplier();
        Assert.assertEquals(PGXADataSource.class.getName(), subject.getDriver());

        final PGXADataSource xaDataSource = (PGXADataSource) subject.getXaDataSource("host", null, "database", null, null, null);

        Assert.assertNotNull(xaDataSource);
        Assert.assertEquals("host", xaDataSource.getServerName());
        Assert.assertEquals(0, xaDataSource.getPortNumber());
        Assert.assertEquals("database", xaDataSource.getDatabaseName());
        Assert.assertEquals(null, xaDataSource.getUser());
        Assert.assertEquals(null, xaDataSource.getPassword());
        Assert.assertEquals(null,xaDataSource.getSslMode());
    }
}
