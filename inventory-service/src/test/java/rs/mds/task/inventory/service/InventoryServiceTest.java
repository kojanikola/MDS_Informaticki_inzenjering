package rs.mds.task.inventory.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;
import org.junit.jupiter.api.Test;
import rs.mds.task.events.OrderCreatedEvent;
import rs.mds.task.inventory.config.StockProperties;
import rs.mds.task.inventory.exception.InvalidOrderEventException;
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
    void duplicateEventDoesNotReserveStockTwice() {
        Reservation first = service.process(OrderCreatedEvent.create("o-4", "item-1", 2));
        Reservation second = service.process(OrderCreatedEvent.create("o-4", "item-1", 2));

        assertThat(second).isSameAs(first);
        assertThat(service.currentStock()).containsEntry("item-1", 3);
    }

    @Test
    void duplicateOfRejectedOrderStaysRejectedEvenIfStockAppears() {
        Reservation first = service.process(OrderCreatedEvent.create("o-5", "item-1", 99));
        Reservation second = service.process(OrderCreatedEvent.create("o-5", "item-1", 1));

        assertThat(first.status()).isEqualTo(ReservationStatus.REJECTED);
        assertThat(second).isSameAs(first);
    }

    @Test
    void invalidEventsAreRejectedAsPoisonMessages() {
        assertThatThrownBy(() -> service.process(OrderCreatedEvent.create("o-6", "item-1", 0)))
                .isInstanceOf(InvalidOrderEventException.class);
        assertThatThrownBy(() -> service.process(OrderCreatedEvent.create(" ", "item-1", 1)))
                .isInstanceOf(InvalidOrderEventException.class);
        assertThatThrownBy(() -> service.process(null)).isInstanceOf(InvalidOrderEventException.class);
        assertThat(service.currentStock()).containsEntry("item-1", 5);
    }

    @Test
    void rejectsOrderForUnknownItem() {
        Reservation reservation = service.process(OrderCreatedEvent.create("o-3", "ghost", 1));

        assertThat(reservation.status()).isEqualTo(ReservationStatus.REJECTED);
        assertThat(reservation.reason()).isEqualTo("UNKNOWN_ITEM");
    }
}
