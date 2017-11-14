package nl.futureedge.simple.jta.store.jdbc;

/*-
 * #%L
 * Simple JTA
 * %%
 * Copyright (C) 2017 Future Edge IT
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
import java.io.IOException;
import java.net.ServerSocket;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;
import javax.sql.DataSource;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.GenericXmlApplicationContext;
import org.springframework.core.env.PropertiesPropertySource;

public abstract class AbstractJdbcTransactionStoreIT {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractJdbcTransactionStoreIT.class);

    private static Properties portProperties = new Properties();
    private static GenericXmlApplicationContext databaseContext;

    protected JdbcTransactionStore subject;

    @BeforeClass
    public static void startDependencies() throws IOException {
        try (ServerSocket databasePort = new ServerSocket(0)) {
            LOGGER.info("Configuring database to port: {}", databasePort.getLocalPort());
            portProperties.setProperty("test.database.port", Integer.toString(databasePort.getLocalPort()));
        }

        // Start DB
        databaseContext = new GenericXmlApplicationContext();
        databaseContext.load("classpath:embedded-database.xml");
        databaseContext.getEnvironment().getPropertySources().addLast(new PropertiesPropertySource("configuration", portProperties));
        databaseContext.refresh();
    }

    @AfterClass
    public static void stopTestContext() {
        if (databaseContext != null) {
            try {
                databaseContext.close();
            } catch (final Exception e) {
                LOGGER.warn("Problem closing DATABASE context", e);
            }
        }
    }

    @Before
    public void setup() throws Exception {
        subject = new JdbcTransactionStore();
        subject.setCreate(true);
        subject.setDriver(null);
        subject.setUrl("jdbc:hsqldb:hsql://localhost:" + portProperties.getProperty("test.database.port") + "/trans");
        subject.setUser("sa");
        subject.setPassword("");
        subject.setSqlTemplate(null);
        setupSubject(subject);

        subject.afterPropertiesSet();
    }

    abstract void setupSubject(JdbcTransactionStore subject);

    @After
    public void destroy() throws Exception {
        subject.destroy();
    }

    protected void debugTables() throws SQLException {
        final DataSource dataSource = databaseContext.getBean("transDataSource", DataSource.class);
        try (final Connection connection = dataSource.getConnection()) {
            LOGGER.debug("TRANSACTIONS");
            try (final PreparedStatement statement = connection.prepareStatement("select id, status, created, updated from transactions order by id");
                 final ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    LOGGER.debug(String.format("| %s | %s | %s | %s |", result.getLong(1), result.getString(2), result.getDate(3), result.getDate(4)));
                }
            }

            LOGGER.debug("TRANSACTION_RESOURCES");
            try (final PreparedStatement statement = connection
                    .prepareStatement("select transaction_id, name, status, cause, created, updated from transaction_resources order by transaction_id, name");
                 final ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    LOGGER.debug(
                            String.format("| %s | %s | %s | %s | %s | %s |", result.getLong(1), result.getString(2), result.getString(3), result.getString(4),
                                    result.getDate(5), result.getDate(6)));
                }
            }
        }
    }

    protected String selectStatus(long transactionId) throws SQLException {
        final DataSource dataSource = databaseContext.getBean("transDataSource", DataSource.class);
        try (final Connection connection = dataSource.getConnection()) {
            final PreparedStatement statement = connection.prepareStatement("select status from transactions where id = ?");
            statement.setLong(1, transactionId);

            final ResultSet result = statement.executeQuery();
            if (result.next()) {
                return result.getString(1);
            } else {
                return null;
            }
        }
    }

    protected String selectStatus(long transactionId, String resource) throws SQLException {
        final DataSource dataSource = databaseContext.getBean("transDataSource", DataSource.class);
        try (final Connection connection = dataSource.getConnection()) {
            final PreparedStatement statement = connection.prepareStatement("select status from transaction_resources where transaction_id = ? and name = ?");
            statement.setLong(1, transactionId);
            statement.setString(2, resource);

            final ResultSet result = statement.executeQuery();
            if (result.next()) {
                return result.getString(1);
            } else {
                return null;
            }
        }
    }

}
