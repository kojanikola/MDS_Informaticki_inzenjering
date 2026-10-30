package rs.mds.task.order.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import rs.mds.task.events.OrderCreatedEvent;
import rs.mds.task.order.dto.CreateOrderRequest;
import rs.mds.task.order.dto.OrderResponse;
import rs.mds.task.order.messaging.OrderEventPublisher;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderEventPublisher publisher;

    @InjectMocks
    private OrderService orderService;

    @Test
    void placeOrderPublishesEventAndReturnsAccepted() {
        OrderResponse response = orderService.placeOrder(new CreateOrderRequest("123", "item-1", 2));

        verify(publisher).publish(new OrderCreatedEvent("123", "item-1", 2));
        assertThat(response).isEqualTo(new OrderResponse("123", "ACCEPTED"));
    }
}
