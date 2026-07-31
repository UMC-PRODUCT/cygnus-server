package com.umc.product.community.domain.event;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public record CommunityThreadMemberKickedEvent(
    UUID eventId,
    Instant occurredAt,
    Long threadId,
    Long actorMemberId,
    Long memberId,
    List<Long> activeMemberIds
) implements CommunityThreadLifecycleEvent {

    public CommunityThreadMemberKickedEvent {
        eventId = CommunityThreadEventSupport.eventId(eventId);
        occurredAt = CommunityThreadEventSupport.occurredAt(occurredAt);
        threadId = CommunityThreadEventSupport.id(threadId);
        actorMemberId = CommunityThreadEventSupport.id(actorMemberId);
        memberId = CommunityThreadEventSupport.id(memberId);
        activeMemberIds = CommunityThreadEventSupport.idSnapshot(activeMemberIds);
    }

    public static CommunityThreadMemberKickedEvent of(
        Long threadId,
        Long actorMemberId,
        Long memberId,
        Collection<Long> activeMemberIds,
        Instant occurredAt
    ) {
        return new CommunityThreadMemberKickedEvent(
            UUID.randomUUID(),
            occurredAt,
            threadId,
            actorMemberId,
            memberId,
            List.copyOf(activeMemberIds)
        );
    }

    @Override
    public String eventType() {
        return "community.thread.member.kicked";
    }
}
