package com.umc.product.global.event.application.port.in.query.dto;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

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
            safeFailureCode(outbox),
            outbox.getPublishedAt()
        );
    }

    private static String safeFailureCode(EventOutbox outbox) {
        if (!Objects.equals(outbox.getLastError(), outbox.getSanitizedLastError())) {
            return null;
        }
        return outbox.getSanitizedLastError();
    }

    private record StatusTimes(Instant nextAttemptAt, Instant leaseUntil) {
    }
}
