package rs.mds.task.inventory.model;

import java.time.Instant;

/**
 * Outcome of processing one order.
 *
 * @param reason set only when the order was rejected
 */
public record Reservation(
        String orderId,
        String itemId,
        int quantity,
        ReservationStatus status,
        String reason,
        Instant processedAt) {
}
