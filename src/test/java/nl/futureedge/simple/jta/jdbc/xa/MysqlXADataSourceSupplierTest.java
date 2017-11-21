package nl.futureedge.simple.jta.jdbc.xa;

import com.mysql.jdbc.jdbc2.optional.MysqlXADataSource;
import java.util.Properties;
import org.junit.Assert;
import org.junit.Test;

public class MysqlXADataSourceSupplierTest {

    @Test
    public void test() throws Exception {
        final MysqlXADataSourceSupplier subject = new MysqlXADataSourceSupplier();
        Assert.assertEquals(MysqlXADataSource.class.getName(), subject.getDriver());

        final Properties additionalProperties = new Properties();
        additionalProperties.setProperty("sslmode", "value");
        final MysqlXADataSource xaDataSource = (MysqlXADataSource) subject.getXaDataSource("host", 1234, "database", "user", "password", additionalProperties);

        Assert.assertNotNull(xaDataSource);
        Assert.assertEquals("host", xaDataSource.getServerName());
        Assert.assertEquals(1234, xaDataSource.getPortNumber());
        Assert.assertEquals("database", xaDataSource.getDatabaseName());
        Assert.assertEquals("user", xaDataSource.getUser());
    }

    @Test
    public void minimal() throws Exception {
        final MysqlXADataSourceSupplier subject = new MysqlXADataSourceSupplier();
        Assert.assertEquals(MysqlXADataSource.class.getName(), subject.getDriver());

        final MysqlXADataSource xaDataSource = (MysqlXADataSource) subject.getXaDataSource("host", null, "database", null, null, null);

        Assert.assertNotNull(xaDataSource);
        Assert.assertEquals("host", xaDataSource.getServerName());
        Assert.assertEquals(3306, xaDataSource.getPortNumber());
        Assert.assertEquals("database", xaDataSource.getDatabaseName());
        Assert.assertEquals(null, xaDataSource.getUser());
    }
}
