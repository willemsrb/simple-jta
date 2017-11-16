package nl.futureedge.simple.jta.jdbc.xa;

import java.sql.SQLException;
import java.util.Properties;
import javax.sql.XADataSource;
import org.postgresql.xa.PGXADataSource;

/**
 * PostgreSQL XA DataSource supplier.
 */
public final class PostgresqlXADataSourceSupplier implements JdbcXADataSourceSupplier {

    @Override
    public String getDriver() {
        return "org.postgresql.xa.PGXADataSource";
    }

    @Override
    public XADataSource getXaDataSource(final String host, final Integer port, final String database, final String user, final String password,
                                        final Properties additionalProperties)
            throws SQLException {
        final PGXADataSource xaDataSource = new PGXADataSource();

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
            for (final String additionalProperty : additionalProperties.stringPropertyNames()) {
                xaDataSource.setProperty(additionalProperty, additionalProperties.getProperty(additionalProperty));
            }
        }
        return xaDataSource;
    }
}
