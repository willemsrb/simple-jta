package nl.futureedge.simple.jta.store.jdbc;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import nl.futureedge.simple.jta.JtaTransactionManager;
import nl.futureedge.simple.jta.store.jdbc.sql.JdbcSqlTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;

public final class JdbcDatabaseInitializer implements InitializingBean, BeanFactoryPostProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(JdbcTransactionStore.class);

    private String jdbcDriver;
    private String jdbcUrl;
    private String jdbcUser;
    private String jdbcPassword;

    /**
     * Set the classname of the JDBC database driver to load (can be left empty for JDBC 4.0+ drivers).
     * @param jdbcDriver JDBC driver class name
     */
    public void setDriver(final String jdbcDriver) {
        this.jdbcDriver = jdbcDriver;
    }

    /**
     * Set the JDBC url.
     * @param jdbcUrl JDBC url
     */
    @Required
    public void setUrl(final String jdbcUrl) {
        this.jdbcUrl = jdbcUrl;
    }

    /**
     * Set the username to connect.
     * @param jdbcUser username
     */
    public void setUser(final String jdbcUser) {
        this.jdbcUser = jdbcUser;
    }

    /**
     * Set the password to connect (only used if username is filled).
     * @param jdbcPassword password
     */
    public void setPassword(final String jdbcPassword) {
        this.jdbcPassword = jdbcPassword;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        try (final Connection connection = JdbcHelper.createConnectionSupplier(jdbcDriver, jdbcUrl, jdbcUser, jdbcPassword).getConnection()) {
            create(connection, JdbcSqlTemplate.determineSqlTemplate(jdbcUrl));
        }
    }

    @Override
    public void postProcessBeanFactory(final ConfigurableListableBeanFactory beanFactory) {
        // List all JdbcDatabaseInitializer beans
        final List<String> initializerNames = asList(beanFactory.getBeanNamesForType(JdbcDatabaseInitializer.class));

        // For each JtaTransactionManager, add all JdbcDatabaseInitializer beans as depends-on
        final String[] transactionManagerNames = beanFactory.getBeanNamesForType(JtaTransactionManager.class);
        for (final String transactionManagerName : transactionManagerNames) {
            final BeanDefinition transactionManagerDefinition = beanFactory.getBeanDefinition(transactionManagerName);
            final List<String> dependsOn = new ArrayList<>();
            dependsOn.addAll(asList(transactionManagerDefinition.getDependsOn()));
            dependsOn.addAll(initializerNames);
            transactionManagerDefinition.setDependsOn(dependsOn.toArray(new String[0]));
        }
    }

    private List<String> asList(final String[] values) {
        if (values == null) {
            return Collections.emptyList();
        } else {
            return Arrays.asList(values);
        }
    }

    public static final void create(final Connection connection, final JdbcSqlTemplate sqlTemplate) throws SQLException {
        try (final Statement statement = connection.createStatement()) {
            createTransactionIdSequence(statement, sqlTemplate);
            createTransactionTable(statement, sqlTemplate);
            createResourceTable(statement, sqlTemplate);
        }
    }

    private static void createTransactionIdSequence(final Statement statement, final JdbcSqlTemplate sqlTemplate) {
        try {
            statement.execute(sqlTemplate.createTransactionIdSequence());
        } catch (SQLException e) {
            LOGGER.info("Could not create transaction id sequence; ignoring exception ...", e);
        }
    }

    private static void createTransactionTable(final Statement statement, final JdbcSqlTemplate sqlTemplate) {
        try {
            statement.execute(sqlTemplate.createTransactionTable());
        } catch (SQLException e) {
            LOGGER.info("Could not create transaction table; ignoring exception ...", e);
        }
    }

    private static void createResourceTable(final Statement statement, final JdbcSqlTemplate sqlTemplate) {
        try {
            statement.execute(sqlTemplate.createResourceTable());
        } catch (SQLException e) {
            LOGGER.info("Could not create resource table; ignoring exception ...", e);
        }
    }

}
