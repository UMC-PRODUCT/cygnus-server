package com.umc.product.storage.application.service;

import java.time.Duration;
import java.util.Objects;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.storage.cleanup")
public record FileCleanupProperties(
    boolean enabled,
    Duration pollInterval,
    Duration pendingRetention,
    Duration unreferencedRetention,
    Duration claimTimeout,
    int batchSize,
    int maxAttempts,
    Duration initialBackoff,
    Duration maxBackoff
) {

    public FileCleanupProperties {
        requirePositive(pollInterval, "pollInterval");
        requirePositive(pendingRetention, "pendingRetention");
        requirePositive(unreferencedRetention, "unreferencedRetention");
        requirePositive(claimTimeout, "claimTimeout");
        requirePositive(initialBackoff, "initialBackoff");
        requirePositive(maxBackoff, "maxBackoff");
        if (batchSize < 1) {
            throw new IllegalArgumentException("batchSize must be greater than or equal to 1");
        }
        if (maxAttempts < 1) {
            throw new IllegalArgumentException("maxAttempts must be greater than or equal to 1");
        }
        if (maxBackoff.compareTo(initialBackoff) < 0) {
            throw new IllegalArgumentException("maxBackoff must be greater than or equal to initialBackoff");
        }
    }

    public Duration backoffForAttempt(int attempt) {
        if (attempt < 1) {
            throw new IllegalArgumentException("attempt must be greater than or equal to 1");
        }
        Duration delay = initialBackoff;
        for (int currentAttempt = 1; currentAttempt < attempt; currentAttempt++) {
            try {
                delay = delay.multipliedBy(2);
            } catch (ArithmeticException exception) {
                return maxBackoff;
            }
            if (delay.compareTo(maxBackoff) >= 0) {
                return maxBackoff;
            }
        }
        return delay;
    }

    private static void requirePositive(Duration value, String name) {
        Objects.requireNonNull(value, name + " must not be null");
        if (value.isZero() || value.isNegative()) {
            throw new IllegalArgumentException(name + " must be positive");
        }
    }
}
