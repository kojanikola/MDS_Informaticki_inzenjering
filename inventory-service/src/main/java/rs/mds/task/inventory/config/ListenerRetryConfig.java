package rs.mds.task.inventory.config;

import java.util.Map;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.retry.RejectAndDontRequeueRecoverer;
import org.springframework.amqp.support.converter.MessageConversionException;
import org.springframework.amqp.rabbit.config.ContainerCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.policy.SimpleRetryPolicy;
import rs.mds.task.inventory.exception.InvalidOrderEventException;

/**
 * Failed deliveries are retried with exponential backoff; once attempts are exhausted the message is
 * rejected without requeue, which routes it to the dead-letter queue. Poison messages skip the retries.
 */
@Configuration
public class ListenerRetryConfig {

    @Bean
    SimpleRetryPolicy listenerRetryPolicy(RetryProperties properties) {
        // The listener wraps exceptions, so causes are traversed; anything not listed is retried.
        // Poison messages (invalid content, unparseable payload) can never succeed, so they are not retried.
        return new SimpleRetryPolicy(
                properties.maxAttempts(),
                Map.of(InvalidOrderEventException.class, false,
                        MessageConversionException.class, false),
                true,
                true);
    }

    @Bean
    ContainerCustomizer<SimpleMessageListenerContainer> retryCustomizer(
            SimpleRetryPolicy listenerRetryPolicy, RetryProperties properties) {
        return container -> container.setAdviceChain(RetryInterceptorBuilder.stateless()
                .retryPolicy(listenerRetryPolicy)
                .backOffOptions(
                        properties.initialInterval().toMillis(),
                        properties.multiplier(),
                        properties.maxInterval().toMillis())
                .recoverer(new RejectAndDontRequeueRecoverer())
                .build());
    }
}
