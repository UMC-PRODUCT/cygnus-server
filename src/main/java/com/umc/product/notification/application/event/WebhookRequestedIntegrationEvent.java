package com.umc.product.notification.application.event;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.umc.product.global.event.domain.IntegrationEvent;
import com.umc.product.notification.domain.WebhookAlarmEvent;
import com.umc.product.notification.domain.WebhookPlatform;

public record WebhookRequestedIntegrationEvent(
    UUID eventId,
    Instant occurredAt,
    UUID requestId,
    Detail detail
) implements IntegrationEvent {

    private static final String EVENT_TYPE = "notification.webhook.requested.v1";
    private static final String SOURCE = "umc-product.notification-gateway";

    public WebhookRequestedIntegrationEvent {
        if (eventId == null) {
            eventId = UUID.randomUUID();
        }
        if (occurredAt == null) {
            occurredAt = Instant.now();
        }
        if (requestId == null || detail == null) {
            throw new IllegalArgumentException("Webhook integration event 정보가 올바르지 않습니다.");
        }
    }

    public static WebhookRequestedIntegrationEvent from(WebhookAlarmEvent source) {
        return new WebhookRequestedIntegrationEvent(
            null,
            source.occurredAt(),
            source.eventId(),
            new Detail(source.platforms(), source.title(), source.content(), 1)
        );
    }

    @Override
    public String eventType() {
        return EVENT_TYPE;
    }

    @Override
    public String source() {
        return SOURCE;
    }

    public record Detail(
        List<WebhookPlatform> platforms,
        String title,
        String content,
        int attempt
    ) {

        public Detail {
            platforms = List.copyOf(platforms);
        }
    }
}
