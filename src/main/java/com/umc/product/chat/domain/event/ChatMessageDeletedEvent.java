package com.umc.product.chat.domain.event;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.umc.product.chat.domain.ChatMessage;

public record ChatMessageDeletedEvent(
    UUID eventId,
    Instant occurredAt,
    ChatMessageSnapshot message
) implements ChatRealtimeEvent {

    public ChatMessageDeletedEvent {
        eventId = eventId == null ? UUID.randomUUID() : eventId;
        occurredAt = occurredAt == null ? Instant.now() : occurredAt;
    }

    public static ChatMessageDeletedEvent from(ChatMessage message) {
        return new ChatMessageDeletedEvent(
            null,
            message.getDeletedAt(),
            ChatMessageSnapshot.from(message, List.of())
        );
    }

    @Override
    public String eventType() {
        return "chat.message.deleted";
    }
}
