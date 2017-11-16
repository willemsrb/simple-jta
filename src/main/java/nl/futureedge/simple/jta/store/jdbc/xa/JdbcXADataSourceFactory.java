package nl.futureedge.simple.jta.store.jdbc.xa;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.ServiceLoader;
import javax.sql.XADataSource;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Required;

public final class JdbcXADataSourceFactory implements InitializingBean, FactoryBean<XADataSource> {

    private static final Map<String, JdbcXADataSourceSupplier> SUPPLIERS = loadSuppliers();

    private String driver;
    private String host;
    private Integer port;
    private String database;
    private String user;
    private String password;
    private Properties additionalProperties;

    private XADataSource xaDataSource;

    @Required
    public void setDriver(final String driver) {
        this.driver = driver;
    }

    public void setHost(final String host) {
        this.host = host;
    }

    public void setPort(final Integer port) {
        this.port = port;
    }

    public void setDatabase(final String database) {
        this.database = database;
    }

    public void setUser(final String user) {
        this.user = user;
    }

    public void setPassword(final String password) {
        this.password = password;
    }

    public void setAdditionalProperties(Properties additionalProperties) {
        this.additionalProperties = additionalProperties;
    }

    /* *** SERVICES *** */

    private static Map<String, JdbcXADataSourceSupplier> loadSuppliers() {
        final ServiceLoader<JdbcXADataSourceSupplier> serviceLoader = ServiceLoader.load(JdbcXADataSourceSupplier.class);
        final Map<String, JdbcXADataSourceSupplier> suppliers = new HashMap<>();
        serviceLoader.forEach(supplier -> suppliers.put(supplier.getDriver(), supplier));
        return suppliers;
    }

    /* *** LOGIC *** */

    @Override
    public void afterPropertiesSet() throws Exception {
        final JdbcXADataSourceSupplier supplier = SUPPLIERS.get(driver);
        if (supplier == null) {
            throw new IllegalArgumentException("No XADataSource supplier known for driver '" + driver + "'");
        }

        xaDataSource = supplier.getXaDataSource(host, port, database, user, password, additionalProperties);
    }

    /* *** FACTORY *** */

    @Override
    public XADataSource getObject() throws Exception {
        return xaDataSource;
    }

    @Override
    public Class<?> getObjectType() {
        return XADataSource.class;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }
}
