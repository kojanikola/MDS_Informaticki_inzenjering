package rs.mds.task.events;

/**
 * Broker topology names shared by both services so that they declare identical definitions.
 * A queue redeclared with different arguments is rejected by the broker, so keep both sides in sync.
 */
public final class OrderMessaging {

    public static final String EXCHANGE = "orders.exchange";
    public static final String DEAD_LETTER_EXCHANGE = "orders.dlx";

    // Order -> inventory
    public static final String ORDER_CREATED_ROUTING_KEY = "order.created";
    public static final String ORDER_CREATED_QUEUE = "inventory.order-created";
    public static final String ORDER_CREATED_DEAD_LETTER_ROUTING_KEY = "order.created.dead";
    public static final String ORDER_CREATED_DLQ = "inventory.order-created.dlq";

    // Inventory -> order (processing result)
    public static final String ORDER_RESULT_ROUTING_KEY_PATTERN = "order.result.*";
    public static final String ORDER_RESULT_QUEUE = "order.results";
    public static final String ORDER_RESULT_DEAD_LETTER_ROUTING_KEY = "order.result.dead";
    public static final String ORDER_RESULT_DLQ = "order.results.dlq";

    public static final String CORRELATION_ID_KEY = "correlationId";

    private OrderMessaging() {
    }

    public static String resultRoutingKey(OrderResultStatus status) {
        return "order.result." + status.name().toLowerCase();
    }
}
