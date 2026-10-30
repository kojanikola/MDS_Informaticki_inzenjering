package rs.mds.task.inventory.repository;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import org.springframework.stereotype.Repository;
import rs.mds.task.inventory.model.Reservation;

@Repository
public class ReservationRepository {

    private final ConcurrentHashMap<String, Reservation> reservations = new ConcurrentHashMap<>();

    /**
     * Atomically returns the existing reservation for the order, or creates it with {@code creator}.
     * The creator runs at most once per order id even under concurrent duplicate deliveries.
     */
    public Reservation getOrCreate(String orderId, Supplier<Reservation> creator) {
        return reservations.computeIfAbsent(orderId, id -> creator.get());
    }

    public Optional<Reservation> findByOrderId(String orderId) {
        return Optional.ofNullable(reservations.get(orderId));
    }
}
