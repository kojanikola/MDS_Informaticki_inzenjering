package rs.mds.task.order.repository;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;
import rs.mds.task.order.model.OrderState;
import rs.mds.task.order.model.OrderStatus;

@Repository
public class OrderStateRepository {

    private final ConcurrentHashMap<String, OrderState> states = new ConcurrentHashMap<>();

    /**
     * Records the order as accepted unless a result already arrived, which can happen because the
     * inventory answer may beat the HTTP thread that is still returning from the publish.
     */
    public void markAcceptedIfAbsent(String orderId) {
        states.putIfAbsent(orderId, new OrderState(orderId, OrderStatus.ACCEPTED, null, Instant.now()));
    }

    /** Last write wins, so redelivered results are harmless. */
    public void save(OrderState state) {
        states.put(state.orderId(), state);
    }

    public Optional<OrderState> findByOrderId(String orderId) {
        return Optional.ofNullable(states.get(orderId));
    }
}
