package rs.mds.task.order.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import rs.mds.task.events.OrderMessaging;

/**
 * Declares the topology the order service publishes into. The queue is declared here as well
 * (identically to the inventory service) so orders are buffered even if inventory has not started yet.
 */
@Configuration
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

    @Bean
    MessageConverter jsonMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }
}
