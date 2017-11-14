package nl.futureedge.simple.jta;

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
import javax.transaction.InvalidTransactionException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Exceptions helper.
 */
public final class JtaExceptions {

    private static final Logger LOGGER = LoggerFactory.getLogger(JtaExceptions.class);

    private JtaExceptions() {
        throw new IllegalStateException("Class should not be instantiated");
    }

    /**
     * Create a system exception.
     * @param message message
     * @return system exception
     */
    public static SystemException systemException(final String message) {
        LOGGER.debug(message);
        return new SystemException(message);
    }

    /**
     * Create a system exception.
     * @param message message
     * @param cause cause
     * @return system exception
     */
    public static SystemException systemException(final String message, final Throwable cause) {
        LOGGER.debug(message, cause);
        final SystemException systemException = new SystemException(message);
        systemException.initCause(cause);
        return systemException;
    }

    /**
     * Create a not supported exception.
     * @param message message
     * @return not supported exception
     */
    public static NotSupportedException notSupportedException(final String message) {
        LOGGER.debug(message);
        return new NotSupportedException(message);
    }

    /**
     * Create a rollback exception.
     * @param message message
     * @return rollback exception
     */
    public static RollbackException rollbackException(final String message) {
        LOGGER.debug(message);
        return new RollbackException(message);
    }

    /**
     * Create an illegal state exception
     * @param message message
     * @return illegal state exception
     */
    public static IllegalStateException illegalStateException(final String message) {
        LOGGER.debug(message);
        return new IllegalStateException(message);
    }

    /**
     * Create an unsupported operation exception.
     * @param message message
     * @return unsupported operation exception
     */
    public static UnsupportedOperationException unsupportedOperationException(final String message) {
        LOGGER.debug(message);
        return new UnsupportedOperationException(message);
    }

    /**
     * Create an invalid transaction exception.
     * @param message message
     * @return invalid transaction exception
     */
    public static InvalidTransactionException invalidTransactionException(String message) {
        LOGGER.debug(message);
        return new InvalidTransactionException(message);
    }
}
