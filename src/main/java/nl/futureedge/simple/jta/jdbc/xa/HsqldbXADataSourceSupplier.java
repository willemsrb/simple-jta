package nl.futureedge.simple.jta.jdbc.xa;

import java.sql.SQLException;
import java.util.Properties;
import javax.sql.XADataSource;
import org.hsqldb.jdbc.pool.JDBCXADataSource;

/**
 * HsqlDB XA DataSource supplier.
 */
public final class HsqldbXADataSourceSupplier implements JdbcXADataSourceSupplier {

    @Override
    public String getDriver() {
        return "org.hsqldb.jdbc.pool.JDBCXADataSource";
    }

    @Override
    public XADataSource getXaDataSource(final String host, final Integer port, final String database, final String user, final String password,
                                        final Properties additionalProperties)
            throws SQLException {

        final JDBCXADataSource xaDataSource = new JDBCXADataSource();
        final StringBuilder url = new StringBuilder("jdbc:hsqldb:hsql://");
        url.append(host);
        if (port != null) {
            url.append(":").append(port);
        }
        url.append("/").append(database);
        xaDataSource.setUrl(url.toString());

        if (user != null) {
            xaDataSource.setUser(user);
            xaDataSource.setPassword(password);
        }

        if (additionalProperties != null) {
            xaDataSource.setProperties(additionalProperties);
        }
        return xaDataSource;
    }
}
