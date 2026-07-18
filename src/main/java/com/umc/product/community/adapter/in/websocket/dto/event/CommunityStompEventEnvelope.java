package com.umc.product.community.adapter.in.websocket.dto.event;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record CommunityStompEventEnvelope<T>(
    UUID eventId,
    String type,
    String threadId,
    Instant occurredAt,
    T payload
) {

    public CommunityStompEventEnvelope {
        Objects.requireNonNull(eventId, "eventId must not be null");
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(threadId, "threadId must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        Objects.requireNonNull(payload, "payload must not be null");
    }

    public static CommunityStompEventEnvelope<CommunityCommandAcknowledgement> acknowledged(
        Long threadId,
        CommunityCommandAcknowledgement payload
    ) {
        return new CommunityStompEventEnvelope<>(
            UUID.randomUUID(),
            "command.acknowledged",
            String.valueOf(threadId),
            Instant.now(),
            payload
        );
    }
}
