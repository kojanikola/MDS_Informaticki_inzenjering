package rs.mds.task.order.model;

import java.time.Instant;

/**
 * Last known state of an order, driven by the inventory results.
 *
 * @param reason set only when the order was rejected
 */
public record OrderState(String orderId, OrderStatus status, String reason, Instant updatedAt) {
}
