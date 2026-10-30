package rs.mds.task.order.exception;

/**
 * The broker did not durably accept the order event, so the order must not be reported as accepted.
 */
public class OrderPublishException extends RuntimeException {

    public OrderPublishException(String message) {
        super(message);
    }

    public OrderPublishException(String message, Throwable cause) {
        super(message, cause);
    }
}
