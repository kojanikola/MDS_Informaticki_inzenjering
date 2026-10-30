package rs.mds.task.order.service;

import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import rs.mds.task.events.OrderCreatedEvent;
import rs.mds.task.events.OrderResultEvent;
import rs.mds.task.events.OrderResultStatus;
import rs.mds.task.order.dto.CreateOrderRequest;
import rs.mds.task.order.dto.OrderResponse;
import rs.mds.task.order.messaging.OrderEventPublisher;
import rs.mds.task.order.model.OrderState;
import rs.mds.task.order.model.OrderStatus;
import rs.mds.task.order.repository.OrderStateRepository;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderEventPublisher publisher;
    private final OrderStateRepository stateRepository;

    public OrderService(OrderEventPublisher publisher, OrderStateRepository stateRepository) {
        this.publisher = publisher;
        this.stateRepository = stateRepository;
    }

    public OrderResponse placeOrder(CreateOrderRequest request) {
        publisher.publish(OrderCreatedEvent.create(request.orderId(), request.itemId(), request.quantity()));
        stateRepository.markAcceptedIfAbsent(request.orderId());
        return OrderResponse.accepted(request.orderId());
    }

    public void applyResult(OrderResultEvent event) {
        OrderStatus status = event.status() == OrderResultStatus.RESERVED ? OrderStatus.RESERVED : OrderStatus.REJECTED;
        stateRepository.save(new OrderState(event.orderId(), status, event.reason(), event.occurredAt()));
        log.info("Order {} is now {}{}", event.orderId(), status, event.reason() == null ? "" : " (" + event.reason() + ")");
    }

    public Optional<OrderState> findState(String orderId) {
        return stateRepository.findByOrderId(orderId);
    }
}
