package rs.mds.task.order.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import rs.mds.task.events.OrderCreatedEvent;

/**
 * Temporary stand-in until the RabbitMQ publisher is added in the messaging step.
 */
@Component
public class LoggingOrderEventPublisher implements OrderEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(LoggingOrderEventPublisher.class);

    @Override
    public void publish(OrderCreatedEvent event) {
        log.info("Would publish {}", event);
    }
}
