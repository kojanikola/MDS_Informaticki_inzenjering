package rs.mds.task.inventory.repository;

import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;
import rs.mds.task.inventory.config.StockProperties;
import rs.mds.task.inventory.model.ReserveOutcome;

/**
 * In-memory stock. Reservation is atomic per item, so concurrent consumers cannot oversell.
 */
@Repository
public class StockRepository {

    private final ConcurrentHashMap<String, Integer> stock;

    public StockRepository(StockProperties properties) {
        this.stock = new ConcurrentHashMap<>(properties.stock());
    }

    public ReserveOutcome tryReserve(String itemId, int quantity) {
        ReserveOutcome[] outcome = {ReserveOutcome.UNKNOWN_ITEM};
        stock.computeIfPresent(itemId, (id, available) -> {
            if (available >= quantity) {
                outcome[0] = ReserveOutcome.RESERVED;
                return available - quantity;
            }
            outcome[0] = ReserveOutcome.INSUFFICIENT_STOCK;
            return available;
        });
        return outcome[0];
    }

    public Map<String, Integer> snapshot() {
        return new TreeMap<>(stock);
    }
}
