package rs.mds.task.events;

/**
 * Broker topology names shared by both services so that they declare identical definitions.
 */
public final class OrderMessaging {

    public static final String EXCHANGE = "orders.exchange";
    public static final String ORDER_CREATED_ROUTING_KEY = "order.created";
    public static final String ORDER_CREATED_QUEUE = "inventory.order-created";

    private OrderMessaging() {
    }
}
