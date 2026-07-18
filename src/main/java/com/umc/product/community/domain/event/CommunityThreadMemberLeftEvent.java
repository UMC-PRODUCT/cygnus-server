package com.umc.product.community.domain.event;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public record CommunityThreadMemberLeftEvent(
    UUID eventId,
    Instant occurredAt,
    Long threadId,
    Long memberId,
    List<Long> activeMemberIds
) implements CommunityThreadLifecycleEvent {

    public CommunityThreadMemberLeftEvent {
        eventId = CommunityThreadEventSupport.eventId(eventId);
        occurredAt = CommunityThreadEventSupport.occurredAt(occurredAt);
        threadId = CommunityThreadEventSupport.id(threadId);
        memberId = CommunityThreadEventSupport.id(memberId);
        activeMemberIds = CommunityThreadEventSupport.idSnapshot(activeMemberIds);
    }

    public static CommunityThreadMemberLeftEvent of(
        Long threadId,
        Long memberId,
        Collection<Long> activeMemberIds,
        Instant occurredAt
    ) {
        return new CommunityThreadMemberLeftEvent(
            UUID.randomUUID(),
            occurredAt,
            threadId,
            memberId,
            List.copyOf(activeMemberIds)
        );
    }

    @Override
    public String eventType() {
        return "community.thread.member.left";
    }
}
