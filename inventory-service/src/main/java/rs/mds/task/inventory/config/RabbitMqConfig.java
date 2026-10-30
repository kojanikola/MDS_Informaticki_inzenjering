package rs.mds.task.inventory.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import rs.mds.task.events.OrderMessaging;

/**
 * Declares the same topology as the order service (including the dead-letter setup) so either can start first.
 */
@Configuration
@EnableConfigurationProperties({StockProperties.class, RetryProperties.class})
public class RabbitMqConfig {

    @Bean
    TopicExchange ordersExchange() {
        return ExchangeBuilder.topicExchange(OrderMessaging.EXCHANGE).durable(true).build();
    }

    @Bean
    DirectExchange deadLetterExchange() {
        return ExchangeBuilder.directExchange(OrderMessaging.DEAD_LETTER_EXCHANGE).durable(true).build();
    }

    @Bean
    Queue orderCreatedQueue() {
        return QueueBuilder.durable(OrderMessaging.ORDER_CREATED_QUEUE)
                .deadLetterExchange(OrderMessaging.DEAD_LETTER_EXCHANGE)
                .deadLetterRoutingKey(OrderMessaging.ORDER_CREATED_DEAD_LETTER_ROUTING_KEY)
                .build();
    }

    @Bean
    Queue orderCreatedDeadLetterQueue() {
        return QueueBuilder.durable(OrderMessaging.ORDER_CREATED_DLQ).build();
    }

    @Bean
    Binding orderCreatedBinding(Queue orderCreatedQueue, TopicExchange ordersExchange) {
        return BindingBuilder.bind(orderCreatedQueue)
                .to(ordersExchange)
                .with(OrderMessaging.ORDER_CREATED_ROUTING_KEY);
    }

    @Bean
    Binding orderCreatedDeadLetterBinding(Queue orderCreatedDeadLetterQueue, DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(orderCreatedDeadLetterQueue)
                .to(deadLetterExchange)
                .with(OrderMessaging.ORDER_CREATED_DEAD_LETTER_ROUTING_KEY);
    }

    @Bean
    Queue orderResultQueue() {
        return QueueBuilder.durable(OrderMessaging.ORDER_RESULT_QUEUE)
                .deadLetterExchange(OrderMessaging.DEAD_LETTER_EXCHANGE)
                .deadLetterRoutingKey(OrderMessaging.ORDER_RESULT_DEAD_LETTER_ROUTING_KEY)
                .build();
    }

    @Bean
    Queue orderResultDeadLetterQueue() {
        return QueueBuilder.durable(OrderMessaging.ORDER_RESULT_DLQ).build();
    }

    @Bean
    Binding orderResultBinding(Queue orderResultQueue, TopicExchange ordersExchange) {
        return BindingBuilder.bind(orderResultQueue)
                .to(ordersExchange)
                .with(OrderMessaging.ORDER_RESULT_ROUTING_KEY_PATTERN);
    }

    @Bean
    Binding orderResultDeadLetterBinding(Queue orderResultDeadLetterQueue, DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(orderResultDeadLetterQueue)
                .to(deadLetterExchange)
                .with(OrderMessaging.ORDER_RESULT_DEAD_LETTER_ROUTING_KEY);
    }

    /**
     * Always converts to the type declared by the listener method, ignoring the producer's
     * {@code __TypeId__} header, so the services are not coupled to each other's class names.
     */
    @Bean
    MessageConverter jsonMessageConverter(ObjectMapper objectMapper) {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter(objectMapper);
        converter.setAlwaysConvertToInferredType(true);
        return converter;
    }
}
