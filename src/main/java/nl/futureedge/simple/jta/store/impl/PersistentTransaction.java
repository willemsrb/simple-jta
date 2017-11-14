package nl.futureedge.simple.jta.store.impl;

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
import nl.futureedge.simple.jta.store.JtaTransactionStoreException;

/**
 * Transaction store delegate to stably store information for one transaction.
 */
public interface PersistentTransaction {

    /**
     * Save the status for the (global) transaction.
     * @param status status
     * @throws JtaTransactionStoreException Thrown if the transaction store encounters an unexpected error condition
     */
    void save(TransactionStatus status) throws JtaTransactionStoreException;

    /**
     * Save the status for a branch of the transaction.
     * @param status status
     * @param branchId branch xid
     * @param resourceManager resource manager
     * @throws JtaTransactionStoreException Thrown if the transaction store encounters an unexpected error condition
     */
    void save(TransactionStatus status, long branchId, String resourceManager) throws JtaTransactionStoreException;

    /**
     * Save the status for a branch of the transaction.
     * @param status status
     * @param branchId branch xid
     * @param resourceManager resource manager
     * @param cause exception information
     * @throws JtaTransactionStoreException Thrown if the transaction store encounters an unexpected error condition
     */
    void save(TransactionStatus status, long branchId, String resourceManager, Exception cause) throws JtaTransactionStoreException;

    /**
     * Remove the information for this transaction.
     * @throws JtaTransactionStoreException Thrown if the transaction store encounters an unexpected error condition
     */
    void remove() throws JtaTransactionStoreException;

    /**
     * Retrieves the current (global) status for the transaction
     * @return status
     * @throws JtaTransactionStoreException Thrown if the transaction store encounters an unexpected error condition
     */
    TransactionStatus getStatus() throws JtaTransactionStoreException;

    /**
     * Close all resources connected to this persistent transaction.
     */
    void close();

}
