package com.umc.product.community.domain.event;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public record CommunityThreadInvitedEvent(
    UUID eventId,
    Instant occurredAt,
    Long threadId,
    Long inviterMemberId,
    List<Long> invitedMemberIds
) implements CommunityThreadLifecycleEvent {

    public CommunityThreadInvitedEvent {
        eventId = CommunityThreadEventSupport.eventId(eventId);
        occurredAt = CommunityThreadEventSupport.occurredAt(occurredAt);
        threadId = CommunityThreadEventSupport.id(threadId);
        inviterMemberId = CommunityThreadEventSupport.id(inviterMemberId);
        invitedMemberIds = CommunityThreadEventSupport.idSnapshot(invitedMemberIds);
    }

    public static CommunityThreadInvitedEvent of(
        Long threadId,
        Long inviterMemberId,
        Collection<Long> invitedMemberIds,
        Instant occurredAt
    ) {
        return new CommunityThreadInvitedEvent(
            UUID.randomUUID(),
            occurredAt,
            threadId,
            inviterMemberId,
            List.copyOf(invitedMemberIds)
        );
    }

    @Override
    public String eventType() {
        return "community.thread.invited";
    }
}
