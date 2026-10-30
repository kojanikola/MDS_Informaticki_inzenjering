package rs.mds.task.order.messaging;

import rs.mds.task.events.OrderCreatedEvent;

/**
 * Outbound port towards the message broker. Keeps the service layer independent of RabbitMQ.
 */
public interface OrderEventPublisher {

    void publish(OrderCreatedEvent event);
}
