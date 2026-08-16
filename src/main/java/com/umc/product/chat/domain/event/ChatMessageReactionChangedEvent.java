package com.umc.product.chat.domain.event;

import java.time.Instant;
import java.util.UUID;

public record ChatMessageReactionChangedEvent(
    UUID eventId,
    Instant occurredAt,
    Long roomId,
    Long messageId,
    Long memberId,
    String emoji,
    boolean added
) implements ChatRealtimeEvent {

    public ChatMessageReactionChangedEvent {
        eventId = eventId == null ? UUID.randomUUID() : eventId;
        occurredAt = occurredAt == null ? Instant.now() : occurredAt;
    }

    public static ChatMessageReactionChangedEvent of(
        Long roomId,
        Long messageId,
        Long memberId,
        String emoji,
        boolean added
    ) {
        return new ChatMessageReactionChangedEvent(null, null, roomId, messageId, memberId, emoji, added);
    }

    @Override
    public String eventType() {
        return "chat.message.reaction.changed";
    }
}
