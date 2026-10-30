package rs.mds.task.inventory.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.support.ListenerExecutionFailedException;
import org.springframework.retry.RetryContext;
import org.springframework.retry.policy.SimpleRetryPolicy;
import rs.mds.task.inventory.exception.InvalidOrderEventException;

class ListenerRetryConfigTest {

    private final SimpleRetryPolicy policy = new ListenerRetryConfig().listenerRetryPolicy(
            new RetryProperties(3, Duration.ofSeconds(1), 2.0, Duration.ofSeconds(10)));

    private boolean canRetryAfter(Throwable failure, int failures) {
        RetryContext context = policy.open(null);
        for (int i = 0; i < failures; i++) {
            policy.registerThrowable(context, failure);
        }
        return policy.canRetry(context);
    }

    private ListenerExecutionFailedException wrapped(Throwable cause) {
        return new ListenerExecutionFailedException("listener failed", cause);
    }

    @Test
    void retriesTransientFailuresUntilMaxAttempts() {
        Throwable transientFailure = wrapped(new IllegalStateException("downstream unavailable"));

        assertThat(canRetryAfter(transientFailure, 1)).isTrue();
        assertThat(canRetryAfter(transientFailure, 2)).isTrue();
        assertThat(canRetryAfter(transientFailure, 3)).isFalse();
    }

    @Test
    void doesNotRetryPoisonMessages() {
        assertThat(canRetryAfter(wrapped(new InvalidOrderEventException("bad")), 1)).isFalse();
    }
}
