package rs.mds.task.inventory.config;

import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "inventory")
public record StockProperties(Map<String, Integer> stock) {

    public StockProperties {
        stock = stock == null ? Map.of() : Map.copyOf(stock);
    }
}
