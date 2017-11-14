package nl.futureedge.simple.jta.spring.hibernate;

import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.UserTransaction;
import org.hibernate.engine.transaction.jta.platform.spi.JtaPlatform;
import org.hibernate.engine.transaction.jta.platform.spi.JtaPlatformException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.jta.JtaTransactionManager;

/**
 * Bridge between Spring and Hibernate to let hibernate use the Spring JTA Transaction Manager as a JTA platform.
 */
public final class SpringJtaPlatform implements JtaPlatform {

    private JtaTransactionManager springJtaTransactionManager;

    /**
     * The Spring JTA transaction manager to use.
     * @param springJtaTransactionManager Spring JTA transaction manager
     */
    @Autowired(required = true)
    public void setSpringJtaTransactionManager(final JtaTransactionManager springJtaTransactionManager) {
        this.springJtaTransactionManager = springJtaTransactionManager;
    }

    @Override
    public TransactionManager retrieveTransactionManager() {
        return springJtaTransactionManager.getTransactionManager();
    }

    @Override
    public UserTransaction retrieveUserTransaction() {
        return springJtaTransactionManager.getUserTransaction();
    }

    @Override
    public Object getTransactionIdentifier(final Transaction transaction) {
        return transaction;
    }

    @Override
    public boolean canRegisterSynchronization() {
        try {
            return Status.STATUS_ACTIVE == getCurrentStatus();
        } catch (SystemException e) {
            throw new JtaPlatformException("Could not determine transaction status", e);
        }
    }

    @Override
    public void registerSynchronization(final Synchronization synchronization) {
        try {
            retrieveTransactionManager().getTransaction().registerSynchronization(synchronization);
        } catch (SystemException | RollbackException e) {
            throw new JtaPlatformException("Could not access JTA Transaction to register synchronization", e);
        }
    }

    @Override
    public int getCurrentStatus() throws SystemException {
        return retrieveTransactionManager().getStatus();
    }
}
