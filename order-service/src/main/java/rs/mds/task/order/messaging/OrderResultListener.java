package rs.mds.task.order.messaging;

import org.slf4j.MDC;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import rs.mds.task.events.OrderMessaging;
import rs.mds.task.events.OrderResultEvent;
import rs.mds.task.order.service.OrderService;

@Component
public class OrderResultListener {

    private final OrderService orderService;

    public OrderResultListener(OrderService orderService) {
        this.orderService = orderService;
    }

    @RabbitListener(queues = OrderMessaging.ORDER_RESULT_QUEUE)
    public void onOrderResult(OrderResultEvent event,
                              @Header(name = AmqpHeaders.CORRELATION_ID, required = false) String correlationId) {
        if (correlationId != null) {
            MDC.put(OrderMessaging.CORRELATION_ID_KEY, correlationId);
        }
        try {
            orderService.applyResult(event);
        } finally {
            MDC.remove(OrderMessaging.CORRELATION_ID_KEY);
        }
    }
}
