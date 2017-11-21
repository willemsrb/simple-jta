package nl.futureedge.simple.jta.jdbc.xa;

import javax.sql.XADataSource;
import org.hsqldb.jdbc.pool.JDBCXADataSource;
import org.junit.Assert;
import org.junit.Test;
import org.postgresql.xa.PGXADataSource;

public class JdbcXADataSourceFactoryTest {

    @Test
    public void testHsqldb() throws Exception {
        final JdbcXADataSourceFactory subject = new JdbcXADataSourceFactory();
        subject.setDriver("org.hsqldb.jdbc.pool.JDBCXADataSource");
        subject.setHost("host");
        subject.setPort(1234);
        subject.setDatabase("database");
        subject.setUser("user");
        subject.setPassword("password");
        subject.setAdditionalProperties(null);
        subject.afterPropertiesSet();

        Assert.assertNotNull(subject.getObject());
        Assert.assertTrue(subject.getObject() instanceof XADataSource);
        Assert.assertTrue(subject.getObject() instanceof JDBCXADataSource);
        Assert.assertEquals(XADataSource.class, subject.getObjectType());
        Assert.assertTrue(subject.isSingleton());
    }

    @Test
    public void testPostgresql() throws Exception {
        final JdbcXADataSourceFactory subject = new JdbcXADataSourceFactory();
        subject.setDriver("org.postgresql.xa.PGXADataSource");
        subject.setHost("host");
        subject.setPort(1234);
        subject.setDatabase("database");
        subject.setUser("user");
        subject.setPassword("password");
        subject.setAdditionalProperties(null);
        subject.afterPropertiesSet();

        Assert.assertNotNull(subject.getObject());
        Assert.assertTrue(subject.getObject() instanceof XADataSource);
        Assert.assertTrue(subject.getObject() instanceof PGXADataSource);
        Assert.assertEquals(XADataSource.class, subject.getObjectType());
        Assert.assertTrue(subject.isSingleton());
    }

    @Test
    public void testIllegal() throws Exception {
        final JdbcXADataSourceFactory subject = new JdbcXADataSourceFactory();
        subject.setDriver("not.a.valid.XADataSource");
        subject.setHost("host");
        subject.setPort(1234);
        subject.setDatabase("database");
        subject.setUser("user");
        subject.setPassword("password");
        subject.setAdditionalProperties(null);
        try {
            subject.afterPropertiesSet();
            Assert.fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {
            // Expected
        }
    }
}
