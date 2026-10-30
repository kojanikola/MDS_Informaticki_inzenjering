package rs.mds.task.order.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import rs.mds.task.events.OrderCreatedEvent;
import rs.mds.task.events.OrderMessaging;
import rs.mds.task.order.exception.OrderPublishException;

class RabbitOrderEventPublisherTest {

    private final RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
    private final RabbitOrderEventPublisher publisher =
            new RabbitOrderEventPublisher(rabbitTemplate, Duration.ofMillis(200));
    private final OrderCreatedEvent event = OrderCreatedEvent.create("123", "item-1", 2);

    /** Simulates the broker answering the publish with the given confirm. */
    private void brokerAnswers(boolean ack, String reason) {
        doAnswer(invocation -> {
            CorrelationData correlation = invocation.getArgument(4);
            correlation.getFuture().complete(new CorrelationData.Confirm(ack, reason));
            return null;
        }).when(rabbitTemplate).convertAndSend(
                eq(OrderMessaging.EXCHANGE), eq(OrderMessaging.ORDER_CREATED_ROUTING_KEY),
                eq((Object) event), any(MessagePostProcessor.class), any(CorrelationData.class));
    }

    @Test
    void returnsWhenBrokerAcks() {
        brokerAnswers(true, null);

        publisher.publish(event);
    }

    @Test
    void failsWhenBrokerNacks() {
        brokerAnswers(false, "disk full");

        assertThatThrownBy(() -> publisher.publish(event))
                .isInstanceOf(OrderPublishException.class)
                .hasMessageContaining("nacked");
    }

    @Test
    void failsWhenMessageIsReturnedAsUnroutable() {
        doAnswer(invocation -> {
            CorrelationData correlation = invocation.getArgument(4);
            correlation.setReturned(new org.springframework.amqp.core.ReturnedMessage(
                    new Message(new byte[0], new MessageProperties()), 312, "NO_ROUTE",
                    OrderMessaging.EXCHANGE, OrderMessaging.ORDER_CREATED_ROUTING_KEY));
            correlation.getFuture().complete(new CorrelationData.Confirm(true, null));
            return null;
        }).when(rabbitTemplate).convertAndSend(
                eq(OrderMessaging.EXCHANGE), eq(OrderMessaging.ORDER_CREATED_ROUTING_KEY),
                eq((Object) event), any(MessagePostProcessor.class), any(CorrelationData.class));

        assertThatThrownBy(() -> publisher.publish(event))
                .isInstanceOf(OrderPublishException.class)
                .hasMessageContaining("unroutable");
    }

    @Test
    void failsWhenNoConfirmArrivesInTime() {
        assertThatThrownBy(() -> publisher.publish(event))
                .isInstanceOf(OrderPublishException.class)
                .hasMessageContaining("No broker confirm");
    }

    @Test
    void failsWhenBrokerIsUnreachable() {
        doThrow(new AmqpConnectException(new java.net.ConnectException("refused")))
                .when(rabbitTemplate).convertAndSend(
                        any(String.class), any(String.class), any(Object.class),
                        any(MessagePostProcessor.class), any(CorrelationData.class));

        assertThatThrownBy(() -> publisher.publish(event))
                .isInstanceOf(OrderPublishException.class)
                .hasCauseInstanceOf(AmqpConnectException.class);
        assertThat(event.eventId()).isNotBlank();
    }
}
