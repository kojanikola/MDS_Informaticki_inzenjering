package rs.mds.task.inventory.messaging;

import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import rs.mds.task.events.OrderMessaging;
import rs.mds.task.events.OrderResultEvent;
import rs.mds.task.inventory.exception.ResultPublishException;

/**
 * Publishes with broker confirms, same approach as the order service: returns only after the broker
 * acknowledged the message and fails on nack, unroutable message or confirm timeout.
 */
@Component
public class RabbitOrderResultPublisher implements OrderResultPublisher {

    private static final Logger log = LoggerFactory.getLogger(RabbitOrderResultPublisher.class);

    private final RabbitTemplate rabbitTemplate;
    private final Duration confirmTimeout;

    public RabbitOrderResultPublisher(RabbitTemplate rabbitTemplate,
                                      @Value("${inventory.publisher.confirm-timeout:5s}") Duration confirmTimeout) {
        this.rabbitTemplate = rabbitTemplate;
        this.confirmTimeout = confirmTimeout;
    }

    @Override
    public void publish(OrderResultEvent event) {
        CorrelationData correlation = new CorrelationData(event.eventId());
        String correlationId = MDC.get(OrderMessaging.CORRELATION_ID_KEY);
        try {
            rabbitTemplate.convertAndSend(
                    OrderMessaging.EXCHANGE,
                    OrderMessaging.resultRoutingKey(event.status()),
                    event,
                    message -> {
                        message.getMessageProperties().setMessageId(event.eventId());
                        if (correlationId != null) {
                            message.getMessageProperties().setCorrelationId(correlationId);
                        }
                        return message;
                    },
                    correlation);

            CorrelationData.Confirm confirm = correlation.getFuture()
                    .get(confirmTimeout.toMillis(), TimeUnit.MILLISECONDS);
            if (!confirm.isAck()) {
                throw new ResultPublishException("Broker nacked event " + event.eventId() + ": " + confirm.getReason());
            }
            if (correlation.getReturned() != null) {
                throw new ResultPublishException("Event " + event.eventId() + " was unroutable: "
                        + correlation.getReturned().getReplyText());
            }
            log.info("Published and confirmed result {} for order {}", event.status(), event.orderId());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ResultPublishException("Interrupted while waiting for broker confirm", e);
        } catch (TimeoutException e) {
            throw new ResultPublishException("No broker confirm within " + confirmTimeout, e);
        } catch (ExecutionException | AmqpException e) {
            throw new ResultPublishException("Failed to publish result " + event.eventId(), e);
        }
    }
}
