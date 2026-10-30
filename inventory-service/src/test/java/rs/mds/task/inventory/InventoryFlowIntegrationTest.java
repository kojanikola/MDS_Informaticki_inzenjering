package rs.mds.task.inventory;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import rs.mds.task.events.OrderCreatedEvent;
import rs.mds.task.events.OrderMessaging;
import rs.mds.task.events.OrderResultEvent;
import rs.mds.task.events.OrderResultStatus;
import rs.mds.task.inventory.service.InventoryService;

/**
 * Real broker, real listener: order event in, reservation and result event out.
 */
@SpringBootTest
class InventoryFlowIntegrationTest extends RabbitIntegrationTest {

    private static final long TIMEOUT_MS = 10_000;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private RabbitAdmin rabbitAdmin;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private InventoryService inventoryService;

    @BeforeEach
    void purgeQueues() {
        rabbitAdmin.purgeQueue(OrderMessaging.ORDER_RESULT_QUEUE, true);
        rabbitAdmin.purgeQueue(OrderMessaging.ORDER_CREATED_DLQ, true);
    }

    private void send(OrderCreatedEvent event, String correlationId) {
        rabbitTemplate.convertAndSend(OrderMessaging.EXCHANGE, OrderMessaging.ORDER_CREATED_ROUTING_KEY, event,
                message -> {
                    message.getMessageProperties().setCorrelationId(correlationId);
                    return message;
                });
    }

    private Message receive(String queue) {
        return rabbitTemplate.receive(queue, TIMEOUT_MS);
    }

    private OrderResultEvent toResult(Message message) throws Exception {
        return objectMapper.readValue(message.getBody(), OrderResultEvent.class);
    }

    @Test
    void reservedOrderPublishesResultWithCorrelationId() throws Exception {
        OrderCreatedEvent event = OrderCreatedEvent.create("it-reserve", "item-1", 1);

        send(event, "corr-reserve");

        Message message = receive(OrderMessaging.ORDER_RESULT_QUEUE);
        assertThat(message).isNotNull();
        OrderResultEvent result = toResult(message);
        assertThat(result.orderId()).isEqualTo("it-reserve");
        assertThat(result.status()).isEqualTo(OrderResultStatus.RESERVED);
        assertThat(result.causedByEventId()).isEqualTo(event.eventId());
        assertThat(message.getMessageProperties().getCorrelationId()).isEqualTo("corr-reserve");
        assertThat(message.getMessageProperties().getReceivedRoutingKey()).isEqualTo("order.result.reserved");
        assertThat(message.getMessageProperties().getReceivedDeliveryMode()).isEqualTo(MessageDeliveryMode.PERSISTENT);
    }

    @Test
    void insufficientStockPublishesRejectedResult() throws Exception {
        send(OrderCreatedEvent.create("it-reject", "item-3", 1), "corr-reject");

        Message message = receive(OrderMessaging.ORDER_RESULT_QUEUE);
        assertThat(message).isNotNull();
        OrderResultEvent result = toResult(message);
        assertThat(result.status()).isEqualTo(OrderResultStatus.REJECTED);
        assertThat(result.reason()).isEqualTo("INSUFFICIENT_STOCK");
        assertThat(message.getMessageProperties().getReceivedRoutingKey()).isEqualTo("order.result.rejected");
    }

    @Test
    void duplicateDeliveryReservesStockOnlyOnce() throws Exception {
        int before = inventoryService.currentStock().get("item-2");
        OrderCreatedEvent event = OrderCreatedEvent.create("it-duplicate", "item-2", 1);

        send(event, "corr-dup-1");
        send(event, "corr-dup-2");

        assertThat(toResult(receive(OrderMessaging.ORDER_RESULT_QUEUE)).status()).isEqualTo(OrderResultStatus.RESERVED);
        assertThat(toResult(receive(OrderMessaging.ORDER_RESULT_QUEUE)).status()).isEqualTo(OrderResultStatus.RESERVED);
        assertThat(inventoryService.currentStock().get("item-2")).isEqualTo(before - 1);
    }

    @Test
    void invalidEventGoesStraightToDeadLetterQueue() {
        send(OrderCreatedEvent.create("it-poison", "item-1", 0), "corr-poison");

        assertThat(receive(OrderMessaging.ORDER_CREATED_DLQ)).isNotNull();
        assertThat(rabbitTemplate.receive(OrderMessaging.ORDER_RESULT_QUEUE, 500)).isNull();
    }

    @Test
    void unparseablePayloadGoesToDeadLetterQueue() {
        Message garbage = new Message("not json".getBytes(), jsonProperties());

        rabbitTemplate.send(OrderMessaging.EXCHANGE, OrderMessaging.ORDER_CREATED_ROUTING_KEY, garbage);

        assertThat(receive(OrderMessaging.ORDER_CREATED_DLQ)).isNotNull();
    }

    private MessageProperties jsonProperties() {
        MessageProperties properties = new MessageProperties();
        properties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
        return properties;
    }
}
