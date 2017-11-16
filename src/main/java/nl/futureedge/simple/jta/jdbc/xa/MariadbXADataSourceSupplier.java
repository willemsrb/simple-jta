package nl.futureedge.simple.jta.jdbc.xa;

import java.sql.SQLException;
import java.util.Properties;
import javax.sql.XADataSource;
import org.mariadb.jdbc.MariaDbDataSource;

/**
 * MySQL XA DataSource supplier.
 */
public final class MariadbXADataSourceSupplier implements JdbcXADataSourceSupplier {

    @Override
    public String getDriver() {
        return "org.mariadb.jdbc.MariaDbDataSource";
    }

    @Override
    public XADataSource getXaDataSource(final String host, final Integer port, final String database, final String user, final String password,
                                        final Properties additionalProperties)
            throws SQLException {

        final MariaDbDataSource xaDataSource = new MariaDbDataSource();

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
