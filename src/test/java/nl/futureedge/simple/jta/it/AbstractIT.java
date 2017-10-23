package nl.futureedge.simple.jta.it;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.Properties;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.GenericXmlApplicationContext;
import org.springframework.core.env.PropertiesPropertySource;


public abstract class AbstractIT {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractIT.class);

    private static Properties portProperties = new Properties();
    private static GenericXmlApplicationContext databaseContext;
    private static GenericXmlApplicationContext messagebrokerContext;

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

    @Before
    public void start() {
        // Create test context
        testContext = new GenericXmlApplicationContext();
        testContext.load("classpath:test-context.xml");
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
}
