package com.umc.product.chat.domain.event;

import java.time.Instant;
import java.util.UUID;

public record ChatReadUpdatedEvent(
    UUID eventId,
    Instant occurredAt,
    Long roomId,
    Long memberId,
    Long lastReadMessageId
) implements ChatRealtimeEvent {

    public ChatReadUpdatedEvent {
        eventId = eventId == null ? UUID.randomUUID() : eventId;
        occurredAt = occurredAt == null ? Instant.now() : occurredAt;
    }

    public static ChatReadUpdatedEvent of(Long roomId, Long memberId, Long lastReadMessageId) {
        return new ChatReadUpdatedEvent(null, null, roomId, memberId, lastReadMessageId);
    }

    @Override
    public String eventType() {
        return "chat.read.updated";
    }
}
