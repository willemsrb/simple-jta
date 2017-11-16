package nl.futureedge.simple.jta.jdbc.xa;

import com.mysql.jdbc.jdbc2.optional.MysqlXADataSource;
import java.sql.SQLException;
import java.util.Properties;
import javax.sql.XADataSource;

/**
 * MySQL XA DataSource supplier.
 */
public final class MysqlXADataSourceSupplier implements JdbcXADataSourceSupplier {

    @Override
    public String getDriver() {
        return "com.mysql.jdbc.jdbc2.optional.MysqlXADataSource";
    }

    @Override
    public XADataSource getXaDataSource(final String host, final Integer port, final String database, final String user, final String password,
                                        final Properties additionalProperties)
            throws SQLException {

        final MysqlXADataSource xaDataSource = new MysqlXADataSource();

        xaDataSource.setServerName(host);
        if (port != null) {
            xaDataSource.setPortNumber(port);
        }
        xaDataSource.setDatabaseName(database);

        if (user != null) {
            xaDataSource.setUser(user);
            xaDataSource.setPassword(password);
        }

        if (additionalProperties != null) {
            // Ignore
        }
        return xaDataSource;
    }
}
