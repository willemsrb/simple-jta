package nl.futureedge.simple.jta.it;

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

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.support.GenericXmlApplicationContext;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

@RunWith(Parameterized.class)
public class SimpleJtaSuspendIT {

    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleJtaSuspendIT.class);

    @Autowired
    private PlatformTransactionManager springTransactionManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final String testContextConfiguration;
    private GenericXmlApplicationContext testContext;

    @Parameterized.Parameters(name = "{0}")
    public static Object[] data() {
        return new Object[]{
                "classpath:test-suspend-hsqldb-context.xml",
                "classpath:test-suspend-mariadb-context.xml",
        };
    }

    public SimpleJtaSuspendIT(final String testContextConfiguration) {
        this.testContextConfiguration = testContextConfiguration;
    }

    @Before
    public void start() {
        // Create test context
        testContext = new GenericXmlApplicationContext();
        testContext.load(testContextConfiguration);
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


    private void testJdbcTemplate(boolean rollbackOuter, boolean rollbackInner) {
        jdbcTemplate.execute("delete from test");

        final TransactionTemplate outerTransaction =
                new TransactionTemplate(springTransactionManager, new DefaultTransactionDefinition(TransactionDefinition.PROPAGATION_REQUIRED));
        LOGGER.info("\n*** BEFORE OUTER ***\n*** BEFORE OUTER ***\n*** BEFORE OUTER ***");
        outerTransaction.execute(outerTransactionStatus -> {
            LOGGER.info("\n*** BEGIN OUTER ***\n*** BEGIN OUTER ***\n*** BEGIN OUTER ***");
            jdbcTemplate.execute("insert into test(id, description) values(1, 'first')");

            LOGGER.info("\n*** BEFORE INNER ***\n*** BEFORE INNER ***\n*** BEFORE INNER ***");
            final TransactionTemplate innerTransaction =
                    new TransactionTemplate(springTransactionManager, new DefaultTransactionDefinition(TransactionDefinition.PROPAGATION_REQUIRES_NEW));
            innerTransaction.execute(innerTransactionStatus -> {
                LOGGER.info("\n*** BEGIN INNER ***\n*** BEGIN INNER ***\n*** BEGIN INNER ***");
                jdbcTemplate.execute("insert into test(id, description) values(2, 'second')");
                if (rollbackInner) {
                    innerTransactionStatus.setRollbackOnly();
                }
                LOGGER.info("\n*** END INNER ***\n*** END INNER ***\n*** END INNER ***");
                return null;
            });
            LOGGER.info("\n*** AFTER INNER ***\n*** AFTER INNER ***\n*** AFTER INNER ***");
            if (rollbackOuter) {
                outerTransactionStatus.setRollbackOnly();
            }

            LOGGER.info("\n*** END OUTER ***\n*** END OUTER ***\n*** END OUTER ***");
            return null;
        });
        LOGGER.info("\n*** AFTER OUTER ***\n*** AFTER OUTER ***\n*** AFTER OUTER ***");

    }

    @Test
    public void outerOkInnerOk() {
        testJdbcTemplate(false, false);

        Assert.assertEquals("first", queryForObjectOrNull("select description from test where id = ?", String.class, 1));
        Assert.assertEquals("second", queryForObjectOrNull("select description from test where id = ?", String.class, 2));
    }


    @Test
    public void outerOkInnerNok() {
        testJdbcTemplate(false, true);

        Assert.assertEquals("first", queryForObjectOrNull("select description from test where id = ?", String.class, 1));
        Assert.assertEquals(null, queryForObjectOrNull("select description from test where id = ?", String.class, 2));
    }


    @Test
    public void outerNokInnerOk() {
        testJdbcTemplate(true, false);

        Assert.assertEquals(null, queryForObjectOrNull("select description from test where id = ?", String.class, 1));
        Assert.assertEquals("second", queryForObjectOrNull("select description from test where id = ?", String.class, 2));
    }


    @Test
    public void outerNokInnerNok() {
        testJdbcTemplate(true, true);

        Assert.assertEquals(null, queryForObjectOrNull("select description from test where id = ?", String.class, 1));
        Assert.assertEquals(null, queryForObjectOrNull("select description from test where id = ?", String.class, 2));
    }


    <T> T queryForObjectOrNull(String sql, Class<T> requiredType, Object... arguments) {
        try {
            return jdbcTemplate.queryForObject(sql, requiredType, arguments);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }
}
