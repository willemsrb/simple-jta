package nl.futureedge.simple.jta.jms;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.XAConnection;
import javax.jms.XAConnectionFactory;
import nl.futureedge.simple.jta.JtaTransactionManager;
import nl.futureedge.simple.jta.xa.XAResourceAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Required;

/**
 * XAConnectionFactory adapter; creates connections on the wrapped xa connection factory and adapts them using a {@link XAConnectionAdapter}.
 */
public final class XAConnectionFactoryAdapter implements ConnectionFactory, InitializingBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(XAConnectionFactoryAdapter.class);

    private String uniqueName;
    private XAConnectionFactory xaConnectionFactory;
    private JtaTransactionManager jtaTransactionManager;
    private boolean supportsJoin = false;

    /**
     * Set unique name to use for this xa resource (manager).
     *
     * This must be unique with the set of xa resources that are used with a transaction manager . This must be consistent between session for recovery (commit
     * or rollback after crash) to work.
     * @param uniqueName unique name
     */
    @Required
    public void setUniqueName(final String uniqueName) {
        this.uniqueName = uniqueName;
    }

    /**
     * Set the xa connection factory to wrap.
     * @param xaConnectionFactory xa connection factory
     */
    @Required
    public void setXaConnectionFactory(final XAConnectionFactory xaConnectionFactory) {
        this.xaConnectionFactory = xaConnectionFactory;
    }

    /**
     * Set the jta monitor to use.
     * @param jtaTransactionManager jta monitor
     */
    @Required
    @Autowired
    public void setJtaTransactionManager(final JtaTransactionManager jtaTransactionManager) {
        this.jtaTransactionManager = jtaTransactionManager;
    }

    public void setSupportsJoin(boolean supportsJoin) {
        this.supportsJoin = supportsJoin;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        // Execute recovery
        jtaTransactionManager.recover(new XAResourceAdapter(uniqueName, false, xaConnectionFactory.createXAConnection().createXASession().getXAResource()));
    }

    /* ******************************************************** */
    /* ******************************************************** */
    /* ******************************************************** */

    @Override
    public Connection createConnection() throws JMSException {
        LOGGER.trace("getConnection()");
        final XAConnection xaConnection = xaConnectionFactory.createXAConnection();
        return new XAConnectionAdapter(uniqueName, supportsJoin, xaConnection, jtaTransactionManager);
    }

    @Override
    public Connection createConnection(final String username, final String password) throws JMSException {
        LOGGER.trace("getConnection(username={}, password not logged)", username, password);
        final XAConnection xaConnection = xaConnectionFactory.createXAConnection(username, password);
        return new XAConnectionAdapter(uniqueName, supportsJoin, xaConnection, jtaTransactionManager);
    }
}
