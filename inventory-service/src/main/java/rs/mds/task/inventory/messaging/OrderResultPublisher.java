package rs.mds.task.inventory.messaging;

import rs.mds.task.events.OrderResultEvent;

/**
 * Outbound port towards the message broker.
 */
public interface OrderResultPublisher {

    void publish(OrderResultEvent event);
}
