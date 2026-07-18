package com.umc.product.community.application.port.in.realtime.dto;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record CommunityThreadRealtimeEvent<P extends CommunityThreadRealtimePayload>(
    UUID eventId,
    CommunityThreadRealtimeEventType type,
    String threadId,
    Instant occurredAt,
    P payload
) {

    public CommunityThreadRealtimeEvent {
        Objects.requireNonNull(eventId, "eventId must not be null");
        Objects.requireNonNull(type, "type must not be null");
        threadId = requireThreadId(threadId);
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        Objects.requireNonNull(payload, "payload must not be null");
    }

    public static <P extends CommunityThreadRealtimePayload> CommunityThreadRealtimeEvent<P> of(
        UUID eventId,
        CommunityThreadRealtimeEventType type,
        Long threadId,
        Instant occurredAt,
        P payload
    ) {
        if (threadId == null || threadId <= 0) {
            throw new IllegalArgumentException("threadId must be positive");
        }
        return new CommunityThreadRealtimeEvent<>(
            eventId,
            type,
            threadId.toString(),
            occurredAt,
            payload
        );
    }

    private static String requireThreadId(String threadId) {
        if (threadId == null || threadId.isBlank()) {
            throw new IllegalArgumentException("threadId must not be blank");
        }
        try {
            if (Long.parseLong(threadId) <= 0) {
                throw new IllegalArgumentException("threadId must be positive");
            }
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("threadId must be a positive number", exception);
        }
        return threadId;
    }
}
