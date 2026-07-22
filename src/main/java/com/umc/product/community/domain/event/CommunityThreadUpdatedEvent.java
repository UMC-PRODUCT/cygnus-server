package com.umc.product.community.domain.event;

import java.time.Instant;
import java.util.UUID;

public record CommunityThreadUpdatedEvent(
    UUID eventId,
    Instant occurredAt,
    Long threadId,
    Long actorMemberId
) implements CommunityThreadLifecycleEvent {

    public CommunityThreadUpdatedEvent {
        eventId = CommunityThreadEventSupport.eventId(eventId);
        occurredAt = CommunityThreadEventSupport.occurredAt(occurredAt);
        threadId = CommunityThreadEventSupport.id(threadId);
        actorMemberId = CommunityThreadEventSupport.id(actorMemberId);
    }

    public static CommunityThreadUpdatedEvent of(
        Long threadId,
        Long actorMemberId,
        Instant occurredAt
    ) {
        return new CommunityThreadUpdatedEvent(
            UUID.randomUUID(),
            occurredAt,
            threadId,
            actorMemberId
        );
    }

    @Override
    public String eventType() {
        return "community.thread.updated";
    }
}
