package com.umc.product.global.event.domain;

import java.util.UUID;

public record IntegrationEventEnvelope(
    int schemaVersion,
    UUID eventId,
    String eventType,
    String source,
    String occurredAt,
    String traceparent,
    UUID requestId,
    Object detail
) {

    public static IntegrationEventEnvelope from(IntegrationEvent event, String traceparent) {
        return new IntegrationEventEnvelope(
            event.schemaVersion(),
            event.eventId(),
            event.eventType(),
            event.source(),
            event.occurredAt().toString(),
            traceparent,
            event.requestId(),
            event.detail()
        );
    }
}
