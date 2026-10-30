package rs.mds.task.inventory.messaging;

import org.slf4j.MDC;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import rs.mds.task.events.OrderCreatedEvent;
import rs.mds.task.events.OrderMessaging;
import rs.mds.task.events.OrderResultEvent;
import rs.mds.task.events.OrderResultStatus;
import rs.mds.task.inventory.model.Reservation;
import rs.mds.task.inventory.model.ReservationStatus;
import rs.mds.task.inventory.service.InventoryService;

/**
 * Processes the order, then publishes the outcome. The result is published for duplicates too, so a delivery
 * that failed after processing but before the result was confirmed is repaired by the redelivery.
 */
@Component
public class OrderCreatedListener {

    private final InventoryService inventoryService;
    private final OrderResultPublisher resultPublisher;

    public OrderCreatedListener(InventoryService inventoryService, OrderResultPublisher resultPublisher) {
        this.inventoryService = inventoryService;
        this.resultPublisher = resultPublisher;
    }

    @RabbitListener(queues = OrderMessaging.ORDER_CREATED_QUEUE)
    public void onOrderCreated(OrderCreatedEvent event,
                               @Header(name = AmqpHeaders.CORRELATION_ID, required = false) String correlationId) {
        MDC.put(OrderMessaging.CORRELATION_ID_KEY, correlationId != null ? correlationId : String.valueOf(
                event == null ? null : event.eventId()));
        try {
            Reservation reservation = inventoryService.process(event);
            resultPublisher.publish(OrderResultEvent.create(
                    reservation.orderId(),
                    reservation.status() == ReservationStatus.RESERVED
                            ? OrderResultStatus.RESERVED : OrderResultStatus.REJECTED,
                    reservation.reason(),
                    event.eventId()));
        } finally {
            MDC.remove(OrderMessaging.CORRELATION_ID_KEY);
        }
    }
}
