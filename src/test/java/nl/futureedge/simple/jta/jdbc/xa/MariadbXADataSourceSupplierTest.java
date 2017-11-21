package nl.futureedge.simple.jta.jdbc.xa;

import java.util.Properties;
import org.junit.Assert;
import org.junit.Test;
import org.mariadb.jdbc.MariaDbDataSource;

public class MariadbXADataSourceSupplierTest {

    @Test
    public void test() throws Exception {
        final MariadbXADataSourceSupplier subject = new MariadbXADataSourceSupplier();
        Assert.assertEquals(MariaDbDataSource.class.getName(), subject.getDriver());

        final Properties additionalProperties = new Properties();
        additionalProperties.setProperty("sslmode", "value");
        final MariaDbDataSource xaDataSource = (MariaDbDataSource) subject.getXaDataSource("host", 1234, "database", "user", "password", additionalProperties);

        Assert.assertNotNull(xaDataSource);
        Assert.assertEquals("host", xaDataSource.getServerName());
        Assert.assertEquals(1234, xaDataSource.getPortNumber());
        Assert.assertEquals("database", xaDataSource.getDatabaseName());
        Assert.assertEquals("user", xaDataSource.getUser());
    }

    @Test
    public void minimal() throws Exception {
        final MariadbXADataSourceSupplier subject = new MariadbXADataSourceSupplier();
        Assert.assertEquals(MariaDbDataSource.class.getName(), subject.getDriver());

        final MariaDbDataSource xaDataSource = (MariaDbDataSource) subject.getXaDataSource("host", null, "database", null, null, null);

        Assert.assertNotNull(xaDataSource);
        Assert.assertEquals("host", xaDataSource.getServerName());
        Assert.assertEquals(3306, xaDataSource.getPortNumber());
        Assert.assertEquals("database", xaDataSource.getDatabaseName());
        Assert.assertEquals(null, xaDataSource.getUser());
    }
}
