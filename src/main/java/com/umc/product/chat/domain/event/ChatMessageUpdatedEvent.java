package com.umc.product.chat.domain.event;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.umc.product.chat.domain.ChatMessage;

public record ChatMessageUpdatedEvent(
    UUID eventId,
    Instant occurredAt,
    ChatMessageSnapshot message
) implements ChatRealtimeEvent {

    public ChatMessageUpdatedEvent {
        eventId = eventId == null ? UUID.randomUUID() : eventId;
        occurredAt = occurredAt == null ? Instant.now() : occurredAt;
    }

    public static ChatMessageUpdatedEvent from(ChatMessage message, List<Long> mentionedMemberIds) {
        return new ChatMessageUpdatedEvent(
            null,
            message.getEditedAt(),
            ChatMessageSnapshot.from(message, mentionedMemberIds)
        );
    }

    @Override
    public String eventType() {
        return "chat.message.updated";
    }
}
