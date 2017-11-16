package nl.futureedge.simple.jta.jdbc.xa;

import java.sql.SQLException;
import java.util.Properties;
import javax.sql.XADataSource;

public interface JdbcXADataSourceSupplier {

    String getDriver();

    XADataSource getXaDataSource(String host, Integer port, String database, String user, String password, Properties additionalProperties) throws SQLException;
}
