package rs.mds.task.inventory.messaging;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import rs.mds.task.events.OrderCreatedEvent;
import rs.mds.task.inventory.service.InventoryService;

class OrderCreatedListenerTest {

    @Test
    void delegatesEventToService() {
        InventoryService service = mock(InventoryService.class);
        OrderCreatedEvent event = OrderCreatedEvent.create("123", "item-1", 2);

        new OrderCreatedListener(service).onOrderCreated(event);

        verify(service).process(event);
    }
}
