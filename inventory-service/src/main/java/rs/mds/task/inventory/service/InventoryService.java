package rs.mds.task.inventory.service;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import rs.mds.task.events.OrderCreatedEvent;
import rs.mds.task.inventory.exception.InvalidOrderEventException;
import rs.mds.task.inventory.model.Reservation;
import rs.mds.task.inventory.model.ReservationStatus;
import rs.mds.task.inventory.model.ReserveOutcome;
import rs.mds.task.inventory.repository.ReservationRepository;
import rs.mds.task.inventory.repository.StockRepository;

@Service
public class InventoryService {

    private static final Logger log = LoggerFactory.getLogger(InventoryService.class);

    private final StockRepository stockRepository;
    private final ReservationRepository reservationRepository;

    public InventoryService(StockRepository stockRepository, ReservationRepository reservationRepository) {
        this.stockRepository = stockRepository;
        this.reservationRepository = reservationRepository;
    }

    /**
     * Idempotent on {@code orderId}: the broker delivers at least once, so a redelivered event returns the
     * original result instead of reserving stock a second time.
     */
    public Reservation process(OrderCreatedEvent event) {
        validate(event);
        boolean[] created = {false};
        Reservation reservation = reservationRepository.getOrCreate(event.orderId(), () -> {
            created[0] = true;
            return reserve(event);
        });
        if (!created[0]) {
            log.info("Order {} already processed ({}), ignoring duplicate event {}",
                    event.orderId(), reservation.status(), event.eventId());
        }
        return reservation;
    }

    public Optional<Reservation> findReservation(String orderId) {
        return reservationRepository.findByOrderId(orderId);
    }

    public Map<String, Integer> currentStock() {
        return stockRepository.snapshot();
    }

    private Reservation reserve(OrderCreatedEvent event) {
        ReserveOutcome outcome = stockRepository.tryReserve(event.itemId(), event.quantity());
        Reservation reservation = new Reservation(
                event.orderId(),
                event.itemId(),
                event.quantity(),
                outcome == ReserveOutcome.RESERVED ? ReservationStatus.RESERVED : ReservationStatus.REJECTED,
                outcome == ReserveOutcome.RESERVED ? null : outcome.name(),
                Instant.now());
        log.info("Order {} for {} x {}: {}{}", event.orderId(), event.quantity(), event.itemId(),
                reservation.status(), reservation.reason() == null ? "" : " (" + reservation.reason() + ")");
        return reservation;
    }

    private void validate(OrderCreatedEvent event) {
        if (event == null || isBlank(event.orderId()) || isBlank(event.itemId()) || event.quantity() <= 0) {
            throw new InvalidOrderEventException("Invalid order event: " + event);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
