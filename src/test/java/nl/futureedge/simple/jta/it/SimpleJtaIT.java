package nl.futureedge.simple.jta.it;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.TextMessage;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;
import org.springframework.transaction.support.TransactionTemplate;


public class SimpleJtaIT extends AbstractIT {

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private JmsTemplate jmsTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

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
        System.out.println("Received message: " + text);
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
            jdbcTemplate.execute("insert into test(id, description) values(2, 'second')");

            // Send message
            jmsTemplate.send("QueueOne", (MessageCreator) session -> {
                return session.createTextMessage("test message");
            });
            return null;
        });
    }
}
