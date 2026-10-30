package rs.mds.task.inventory.messaging;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import rs.mds.task.events.OrderCreatedEvent;
import rs.mds.task.events.OrderMessaging;
import rs.mds.task.inventory.service.InventoryService;

@Component
public class OrderCreatedListener {

    private final InventoryService inventoryService;

    public OrderCreatedListener(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @RabbitListener(queues = OrderMessaging.ORDER_CREATED_QUEUE)
    public void onOrderCreated(OrderCreatedEvent event) {
        inventoryService.process(event);
    }
}
