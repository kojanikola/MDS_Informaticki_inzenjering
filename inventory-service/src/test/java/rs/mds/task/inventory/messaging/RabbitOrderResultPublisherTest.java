package rs.mds.task.inventory.messaging;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import rs.mds.task.events.OrderMessaging;
import rs.mds.task.events.OrderResultEvent;
import rs.mds.task.events.OrderResultStatus;
import rs.mds.task.inventory.exception.ResultPublishException;

class RabbitOrderResultPublisherTest {

    private final RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
    private final RabbitOrderResultPublisher publisher =
            new RabbitOrderResultPublisher(rabbitTemplate, Duration.ofMillis(200));
    private final OrderResultEvent event =
            OrderResultEvent.create("123", OrderResultStatus.REJECTED, "INSUFFICIENT_STOCK", "c-1");

    private void brokerAnswers(boolean ack, String reason) {
        doAnswer(invocation -> {
            CorrelationData correlation = invocation.getArgument(4);
            correlation.getFuture().complete(new CorrelationData.Confirm(ack, reason));
            return null;
        }).when(rabbitTemplate).convertAndSend(
                eq(OrderMessaging.EXCHANGE), eq("order.result.rejected"),
                eq((Object) event), any(MessagePostProcessor.class), any(CorrelationData.class));
    }

    @Test
    void routesByResultStatusAndReturnsWhenBrokerAcks() {
        brokerAnswers(true, null);

        publisher.publish(event);
    }

    @Test
    void failsWhenBrokerNacks() {
        brokerAnswers(false, "disk full");

        assertThatThrownBy(() -> publisher.publish(event))
                .isInstanceOf(ResultPublishException.class)
                .hasMessageContaining("nacked");
    }

    @Test
    void failsWhenNoConfirmArrivesInTime() {
        assertThatThrownBy(() -> publisher.publish(event))
                .isInstanceOf(ResultPublishException.class)
                .hasMessageContaining("No broker confirm");
    }
}
