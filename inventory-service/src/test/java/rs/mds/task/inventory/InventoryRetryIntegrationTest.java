package rs.mds.task.inventory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import rs.mds.task.events.OrderCreatedEvent;
import rs.mds.task.events.OrderMessaging;
import rs.mds.task.events.OrderResultEvent;
import rs.mds.task.inventory.exception.ResultPublishException;
import rs.mds.task.inventory.messaging.OrderResultPublisher;
import rs.mds.task.inventory.service.InventoryService;

/**
 * Transient failures (here: the result cannot be published) are retried, and dead-lettered once attempts run out.
 */
@SpringBootTest(properties = {
        "inventory.retry.max-attempts=3",
        "inventory.retry.initial-interval=50ms",
        "inventory.retry.max-interval=100ms"})
class InventoryRetryIntegrationTest extends RabbitIntegrationTest {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private RabbitAdmin rabbitAdmin;

    @Autowired
    private InventoryService inventoryService;

    @MockBean
    private OrderResultPublisher resultPublisher;

    @BeforeEach
    void purgeQueues() {
        rabbitAdmin.purgeQueue(OrderMessaging.ORDER_CREATED_DLQ, true);
    }

    private void send(OrderCreatedEvent event) {
        rabbitTemplate.convertAndSend(OrderMessaging.EXCHANGE, OrderMessaging.ORDER_CREATED_ROUTING_KEY, event);
    }

    @Test
    void persistentFailureIsRetriedThenDeadLettered() {
        doThrow(new ResultPublishException("broker unavailable")).when(resultPublisher).publish(any());

        send(OrderCreatedEvent.create("retry-fail", "item-1", 1));

        verify(resultPublisher, timeout(10_000).times(3)).publish(any(OrderResultEvent.class));
        assertThat(rabbitTemplate.receive(OrderMessaging.ORDER_CREATED_DLQ, 10_000)).isNotNull();
        // Redeliveries are idempotent: the order is processed once even though it was attempted three times.
        assertThat(inventoryService.findReservation("retry-fail")).isPresent();
    }

    @Test
    void recoversWhenFailureIsTransient() {
        doThrow(new ResultPublishException("blip"))
                .doThrow(new ResultPublishException("blip"))
                .doNothing()
                .when(resultPublisher).publish(any());

        send(OrderCreatedEvent.create("retry-recover", "item-1", 1));

        verify(resultPublisher, timeout(10_000).times(3)).publish(any(OrderResultEvent.class));
        assertThat(rabbitTemplate.receive(OrderMessaging.ORDER_CREATED_DLQ, 1_000)).isNull();
    }
}
