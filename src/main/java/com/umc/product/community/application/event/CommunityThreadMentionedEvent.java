package com.umc.product.community.application.event;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.umc.product.global.event.domain.DomainEvent;

public record CommunityThreadMentionedEvent(
    UUID eventId,
    Instant occurredAt,
    Long threadId,
    Long messageId,
    Long senderMemberId,
    List<Long> mentionedMemberIds
) implements DomainEvent {

    public CommunityThreadMentionedEvent {
        eventId = eventId == null ? UUID.randomUUID() : eventId;
        occurredAt = occurredAt == null ? Instant.now() : occurredAt;
        threadId = requirePositive(threadId, "threadId");
        messageId = requirePositive(messageId, "messageId");
        senderMemberId = requirePositive(senderMemberId, "senderMemberId");
        mentionedMemberIds = normalizeIds(mentionedMemberIds, "mentionedMemberIds");
    }

    public static CommunityThreadMentionedEvent of(
        Long threadId,
        Long messageId,
        Long senderMemberId,
        List<Long> mentionedMemberIds
    ) {
        return new CommunityThreadMentionedEvent(
            null,
            null,
            threadId,
            messageId,
            senderMemberId,
            mentionedMemberIds
        );
    }

    @Override
    public String eventType() {
        return "community.thread.message.mentioned";
    }

    private static List<Long> normalizeIds(List<Long> ids, String fieldName) {
        if (ids == null) {
            return List.of();
        }
        return ids.stream()
            .map(id -> requirePositive(id, fieldName))
            .distinct()
            .sorted()
            .toList();
    }

    private static Long requirePositive(Long value, String fieldName) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(fieldName + " must be positive");
        }
        return value;
    }
}
