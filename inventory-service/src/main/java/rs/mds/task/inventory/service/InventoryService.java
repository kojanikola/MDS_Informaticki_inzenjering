package rs.mds.task.inventory.service;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import rs.mds.task.events.OrderCreatedEvent;
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

    public Reservation process(OrderCreatedEvent event) {
        ReserveOutcome outcome = stockRepository.tryReserve(event.itemId(), event.quantity());
        Reservation reservation = new Reservation(
                event.orderId(),
                event.itemId(),
                event.quantity(),
                outcome == ReserveOutcome.RESERVED ? ReservationStatus.RESERVED : ReservationStatus.REJECTED,
                outcome == ReserveOutcome.RESERVED ? null : outcome.name(),
                Instant.now());
        reservationRepository.save(reservation);
        log.info("Order {} for {} x {}: {}{}", event.orderId(), event.quantity(), event.itemId(),
                reservation.status(), reservation.reason() == null ? "" : " (" + reservation.reason() + ")");
        return reservation;
    }

    public Optional<Reservation> findReservation(String orderId) {
        return reservationRepository.findByOrderId(orderId);
    }

    public Map<String, Integer> currentStock() {
        return stockRepository.snapshot();
    }
}
