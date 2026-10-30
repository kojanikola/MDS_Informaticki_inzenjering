package rs.mds.task.events;

import java.time.Instant;
import java.util.UUID;

/**
 * Published by the inventory service once an order has been processed.
 *
 * @param causedByEventId the {@link OrderCreatedEvent#eventId()} this result answers
 * @param reason          set only when the order was rejected
 */
public record OrderResultEvent(
        String eventId,
        String orderId,
        OrderResultStatus status,
        String reason,
        String causedByEventId,
        Instant occurredAt) {

    public static OrderResultEvent create(String orderId, OrderResultStatus status, String reason,
                                          String causedByEventId) {
        return new OrderResultEvent(UUID.randomUUID().toString(), orderId, status, reason, causedByEventId,
                Instant.now());
    }
}
