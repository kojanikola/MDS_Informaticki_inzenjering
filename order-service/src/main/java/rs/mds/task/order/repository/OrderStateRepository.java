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
     * Atomically claims the order id by recording the order as accepted.
     *
     * @return false if the id is already taken, so concurrent requests with the same id cannot both succeed
     */
    public boolean registerAcceptedIfAbsent(String orderId) {
        return states.putIfAbsent(orderId, new OrderState(orderId, OrderStatus.ACCEPTED, null, Instant.now())) == null;
    }

    /**
     * Releases a claimed id when publishing failed. Only an order still in ACCEPTED is removed, so a result
     * that already arrived is never thrown away.
     */
    public void removeIfAccepted(String orderId) {
        states.computeIfPresent(orderId, (id, state) -> state.status() == OrderStatus.ACCEPTED ? null : state);
    }

    /** Last write wins, so redelivered results are harmless. */
    public void save(OrderState state) {
        states.put(state.orderId(), state);
    }

    public Optional<OrderState> findByOrderId(String orderId) {
        return Optional.ofNullable(states.get(orderId));
    }
}
