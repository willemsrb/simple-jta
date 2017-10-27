package nl.futureedge.simple.jta.store.jdbc.sql;

/**
 * SQL template.
 */
public interface JdbcSqlTemplate {

    /* *** TRANSACTION ID *** */

    /**
     * @return SQL to create transaction id sequence
     */
    String createTransactionIdSequence();

    /**
     * @return SQL to select next transaction id
     */
    String selectNextTransactionId();

    /* *** TRANSACTION STATUS *** */

    /**
     * @return SQL to create the transactions table (columns for id (java type long), status (java type string), timestamp created (java type date) and
     * timestamp last updated (java type date))
     */
    String createTransactionTable();

    /**
     * @return SQL to select id (result column index 1) and status (result column index 2) from all transactions
     */
    String selectTransactionIdAndStatus();

    /**
     * @return SQL to select status (result column index 1) from a specific transaction selected by id (statement column index 1)
     */
    String selectTransactionStatus();

    /**
     * @return SQL to insert id (statement column index 1), status (statement column index 2), timestamp created (statement column index 3) and timestamp last
     * updated (statement column index 4) for a transaction
     */
    String insertTransactionStatus();

    /**
     * @return SQL to update status (statement column index 1) and timestamp last updated (statement column index 2) for a transaction identified by id
     * (statement column index 3)
     */
    String updateTransactionStatus();

    /**
     * @return SQL to remove transaction information for a transaction identified by id (statement column index 1)
     */
    String deleteTransactionStatus();

    /* *** RESOURCE STATUS *** */

    /**
     * @return SQL to create the transactions resources table (columns for transaction id (java type long; references the id of the transaction), branch id
     * (java type long), resource name (java type string), status (java type string), failure cause (java type string), timestamp created (java type date) and
     * timestamp last updated (java type date))
     */
    String createResourceTable();

    /**
     * @return SQL to select status (result column index 1), for all resources for a transaction identified by id (statement column index 1)
     */
    String selectResourceStatus();

    /**
     * @return SQL to insert transaction id (statement column index 1), branch id (statement column index 2), resource name (statement column index 3), status
     * (statement column index 4), failure cause (statement column index 5), timestamp created (statement column index 6) and timestamp last updated (statement
     * column index 7) for a resource
     */
    String insertResourceStatus();

    /**
     * @return SQL to update status (statement column index 1), failure cause (statement column index 2) and timestamp last updated (statement column index 3)
     * for a resource identified by transaction id (statement column index 4), branch id (statement column index 5) and resource name (statement column index
     * 6)
     */
    String updateResourceStatus();

    /**
     * @return SQL to remove all resource information for a transaction identified by id (statement column index 1)
     */
    String deleteResourceStatus();

}
