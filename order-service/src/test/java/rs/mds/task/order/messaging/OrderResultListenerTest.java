package rs.mds.task.order.messaging;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import rs.mds.task.events.OrderResultEvent;
import rs.mds.task.events.OrderResultStatus;
import rs.mds.task.order.service.OrderService;

class OrderResultListenerTest {

    @Test
    void delegatesResultToService() {
        OrderService service = mock(OrderService.class);
        OrderResultEvent event = new OrderResultEvent(
                "e-1", "123", OrderResultStatus.RESERVED, null, "c-1", Instant.now());

        new OrderResultListener(service).onOrderResult(event, "corr-1");

        verify(service).applyResult(event);
    }
}
