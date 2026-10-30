package rs.mds.task.order.service;

import org.springframework.stereotype.Service;
import rs.mds.task.events.OrderCreatedEvent;
import rs.mds.task.order.dto.CreateOrderRequest;
import rs.mds.task.order.dto.OrderResponse;
import rs.mds.task.order.messaging.OrderEventPublisher;

@Service
public class OrderService {

    private final OrderEventPublisher publisher;

    public OrderService(OrderEventPublisher publisher) {
        this.publisher = publisher;
    }

    public OrderResponse placeOrder(CreateOrderRequest request) {
        publisher.publish(OrderCreatedEvent.create(request.orderId(), request.itemId(), request.quantity()));
        return OrderResponse.accepted(request.orderId());
    }
}
