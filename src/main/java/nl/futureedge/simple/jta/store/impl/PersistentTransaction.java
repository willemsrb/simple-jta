package nl.futureedge.simple.jta.store.impl;

import nl.futureedge.simple.jta.store.JtaTransactionStoreException;

public interface PersistentTransaction {

    void save(TransactionStatus status) throws JtaTransactionStoreException;

    void save(TransactionStatus status, String resourceManager) throws JtaTransactionStoreException;

    void save(TransactionStatus status, String resourceManager, Exception cause) throws JtaTransactionStoreException;

    void remove() throws JtaTransactionStoreException;

    TransactionStatus getStatus() throws JtaTransactionStoreException;
}
