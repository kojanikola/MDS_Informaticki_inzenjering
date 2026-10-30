package rs.mds.task.inventory.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.MDC;
import rs.mds.task.events.OrderCreatedEvent;
import rs.mds.task.events.OrderMessaging;
import rs.mds.task.events.OrderResultEvent;
import rs.mds.task.events.OrderResultStatus;
import rs.mds.task.inventory.exception.InvalidOrderEventException;
import rs.mds.task.inventory.exception.ResultPublishException;
import rs.mds.task.inventory.model.Reservation;
import rs.mds.task.inventory.model.ReservationStatus;
import rs.mds.task.inventory.service.InventoryService;

class OrderCreatedListenerTest {

    private final InventoryService service = mock(InventoryService.class);
    private final OrderResultPublisher publisher = mock(OrderResultPublisher.class);
    private final OrderCreatedListener listener = new OrderCreatedListener(service, publisher);
    private final OrderCreatedEvent event = OrderCreatedEvent.create("123", "item-1", 2);

    private ArgumentCaptor<OrderResultEvent> publishedResult() {
        ArgumentCaptor<OrderResultEvent> captor = ArgumentCaptor.forClass(OrderResultEvent.class);
        verify(publisher).publish(captor.capture());
        return captor;
    }

    @Test
    void publishesReservedResult() {
        when(service.process(event)).thenReturn(
                new Reservation("123", "item-1", 2, ReservationStatus.RESERVED, null, Instant.now()));

        listener.onOrderCreated(event, "corr-1");

        OrderResultEvent result = publishedResult().getValue();
        assertThat(result.orderId()).isEqualTo("123");
        assertThat(result.status()).isEqualTo(OrderResultStatus.RESERVED);
        assertThat(result.reason()).isNull();
        assertThat(result.causedByEventId()).isEqualTo(event.eventId());
    }

    @Test
    void publishesRejectedResultWithReason() {
        when(service.process(event)).thenReturn(new Reservation(
                "123", "item-1", 2, ReservationStatus.REJECTED, "INSUFFICIENT_STOCK", Instant.now()));

        listener.onOrderCreated(event, "corr-1");

        OrderResultEvent result = publishedResult().getValue();
        assertThat(result.status()).isEqualTo(OrderResultStatus.REJECTED);
        assertThat(result.reason()).isEqualTo("INSUFFICIENT_STOCK");
    }

    @Test
    void invalidEventPublishesNothing() {
        when(service.process(event)).thenThrow(new InvalidOrderEventException("bad"));

        assertThatThrownBy(() -> listener.onOrderCreated(event, null))
                .isInstanceOf(InvalidOrderEventException.class);
        verifyNoInteractions(publisher);
    }

    @Test
    void publishFailureIsPropagatedSoTheDeliveryIsRetried() {
        when(service.process(event)).thenReturn(
                new Reservation("123", "item-1", 2, ReservationStatus.RESERVED, null, Instant.now()));
        doThrow(new ResultPublishException("broker down")).when(publisher).publish(
                org.mockito.ArgumentMatchers.any(OrderResultEvent.class));

        assertThatThrownBy(() -> listener.onOrderCreated(event, "corr-1"))
                .isInstanceOf(ResultPublishException.class);
    }

    @Test
    void correlationIdIsOnlyInMdcWhileProcessing() {
        when(service.process(event)).thenAnswer(invocation -> {
            assertThat(MDC.get(OrderMessaging.CORRELATION_ID_KEY)).isEqualTo("corr-1");
            return new Reservation("123", "item-1", 2, ReservationStatus.RESERVED, null, Instant.now());
        });

        listener.onOrderCreated(event, "corr-1");

        assertThat(MDC.get(OrderMessaging.CORRELATION_ID_KEY)).isNull();
    }
}
