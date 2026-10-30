package rs.mds.task.inventory;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Starts a real RabbitMQ for integration tests; skipped automatically when Docker is not available.
 */
@Testcontainers(disabledWithoutDocker = true)
public abstract class RabbitIntegrationTest {

    @Container
    static final RabbitMQContainer RABBIT = new RabbitMQContainer("rabbitmq:3-management");

    @DynamicPropertySource
    static void rabbitProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.rabbitmq.host", RABBIT::getHost);
        registry.add("spring.rabbitmq.port", RABBIT::getAmqpPort);
    }
}
