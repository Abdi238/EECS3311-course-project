package budgetbuddy.persistence;

/**
 * Wraps JDBC failures so repository interfaces stay free of SQLException.
 */
public class PersistenceException extends RuntimeException {
    public PersistenceException(String message) {
        super(message);
    }

    public PersistenceException(String message, Throwable cause) {
        super(message, cause);
    }
}
