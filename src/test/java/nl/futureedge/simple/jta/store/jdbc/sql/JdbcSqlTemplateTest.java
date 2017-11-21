package nl.futureedge.simple.jta.store.jdbc.sql;

import org.junit.Assert;
import org.junit.Test;

public class JdbcSqlTemplateTest {

    @Test
    public void testHsqldb() {
        final JdbcSqlTemplate result = JdbcSqlTemplate.determineSqlTemplate("jdbc:hsqldb:hsql:localhost/jta");
        Assert.assertNotNull(result);
        Assert.assertTrue(result instanceof HsqldbSqlTemplate);
    }
    @Test
    public void testHsqldbInMemory() {
        final JdbcSqlTemplate result = JdbcSqlTemplate.determineSqlTemplate("jdbc:hsqldb:mem/jta");
        Assert.assertNotNull(result);
        Assert.assertTrue(result instanceof HsqldbSqlTemplate);
    }

    @Test
    public void testMysql() {
        final JdbcSqlTemplate result = JdbcSqlTemplate.determineSqlTemplate("jdbc:mysql:localhost/jta");
        Assert.assertNotNull(result);
        Assert.assertTrue(result instanceof MysqlSqlTemplate);
    }

    @Test
    public void testMariadb() {
        final JdbcSqlTemplate result = JdbcSqlTemplate.determineSqlTemplate("jdbc:mariadb:localhost/jta");
        Assert.assertNotNull(result);
        Assert.assertTrue(result instanceof MysqlSqlTemplate);
    }

    @Test
    public void testPostgresql() {
        final JdbcSqlTemplate result = JdbcSqlTemplate.determineSqlTemplate("jdbc:postgresql:localhost/jta");
        Assert.assertNotNull(result);
        Assert.assertTrue(result instanceof PostgresqlSqlTemplate);
    }

    @Test
    public void testDefault() {
        final JdbcSqlTemplate result = JdbcSqlTemplate.determineSqlTemplate("jdbc:oracle:thin:@localhost:1521:jta");
        Assert.assertNotNull(result);
        Assert.assertEquals(DefaultSqlTemplate.class, result.getClass());
    }

    @Test
    public void testUnknown() {
        final JdbcSqlTemplate result = JdbcSqlTemplate.determineSqlTemplate("ILLEGAL!?");
        Assert.assertNotNull(result);
        Assert.assertEquals(DefaultSqlTemplate.class, result.getClass());
    }
}
