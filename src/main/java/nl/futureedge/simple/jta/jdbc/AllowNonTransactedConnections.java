package nl.futureedge.simple.jta.jdbc;

/**
 * Allow non-transacted connections.
 */
public enum AllowNonTransactedConnections {

    /**
     * Yes, allow non-transacted connections.
     */
    YES,

    /**
     * No, do not allow non-transacted connections.
     */
    NO,

    /**
     * Yes, allow non-transacted connections, but log a warning when used.
     */
    WARN;
}
