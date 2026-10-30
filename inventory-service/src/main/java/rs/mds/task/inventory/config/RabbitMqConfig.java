package rs.mds.task.inventory.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
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
 * Declares the same exchange, queue and binding as the order service so either can start first.
 */
@Configuration
@EnableConfigurationProperties(StockProperties.class)
public class RabbitMqConfig {

    @Bean
    TopicExchange ordersExchange() {
        return ExchangeBuilder.topicExchange(OrderMessaging.EXCHANGE).durable(true).build();
    }

    @Bean
    Queue orderCreatedQueue() {
        return QueueBuilder.durable(OrderMessaging.ORDER_CREATED_QUEUE).build();
    }

    @Bean
    Binding orderCreatedBinding(Queue orderCreatedQueue, TopicExchange ordersExchange) {
        return BindingBuilder.bind(orderCreatedQueue)
                .to(ordersExchange)
                .with(OrderMessaging.ORDER_CREATED_ROUTING_KEY);
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
