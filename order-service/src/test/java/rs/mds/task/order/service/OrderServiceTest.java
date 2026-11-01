package rs.mds.task.order.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import rs.mds.task.events.OrderCreatedEvent;
import rs.mds.task.events.OrderResultEvent;
import rs.mds.task.events.OrderResultStatus;
import rs.mds.task.order.dto.CreateOrderRequest;
import rs.mds.task.order.dto.OrderResponse;
import rs.mds.task.order.exception.DuplicateOrderException;
import rs.mds.task.order.exception.OrderPublishException;
import rs.mds.task.order.messaging.OrderEventPublisher;
import rs.mds.task.order.model.OrderState;
import rs.mds.task.order.model.OrderStatus;
import rs.mds.task.order.repository.OrderStateRepository;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderEventPublisher publisher;

    @Mock
    private OrderStateRepository stateRepository;

    @InjectMocks
    private OrderService orderService;

    @Test
    void placeOrderClaimsIdThenPublishesEvent() {
        when(stateRepository.registerAcceptedIfAbsent("123")).thenReturn(true);

        OrderResponse response = orderService.placeOrder(new CreateOrderRequest("123", "item-1", 2));

        ArgumentCaptor<OrderCreatedEvent> captor = ArgumentCaptor.forClass(OrderCreatedEvent.class);
        InOrder inOrder = inOrder(stateRepository, publisher);
        inOrder.verify(stateRepository).registerAcceptedIfAbsent("123");
        inOrder.verify(publisher).publish(captor.capture());
        OrderCreatedEvent event = captor.getValue();
        assertThat(event.orderId()).isEqualTo("123");
        assertThat(event.itemId()).isEqualTo("item-1");
        assertThat(event.quantity()).isEqualTo(2);
        assertThat(event.eventId()).isNotBlank();
        assertThat(event.occurredAt()).isNotNull();
        assertThat(response).isEqualTo(new OrderResponse("123", "ACCEPTED"));
    }

    @Test
    void repeatedOrderIdIsRejectedWithoutPublishing() {
        when(stateRepository.registerAcceptedIfAbsent("123")).thenReturn(false);

        assertThatThrownBy(() -> orderService.placeOrder(new CreateOrderRequest("123", "item-1", 2)))
                .isInstanceOf(DuplicateOrderException.class)
                .hasMessageContaining("123");

        verifyNoInteractions(publisher);
        verifyNoMoreInteractions(stateRepository);
    }

    @Test
    void failedPublishReleasesTheOrderIdSoTheClientCanRetry() {
        when(stateRepository.registerAcceptedIfAbsent("123")).thenReturn(true);
        doThrow(new OrderPublishException("broker down")).when(publisher).publish(any(OrderCreatedEvent.class));

        assertThatThrownBy(() -> orderService.placeOrder(new CreateOrderRequest("123", "item-1", 2)))
                .isInstanceOf(OrderPublishException.class);

        verify(stateRepository).removeIfAccepted("123");
    }

    @Test
    void appliesReservedResult() {
        Instant now = Instant.now();
        orderService.applyResult(new OrderResultEvent("e-1", "123", OrderResultStatus.RESERVED, null, "c-1", now));

        verify(stateRepository).save(new OrderState("123", OrderStatus.RESERVED, null, now));
    }

    @Test
    void appliesRejectedResultWithReason() {
        Instant now = Instant.now();
        orderService.applyResult(
                new OrderResultEvent("e-2", "124", OrderResultStatus.REJECTED, "INSUFFICIENT_STOCK", "c-2", now));

        verify(stateRepository).save(new OrderState("124", OrderStatus.REJECTED, "INSUFFICIENT_STOCK", now));
    }
}
