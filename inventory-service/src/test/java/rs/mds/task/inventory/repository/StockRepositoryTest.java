package rs.mds.task.inventory.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import rs.mds.task.inventory.config.StockProperties;
import rs.mds.task.inventory.model.ReserveOutcome;

class StockRepositoryTest {

    private final StockRepository repository = new StockRepository(new StockProperties(Map.of("item-1", 10)));

    @Test
    void reservesWhenEnoughStock() {
        assertThat(repository.tryReserve("item-1", 4)).isEqualTo(ReserveOutcome.RESERVED);
        assertThat(repository.snapshot()).containsEntry("item-1", 6);
    }

    @Test
    void reservesExactRemainingQuantity() {
        assertThat(repository.tryReserve("item-1", 10)).isEqualTo(ReserveOutcome.RESERVED);
        assertThat(repository.snapshot()).containsEntry("item-1", 0);
    }

    @Test
    void rejectsWhenInsufficientAndLeavesStockUntouched() {
        assertThat(repository.tryReserve("item-1", 11)).isEqualTo(ReserveOutcome.INSUFFICIENT_STOCK);
        assertThat(repository.snapshot()).containsEntry("item-1", 10);
    }

    @Test
    void rejectsUnknownItem() {
        assertThat(repository.tryReserve("nope", 1)).isEqualTo(ReserveOutcome.UNKNOWN_ITEM);
    }

    @Test
    void neverOversellsUnderConcurrency() {
        AtomicInteger reserved = new AtomicInteger();
        IntStream.range(0, 200).parallel().forEach(i -> {
            if (repository.tryReserve("item-1", 1) == ReserveOutcome.RESERVED) {
                reserved.incrementAndGet();
            }
        });

        assertThat(reserved.get()).isEqualTo(10);
        assertThat(repository.snapshot()).containsEntry("item-1", 0);
    }
}
