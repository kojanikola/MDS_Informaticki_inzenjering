package rs.mds.task.inventory.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Listener retry settings: exponential backoff, then the message is dead-lettered.
 */
@ConfigurationProperties(prefix = "inventory.retry")
public record RetryProperties(
        @DefaultValue("3") int maxAttempts,
        @DefaultValue("1s") Duration initialInterval,
        @DefaultValue("2.0") double multiplier,
        @DefaultValue("10s") Duration maxInterval) {
}
