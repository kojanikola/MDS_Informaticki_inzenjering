package rs.mds.task.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import rs.mds.task.events.OrderCreatedEvent;
import rs.mds.task.events.OrderMessaging;
import rs.mds.task.events.OrderResultEvent;
import rs.mds.task.events.OrderResultStatus;

/**
 * Real broker: the REST call publishes a persistent, correlated event, and inventory results update the status.
 */
@SpringBootTest
@AutoConfigureMockMvc
class OrderFlowIntegrationTest extends RabbitIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private RabbitAdmin rabbitAdmin;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void purgeQueues() {
        rabbitAdmin.purgeQueue(OrderMessaging.ORDER_CREATED_QUEUE, true);
    }

    private void postOrder(String orderId, String correlationId) throws Exception {
        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Correlation-Id", correlationId)
                        .content("""
                                {"orderId": "%s", "itemId": "item-1", "quantity": 2}
                                """.formatted(orderId)))
                .andExpect(status().isAccepted())
                .andExpect(header().string("X-Correlation-Id", correlationId));
    }

    @Test
    void postOrderPublishesPersistentCorrelatedEvent() throws Exception {
        postOrder("it-publish", "corr-publish");

        Message message = rabbitTemplate.receive(OrderMessaging.ORDER_CREATED_QUEUE, 10_000);
        assertThat(message).isNotNull();
        OrderCreatedEvent event = objectMapper.readValue(message.getBody(), OrderCreatedEvent.class);
        assertThat(event.orderId()).isEqualTo("it-publish");
        assertThat(event.itemId()).isEqualTo("item-1");
        assertThat(event.quantity()).isEqualTo(2);
        assertThat(message.getMessageProperties().getMessageId()).isEqualTo(event.eventId());
        assertThat(message.getMessageProperties().getCorrelationId()).isEqualTo("corr-publish");
        assertThat(message.getMessageProperties().getReceivedDeliveryMode()).isEqualTo(MessageDeliveryMode.PERSISTENT);
    }

    @Test
    void inventoryResultUpdatesOrderStatus() throws Exception {
        postOrder("it-status", "corr-status");
        mockMvc.perform(get("/orders/it-status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"));

        OrderResultEvent result = OrderResultEvent.create(
                "it-status", OrderResultStatus.REJECTED, "INSUFFICIENT_STOCK", "some-event");
        rabbitTemplate.convertAndSend(OrderMessaging.EXCHANGE, OrderMessaging.resultRoutingKey(result.status()), result);

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() ->
                mockMvc.perform(get("/orders/it-status"))
                        .andExpect(jsonPath("$.status").value("REJECTED"))
                        .andExpect(jsonPath("$.reason").value("INSUFFICIENT_STOCK")));
    }

    @Test
    void repeatedOrderIdIsRejectedAndPublishesOnlyOneEvent() throws Exception {
        postOrder("it-duplicate", "corr-dup-1");

        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"orderId": "it-duplicate", "itemId": "item-1", "quantity": 5}
                                """))
                .andExpect(status().isConflict());

        assertThat(rabbitTemplate.receive(OrderMessaging.ORDER_CREATED_QUEUE, 10_000)).isNotNull();
        assertThat(rabbitTemplate.receive(OrderMessaging.ORDER_CREATED_QUEUE, 500)).isNull();
    }

    @Test
    void unknownOrderIs404() throws Exception {
        mockMvc.perform(get("/orders/never-seen")).andExpect(status().isNotFound());
    }
}
