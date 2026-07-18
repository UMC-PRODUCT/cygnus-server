package com.umc.product.notification.application.port.in.dto;

import java.time.Instant;
import java.util.UUID;

import com.umc.product.global.event.domain.EventOutboxStatus;

public record TemplateEmailRequestInfo(
    UUID eventId,
    EventOutboxStatus status,
    boolean deduplicated,
    Instant availableAt,
    Instant nextAttemptAt
) {
}
