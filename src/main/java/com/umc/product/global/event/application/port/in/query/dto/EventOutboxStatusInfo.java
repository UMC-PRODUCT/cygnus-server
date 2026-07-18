package com.umc.product.global.event.application.port.in.query.dto;

import java.time.Instant;
import java.util.UUID;
import java.util.regex.Pattern;

import com.umc.product.global.event.domain.EventOutbox;
import com.umc.product.global.event.domain.EventOutboxStatus;

public record EventOutboxStatusInfo(
    UUID eventId,
    EventOutboxStatus status,
    int attempts,
    Instant availableAt,
    Instant nextAttemptAt,
    Instant leaseUntil,
    String failureCode,
    Instant publishedAt
) {

    private static final Pattern SAFE_FAILURE_CODE_PATTERN = Pattern.compile(
        "[A-Z][A-Z0-9]*(?:-[A-Z0-9]+)*-\\d{3,4}"
            + "|(?:[a-zA-Z_$][\\w$]*\\.)*[A-Za-z_$][\\w$]*(?:Exception|Error)"
    );

    public static EventOutboxStatusInfo from(EventOutbox outbox) {
        StatusTimes statusTimes = switch (outbox.getStatus()) {
            case PENDING -> new StatusTimes(outbox.getNextAttemptAt(), null);
            case PROCESSING -> new StatusTimes(null, outbox.getNextAttemptAt());
            case PUBLISHED, FAILED -> new StatusTimes(null, null);
        };
        return new EventOutboxStatusInfo(
            outbox.getEventId(),
            outbox.getStatus(),
            outbox.getAttempts(),
            outbox.getAvailableAt(),
            statusTimes.nextAttemptAt(),
            statusTimes.leaseUntil(),
            safeFailureCode(outbox.getLastError()),
            outbox.getPublishedAt()
        );
    }

    private static String safeFailureCode(String lastError) {
        if (lastError == null || !SAFE_FAILURE_CODE_PATTERN.matcher(lastError).matches()) {
            return null;
        }
        return lastError;
    }

    private record StatusTimes(Instant nextAttemptAt, Instant leaseUntil) {
    }
}
