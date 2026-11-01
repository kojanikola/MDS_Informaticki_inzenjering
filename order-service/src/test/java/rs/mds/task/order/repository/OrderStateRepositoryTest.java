package rs.mds.task.order.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import rs.mds.task.order.model.OrderState;
import rs.mds.task.order.model.OrderStatus;

class OrderStateRepositoryTest {

    private final OrderStateRepository repository = new OrderStateRepository();

    @Test
    void firstRegistrationWinsAndSecondIsRefused() {
        assertThat(repository.registerAcceptedIfAbsent("o-1")).isTrue();
        assertThat(repository.registerAcceptedIfAbsent("o-1")).isFalse();
        assertThat(repository.findByOrderId("o-1")).get().extracting(OrderState::status).isEqualTo(OrderStatus.ACCEPTED);
    }

    @Test
    void onlyOneOfManyConcurrentRegistrationsSucceeds() {
        AtomicInteger winners = new AtomicInteger();
        IntStream.range(0, 200).parallel().forEach(i -> {
            if (repository.registerAcceptedIfAbsent("o-race")) {
                winners.incrementAndGet();
            }
        });

        assertThat(winners.get()).isEqualTo(1);
    }

    @Test
    void removeIfAcceptedReleasesAnAcceptedOrder() {
        repository.registerAcceptedIfAbsent("o-2");

        repository.removeIfAccepted("o-2");

        assertThat(repository.findByOrderId("o-2")).isEmpty();
        assertThat(repository.registerAcceptedIfAbsent("o-2")).isTrue();
    }

    @Test
    void removeIfAcceptedKeepsAnOrderThatAlreadyHasAResult() {
        repository.registerAcceptedIfAbsent("o-3");
        repository.save(new OrderState("o-3", OrderStatus.RESERVED, null, Instant.now()));

        repository.removeIfAccepted("o-3");

        assertThat(repository.findByOrderId("o-3")).get().extracting(OrderState::status).isEqualTo(OrderStatus.RESERVED);
    }

    @Test
    void resultOverwritesAcceptedState() {
        repository.registerAcceptedIfAbsent("o-4");

        repository.save(new OrderState("o-4", OrderStatus.REJECTED, "INSUFFICIENT_STOCK", Instant.now()));

        assertThat(repository.findByOrderId("o-4")).get().extracting(OrderState::status).isEqualTo(OrderStatus.REJECTED);
    }
}
