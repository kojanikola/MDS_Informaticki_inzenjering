package rs.mds.task.order.model;

public enum OrderStatus {
    /** Handed to the broker, inventory has not answered yet. */
    ACCEPTED,
    RESERVED,
    REJECTED
}
