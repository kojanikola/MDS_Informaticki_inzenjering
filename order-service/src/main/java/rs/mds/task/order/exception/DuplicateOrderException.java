package rs.mds.task.order.exception;

/**
 * An order with this id already exists. The order id is the business key, so it can be used only once.
 */
public class DuplicateOrderException extends RuntimeException {

    public DuplicateOrderException(String orderId) {
        super("Order " + orderId + " already exists");
    }
}
