package rs.mds.task.order.messaging;

import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import rs.mds.task.events.OrderCreatedEvent;
import rs.mds.task.events.OrderMessaging;
import rs.mds.task.order.exception.OrderPublishException;

/**
 * Publishes with broker confirms: returns only after RabbitMQ acknowledged the message,
 * and fails if it was nacked, unroutable or the confirm timed out.
 * Messages are persistent (the default delivery mode of the JSON converter).
 */
@Component
public class RabbitOrderEventPublisher implements OrderEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(RabbitOrderEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;
    private final Duration confirmTimeout;

    public RabbitOrderEventPublisher(RabbitTemplate rabbitTemplate,
                                     @Value("${order.publisher.confirm-timeout:5s}") Duration confirmTimeout) {
        this.rabbitTemplate = rabbitTemplate;
        this.confirmTimeout = confirmTimeout;
    }

    @Override
    public void publish(OrderCreatedEvent event) {
        CorrelationData correlation = new CorrelationData(event.eventId());
        try {
            rabbitTemplate.convertAndSend(
                    OrderMessaging.EXCHANGE,
                    OrderMessaging.ORDER_CREATED_ROUTING_KEY,
                    event,
                    message -> {
                        message.getMessageProperties().setMessageId(event.eventId());
                        return message;
                    },
                    correlation);

            CorrelationData.Confirm confirm = correlation.getFuture()
                    .get(confirmTimeout.toMillis(), TimeUnit.MILLISECONDS);
            if (!confirm.isAck()) {
                throw new OrderPublishException("Broker nacked event " + event.eventId() + ": " + confirm.getReason());
            }
            if (correlation.getReturned() != null) {
                throw new OrderPublishException("Event " + event.eventId() + " was unroutable: "
                        + correlation.getReturned().getReplyText());
            }
            log.info("Published and confirmed {} for order {}", event.eventId(), event.orderId());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new OrderPublishException("Interrupted while waiting for broker confirm", e);
        } catch (TimeoutException e) {
            throw new OrderPublishException("No broker confirm within " + confirmTimeout, e);
        } catch (ExecutionException | AmqpException e) {
            throw new OrderPublishException("Failed to publish event " + event.eventId(), e);
        }
    }
}
