package com.umc.product.community.domain.event;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public record CommunityThreadDeletedEvent(
    UUID eventId,
    Instant occurredAt,
    Long threadId,
    Long actorMemberId,
    List<Long> activeMemberIds
) implements CommunityThreadLifecycleEvent {

    public CommunityThreadDeletedEvent {
        eventId = CommunityThreadEventSupport.eventId(eventId);
        occurredAt = CommunityThreadEventSupport.occurredAt(occurredAt);
        threadId = CommunityThreadEventSupport.id(threadId);
        actorMemberId = CommunityThreadEventSupport.id(actorMemberId);
        activeMemberIds = CommunityThreadEventSupport.idSnapshot(activeMemberIds);
    }

    public static CommunityThreadDeletedEvent of(
        Long threadId,
        Long actorMemberId,
        Collection<Long> activeMemberIds,
        Instant occurredAt
    ) {
        return new CommunityThreadDeletedEvent(
            UUID.randomUUID(),
            occurredAt,
            threadId,
            actorMemberId,
            List.copyOf(activeMemberIds)
        );
    }

    @Override
    public String eventType() {
        return "community.thread.deleted";
    }
}
