package com.umc.product.chat.domain.event;

import java.time.Instant;
import java.util.UUID;

import com.umc.product.global.event.domain.DomainEvent;

public record ChatRoomPinnedMessageChangedEvent(
    UUID eventId,
    Instant occurredAt,
    Long roomId,
    Long pinnedMessageId
) implements DomainEvent {

    public ChatRoomPinnedMessageChangedEvent {
        if (eventId == null) {
            eventId = UUID.randomUUID();
        }
        if (occurredAt == null) {
            occurredAt = Instant.now();
        }
    }

    public static ChatRoomPinnedMessageChangedEvent of(Long roomId, Long pinnedMessageId) {
        return new ChatRoomPinnedMessageChangedEvent(null, null, roomId, pinnedMessageId);
    }

    @Override
    public String eventType() {
        return "chat.room.pinned-message.changed";
    }
}
