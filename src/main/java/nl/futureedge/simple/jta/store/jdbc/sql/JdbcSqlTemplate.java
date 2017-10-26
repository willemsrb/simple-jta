package nl.futureedge.simple.jta.store.jdbc.sql;

public interface JdbcSqlTemplate {

    String createTransactionIdSequence();

    String selectNextTransactionId();


    String createTransactionTable();

    String selectTransactionIdAndStatus();

    String selectTransactionStatus();

    String insertTransactionStatus();

    String updateTransactionStatus();

    String deleteTransactionStatus();


    String createResourceTable();

    String selectResourceStatus();

    String insertResourceStatus();

    String updateResourceStatus();

    String deleteResourceStatus();

}
