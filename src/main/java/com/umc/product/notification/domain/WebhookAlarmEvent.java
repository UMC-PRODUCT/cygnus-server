package com.umc.product.notification.domain;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import com.umc.product.global.event.domain.DomainEvent;

public record WebhookAlarmEvent(
    UUID eventId,
    Instant occurredAt,
    String eventType,
    List<WebhookPlatform> platforms,
    String title,
    String content
) implements DomainEvent {

    private static final String EVENT_TYPE = "notification.webhook.alarm.requested";

    public WebhookAlarmEvent {
        if (eventId == null) {
            eventId = UUID.randomUUID();
        }
        if (occurredAt == null) {
            occurredAt = Instant.now();
        }
        if (eventType == null || eventType.isBlank()) {
            eventType = EVENT_TYPE;
        }
        Objects.requireNonNull(platforms, "platforms must not be null");
        Objects.requireNonNull(title, "title must not be null");
        Objects.requireNonNull(content, "content must not be null");
        if (platforms.isEmpty()) {
            throw new IllegalArgumentException("platforms must not be empty");
        }
        platforms = List.copyOf(platforms);
    }

    public static WebhookAlarmEvent of(List<WebhookPlatform> platforms, String title, String content) {
        return new WebhookAlarmEvent(null, null, null, platforms, title, content);
    }
}
