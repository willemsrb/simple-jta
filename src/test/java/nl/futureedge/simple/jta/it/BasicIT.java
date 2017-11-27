package nl.futureedge.simple.jta.it;


import java.io.IOException;
import java.net.ServerSocket;
import java.util.Properties;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.TextMessage;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.support.GenericXmlApplicationContext;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Basic IT tests.
 */
@RunWith(Parameterized.class)
public class BasicIT {

    private static final Logger LOGGER = LoggerFactory.getLogger(BasicIT.class);

    private static Properties portProperties = new Properties();
    private static GenericXmlApplicationContext databaseContext;
    private static GenericXmlApplicationContext messagebrokerContext;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private JmsTemplate jmsTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final String testContextConfiguration;
    private GenericXmlApplicationContext testContext;

    @BeforeClass
    public static void startDependencies() throws IOException {
        try (ServerSocket databasePort = new ServerSocket(0)) {
            LOGGER.info("Configuring database to port: {}", databasePort.getLocalPort());
            portProperties.setProperty("test.database.port", Integer.toString(databasePort.getLocalPort()));
        }
        try (ServerSocket brokerPort = new ServerSocket(0)) {
            LOGGER.info("Configuring broker to port: {}", brokerPort.getLocalPort());
            portProperties.setProperty("test.broker.port", Integer.toString(brokerPort.getLocalPort()));
        }

        // Start DB
        databaseContext = new GenericXmlApplicationContext();
        databaseContext.load("classpath:embedded-database.xml");
        databaseContext.getEnvironment().getPropertySources().addLast(new PropertiesPropertySource("configuration", portProperties));
        databaseContext.refresh();

        // Start messagebroker
        messagebrokerContext = new GenericXmlApplicationContext();
        messagebrokerContext.load("classpath:embedded-broker.xml");
        messagebrokerContext.getEnvironment().getPropertySources().addLast(new PropertiesPropertySource("configuration", portProperties));
        messagebrokerContext.refresh();
    }

    @AfterClass
    public static void stopTestContext() {
        if (messagebrokerContext != null) {
            try {
                messagebrokerContext.close();
            } catch (final Exception e) {
                LOGGER.warn("Problem closing BROKER context", e);
            }
        }
        if (databaseContext != null) {
            try {
                databaseContext.close();
            } catch (final Exception e) {
                LOGGER.warn("Problem closing DATABASE context", e);
            }
        }
    }

    @Parameterized.Parameters(name = "{0}")
    public static Object[] data() {
        return new Object[]{
                "classpath:test-file-context.xml",
                "classpath:test-jdbc-hsqldb-context.xml",
                "classpath:test-jdbc-mariadb-context.xml",
                "classpath:test-jdbc-postgres-context.xml",
                "classpath:test-beans-context.xml",
        };
    }

    public BasicIT(String testContextConfiguration) {
        this.testContextConfiguration = testContextConfiguration;
    }

    @Before
    public void start() {
        // Create test context
        testContext = new GenericXmlApplicationContext();
        //testContext.load("classpath:test-context.xml");
        testContext.load(testContextConfiguration);
        testContext.getEnvironment().getPropertySources().addLast(new PropertiesPropertySource("configuration", portProperties));
        testContext.refresh();

        testContext.getAutowireCapableBeanFactory().autowireBean(this);
    }

    @After
    public void shutdown() throws InterruptedException {
        if (testContext != null) {
            try {
                testContext.close();
            } catch (final Exception e) {
                LOGGER.warn("Problem closing TEST context", e);
            }
        }
    }


    @Test
    public void nothing() {
        transactionTemplate.execute(status -> {
            return null;
        });
    }

    @Test
    public void jms() {
        transactionTemplate.execute(status -> {
            // Send message
            jmsTemplate.send("QueueOne", (MessageCreator) session -> {
                return session.createTextMessage("test message");
            });

            return null;
        });

        final String text =
                transactionTemplate.execute(status -> {
                    // Send message
                    jmsTemplate.setReceiveTimeout(5000);
                    final Message message = jmsTemplate.receive("QueueOne");
                    Assert.assertNotNull(message);

                    try {
                        return ((TextMessage) message).getText();
                    } catch (final JMSException e) {
                        Assert.fail(e.getMessage());
                        return null;
                    }
                });
        Assert.assertEquals("test message", text);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void jmsException() {
        transactionTemplate.execute(status -> {
            // Send message
            jmsTemplate.send("QueueOne", (MessageCreator) session -> {
                throw new UnsupportedOperationException("Fail");
            });

            return null;
        });
    }

    @Test
    public void jdbc() {
        transactionTemplate.execute(status -> {
            // Insert
            jdbcTemplate.execute("delete from test");
            jdbcTemplate.execute("insert into test(id, description) values(1, 'first')");

            return null;
        });
    }

    @Test(expected = BadSqlGrammarException.class)
    public void jdbcException() {
        transactionTemplate.execute(status -> {
            // Insert
            jdbcTemplate.execute("insert into totally_unknown_table(id, description) values(1, 'first')");

            return null;
        });
    }

    @Test
    public void jdbcAndJms() {
        transactionTemplate.execute(status -> {
            // Insert
            jdbcTemplate.execute("delete from test");
            jdbcTemplate.execute("insert into test(id, description) values(2, 'second')");

            // Send message
            jmsTemplate.send("QueueOne", (MessageCreator) session -> {
                return session.createTextMessage("test message");
            });
            return null;
        });
    }
}
