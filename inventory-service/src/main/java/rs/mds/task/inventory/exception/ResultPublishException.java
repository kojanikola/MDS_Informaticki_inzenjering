package rs.mds.task.inventory.exception;

/**
 * The broker did not durably accept the result event. Thrown inside the listener so the delivery is retried.
 */
public class ResultPublishException extends RuntimeException {

    public ResultPublishException(String message) {
        super(message);
    }

    public ResultPublishException(String message, Throwable cause) {
        super(message, cause);
    }
}
