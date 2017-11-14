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
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import nl.futureedge.simple.jta.store.JtaTransactionStoreException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JdbcConnectionPoolTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(JdbcConnectionPoolTest.class);

    private Driver testDriver;
    private List<Connection> testConnections;

    private JdbcConnectionPool subject;

    @Before
    public void setup() throws Exception {
        testDriver = Mockito.mock(Driver.class);
        testConnections = new ArrayList<>();
        Mockito.when(testDriver.acceptsURL(Mockito.anyString())).thenAnswer(invocation -> ((String) invocation.getArgument(0)).startsWith("jdbc:test:"));
        Mockito.when(testDriver.connect(Mockito.eq("jdbc:test:bla"), Mockito.any())).thenAnswer(invocation -> {
            LOGGER.info("Returning new MOCK connection");
            final Connection result = Mockito.mock(Connection.class);
            testConnections.add(result);
            return result;
        });

        DriverManager.registerDriver(testDriver);
        subject = new JdbcConnectionPool(null, "jdbc:test:bla", "theUsername", "thePassword");

        Mockito.verify(testDriver).connect(Mockito.eq("jdbc:test:bla"), Mockito.any());
        Assert.assertEquals(1, testConnections.size());
        Mockito.verifyNoMoreInteractions(testDriver);
    }

    @After
    public void destroy() throws Exception {
        Mockito.verifyNoMoreInteractions(testDriver);

        subject.close();
        DriverManager.deregisterDriver(testDriver);

        for (Connection testConnection : testConnections) {
            Mockito.verify(testConnection).close();
        }

        Mockito.verifyNoMoreInteractions(testDriver);

    }

    @Test
    public void test() throws Exception {
        Assert.assertEquals(1, testConnections.size());
        final Connection connection1 = subject.borrowConnection();
        Assert.assertNotNull(connection1);
        Assert.assertEquals(1, testConnections.size());
        Mockito.verifyNoMoreInteractions(testDriver);

        final Connection connection2 = subject.borrowConnection();
        Assert.assertNotNull(connection2);
        Assert.assertEquals(2, testConnections.size());
        Mockito.verify(testDriver, Mockito.times(2)).connect(Mockito.eq("jdbc:test:bla"), Mockito.any());
        Mockito.verifyNoMoreInteractions(testDriver);

        final Connection connection3 = subject.borrowConnection();
        Assert.assertNotNull(connection3);
        Assert.assertEquals(3, testConnections.size());
        Mockito.verify(testDriver, Mockito.times(3)).connect(Mockito.eq("jdbc:test:bla"), Mockito.any());
        Mockito.verifyNoMoreInteractions(testDriver);

        subject.returnConnection(connection2);
        final Connection connection4 = subject.borrowConnection();
        Assert.assertNotNull(connection4);
        Assert.assertEquals(3, testConnections.size());
        Assert.assertSame(connection2, connection4);
        Mockito.verifyNoMoreInteractions(testDriver);
    }

    @Test
    public void testDriverClass() throws Exception {
        final JdbcConnectionPool emptyDriver = new JdbcConnectionPool("", "jdbc:test:bla", "user", "pass");
        Mockito.verify(testDriver, Mockito.times(2)).connect(Mockito.eq("jdbc:test:bla"), Mockito.any());
        Mockito.verifyNoMoreInteractions(testDriver);
        emptyDriver.close();

        final JdbcConnectionPool knownDriver = new JdbcConnectionPool("org.hsqldb.jdbc.JDBCDriver", "jdbc:test:bla", "user", "pass");
        Mockito.verify(testDriver, Mockito.times(3)).connect(Mockito.eq("jdbc:test:bla"), Mockito.any());
        Mockito.verifyNoMoreInteractions(testDriver);
        knownDriver.close();

        try {
            new JdbcConnectionPool("non.existent.JDBCDriver", "jdbc:test:bla", "user", "pass");
            Assert.fail("JtaTransactionStoreException expected");
        } catch (JtaTransactionStoreException e) {
            // Expected
        }
    }

    @Test
    public void createConnectionFail() throws Exception {
        Mockito.when(testDriver.connect(Mockito.eq("jdbc:test:bla2"), Mockito.any())).thenThrow(new SQLException("Test"));

        try {
            new JdbcConnectionPool(null, "jdbc:test:bla2", null, null);
            Assert.fail("JtaTransactionStoreException expected");
        } catch (JtaTransactionStoreException e) {
            // Expected
        }

        Mockito.verify(testDriver).connect(Mockito.eq("jdbc:test:bla2"), Mockito.any());
    }

    @Test
    public void testUsernamePassword() throws Exception {
        subject.borrowConnection();
        subject.borrowConnection();

        final ArgumentCaptor<Properties> infoCaptor = ArgumentCaptor.forClass(Properties.class);
        Mockito.verify(testDriver, Mockito.times(2)).connect(Mockito.eq("jdbc:test:bla"), infoCaptor.capture());
        Properties info = infoCaptor.getValue();
        Assert.assertEquals("theUsername", info.getProperty("user"));
        Assert.assertEquals("thePassword", info.getProperty("password"));
    }

    @Test
    public void testWithoutCredentials() throws Exception {
        Mockito.when(testDriver.connect(Mockito.eq("jdbc:test:bla2"), Mockito.any())).thenAnswer(invocation -> {
            final Connection result = Mockito.mock(Connection.class);
            return result;
        });

        new JdbcConnectionPool(null, "jdbc:test:bla2", null, null);

        final ArgumentCaptor<Properties> infoCaptor = ArgumentCaptor.forClass(Properties.class);
        Mockito.verify(testDriver).connect(Mockito.eq("jdbc:test:bla2"), infoCaptor.capture());
        Properties info = infoCaptor.getValue();
        Assert.assertEquals(null, info.getProperty("user"));
        Assert.assertEquals(null, info.getProperty("password"));
    }

    @Test
    public void closeFailed() throws Exception {
        Mockito.doThrow(new SQLException("Test")).when(testConnections.get(0)).close();
    }
}
