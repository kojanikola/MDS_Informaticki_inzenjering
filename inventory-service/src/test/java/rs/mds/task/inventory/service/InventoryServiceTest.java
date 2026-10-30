package rs.mds.task.inventory.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;
import rs.mds.task.events.OrderCreatedEvent;
import rs.mds.task.inventory.config.StockProperties;
import rs.mds.task.inventory.model.Reservation;
import rs.mds.task.inventory.model.ReservationStatus;
import rs.mds.task.inventory.repository.ReservationRepository;
import rs.mds.task.inventory.repository.StockRepository;

class InventoryServiceTest {

    private final InventoryService service = new InventoryService(
            new StockRepository(new StockProperties(Map.of("item-1", 5))),
            new ReservationRepository());

    @Test
    void reservesOrderWhenStockIsSufficient() {
        Reservation reservation = service.process(OrderCreatedEvent.create("o-1", "item-1", 2));

        assertThat(reservation.status()).isEqualTo(ReservationStatus.RESERVED);
        assertThat(reservation.reason()).isNull();
        assertThat(service.currentStock()).containsEntry("item-1", 3);
        assertThat(service.findReservation("o-1")).contains(reservation);
    }

    @Test
    void rejectsOrderWhenStockIsInsufficient() {
        Reservation reservation = service.process(OrderCreatedEvent.create("o-2", "item-1", 6));

        assertThat(reservation.status()).isEqualTo(ReservationStatus.REJECTED);
        assertThat(reservation.reason()).isEqualTo("INSUFFICIENT_STOCK");
        assertThat(service.currentStock()).containsEntry("item-1", 5);
    }

    @Test
    void rejectsOrderForUnknownItem() {
        Reservation reservation = service.process(OrderCreatedEvent.create("o-3", "ghost", 1));

        assertThat(reservation.status()).isEqualTo(ReservationStatus.REJECTED);
        assertThat(reservation.reason()).isEqualTo("UNKNOWN_ITEM");
    }
}
