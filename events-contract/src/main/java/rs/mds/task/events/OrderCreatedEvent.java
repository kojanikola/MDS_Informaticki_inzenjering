package rs.mds.task.events;

import java.time.Instant;
import java.util.UUID;

/**
 * Published by the order service when a new order is accepted.
 *
 * @param eventId    unique per publish, used for tracing and as the AMQP message id
 * @param orderId    business identifier of the order, the inventory service deduplicates on it
 * @param occurredAt when the order was accepted
 */
public record OrderCreatedEvent(
        String eventId,
        String orderId,
        String itemId,
        int quantity,
        Instant occurredAt) {

    public static OrderCreatedEvent create(String orderId, String itemId, int quantity) {
        return new OrderCreatedEvent(UUID.randomUUID().toString(), orderId, itemId, quantity, Instant.now());
    }
}
