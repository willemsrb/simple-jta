package nl.futureedge.simple.jta.spring.hibernate;

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
