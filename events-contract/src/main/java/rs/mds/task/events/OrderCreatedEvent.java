package rs.mds.task.events;

/**
 * Published by the order service when a new order is accepted.
 * Final shape (event id, timestamp, versioning) is decided in the messaging step.
 */
public record OrderCreatedEvent(String orderId, String itemId, int quantity) {
}
