package rs.mds.task.inventory.repository;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;
import rs.mds.task.inventory.model.Reservation;

@Repository
public class ReservationRepository {

    private final ConcurrentHashMap<String, Reservation> reservations = new ConcurrentHashMap<>();

    public void save(Reservation reservation) {
        reservations.put(reservation.orderId(), reservation);
    }

    public Optional<Reservation> findByOrderId(String orderId) {
        return Optional.ofNullable(reservations.get(orderId));
    }
}
