package com.umc.product.global.event.application.port.out.dto;

import java.time.Instant;
import java.util.UUID;

import com.umc.product.global.event.domain.EventOutbox;
import com.umc.product.global.event.domain.EventOutboxStatus;

public record OutboxPublishResult(
    UUID eventId,
    EventOutboxStatus status,
    boolean deduplicated,
    Instant availableAt,
    Instant nextAttemptAt
) {

    public static OutboxPublishResult from(EventOutbox outbox, boolean deduplicated) {
        Instant nextAttemptAt = switch (outbox.getStatus()) {
            case PENDING -> outbox.getNextAttemptAt();
            case PROCESSING, PUBLISHED, FAILED -> null;
        };
        return new OutboxPublishResult(
            outbox.getEventId(),
            outbox.getStatus(),
            deduplicated,
            outbox.getAvailableAt(),
            nextAttemptAt
        );
    }
}
