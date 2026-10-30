package rs.mds.task.inventory.exception;

/**
 * The event can never be processed successfully (a poison message), so retrying it is pointless.
 */
public class InvalidOrderEventException extends RuntimeException {

    public InvalidOrderEventException(String message) {
        super(message);
    }
}
