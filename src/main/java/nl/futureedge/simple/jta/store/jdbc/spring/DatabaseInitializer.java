package nl.futureedge.simple.jta.store.jdbc.spring;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import nl.futureedge.simple.jta.store.jdbc.JdbcTransactionStore;
import nl.futureedge.simple.jta.store.jdbc.sql.JdbcSqlTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Required;

public final class DatabaseInitializer implements InitializingBean {

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
        // Load driver
        if (jdbcDriver != null && !"".equals(jdbcDriver)) {
            Class.forName(jdbcDriver);
        }

        // Supplier
        final Connection connection;
        if (jdbcUser != null && !"".equals(jdbcUser)) {
            connection = DriverManager.getConnection(jdbcUrl, jdbcUser, jdbcPassword);
        } else {
            connection = DriverManager.getConnection(jdbcUrl);
        }
        try {
            create(connection, JdbcSqlTemplate.determineSqlTemplate(jdbcUrl));
        } finally {
            connection.close();
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
