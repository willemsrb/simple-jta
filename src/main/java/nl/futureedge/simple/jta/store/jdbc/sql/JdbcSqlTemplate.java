package nl.futureedge.simple.jta.store.jdbc.sql;

public interface JdbcSqlTemplate {

    String createTransactionIdSequence();

    String selectNextTransactionId();


    String createTransactionTable();

    String updateTransactionStatus();

    String insertTransactionStatus();

    String selectTransactionStatus();

    String deleteTransactionStatus();


    String createResourceTable();

    String updateResourceStatus();

    String insertResourceStatus();

    String deleteResourceStatus();
}
