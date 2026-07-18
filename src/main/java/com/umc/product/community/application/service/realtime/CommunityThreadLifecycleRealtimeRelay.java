package com.umc.product.community.application.service.realtime;

import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.community.application.port.in.realtime.dto.CommunityThreadRealtimeEvent;
import com.umc.product.community.application.port.in.realtime.dto.CommunityThreadRealtimeEventType;
import com.umc.product.community.application.port.in.realtime.dto.CommunityThreadRealtimePayload;
import com.umc.product.community.application.port.out.thread.LoadCommunityThreadMemberPort;
import com.umc.product.community.application.service.realtime.CommunityThreadRealtimeMetrics.Operation;
import com.umc.product.community.domain.event.CommunityThreadDeletedEvent;
import com.umc.product.community.domain.event.CommunityThreadInvitedEvent;
import com.umc.product.community.domain.event.CommunityThreadMemberKickedEvent;
import com.umc.product.community.domain.event.CommunityThreadMemberLeftEvent;
import com.umc.product.community.domain.event.CommunityThreadUpdatedEvent;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
class CommunityThreadLifecycleRealtimeRelay {

    private final CommunityThreadRealtimeDelivery delivery;
    private final LoadCommunityThreadMemberPort loadMemberPort;

    void relay(CommunityThreadInvitedEvent event) {
        delivery.activeThread(event.threadId()).ifPresent(thread -> delivery.fanOutPersonalMembers(
            thread.getId(),
            delivery.currentlyActiveRecipients(thread.getId(), event.invitedMemberIds()),
            Operation.THREAD_INVITED,
            memberId -> delivery.envelope(
                event.eventId(),
                CommunityThreadRealtimeEventType.THREAD_INVITED,
                thread.getId(),
                event.occurredAt(),
                new CommunityThreadRealtimePayload.ThreadInvited(
                    delivery.threadSummary(thread.getId(), memberId)
                )
            )
        ));
    }

    void relay(CommunityThreadUpdatedEvent event) {
        delivery.activeThread(event.threadId()).ifPresent(thread -> {
            List<Long> recipients = delivery.activeRecipients(thread.getId());
            CommunityThreadRealtimeEvent<CommunityThreadRealtimePayload.ThreadUpdated> envelope =
                delivery.envelope(
                    event.eventId(),
                    CommunityThreadRealtimeEventType.THREAD_UPDATED,
                    thread.getId(),
                    event.occurredAt(),
                    new CommunityThreadRealtimePayload.ThreadUpdated(
                        thread.getId().toString(),
                        thread.getTitle(),
                        thread.getDescription(),
                        thread.getCategory(),
                        thread.getIcon(),
                        recipients.size(),
                        delivery.maxMembers(),
                        thread.getLastActivityAt(),
                        thread.getUpdatedAt()
                    )
                );
            delivery.fanOutThreadMembers(
                thread.getId(),
                recipients,
                Operation.THREAD_UPDATED,
                ignored -> envelope
            );
        });
    }

    void relay(CommunityThreadDeletedEvent event) {
        List<Long> recipients = delivery.audience(event.activeMemberIds());
        CommunityThreadRealtimeEvent<CommunityThreadRealtimePayload.ThreadDeleted> envelope =
            delivery.envelope(
                event.eventId(),
                CommunityThreadRealtimeEventType.THREAD_DELETED,
                event.threadId(),
                event.occurredAt(),
                new CommunityThreadRealtimePayload.ThreadDeleted(
                    event.threadId().toString(),
                    event.occurredAt()
                )
            );
        delivery.fanOutThreadMembers(
            event.threadId(),
            recipients,
            Operation.THREAD_DELETED,
            ignored -> envelope
        );
    }

    void relay(CommunityThreadMemberKickedEvent event) {
        List<Long> recipients = delivery.terminalAudience(event.activeMemberIds(), event.memberId());
        CommunityThreadRealtimeEvent<CommunityThreadRealtimePayload.MemberKicked> envelope =
            delivery.envelope(
                event.eventId(),
                CommunityThreadRealtimeEventType.MEMBER_KICKED,
                event.threadId(),
                event.occurredAt(),
                new CommunityThreadRealtimePayload.MemberKicked(
                    event.memberId(),
                    recipients.size() - 1L
                )
            );
        delivery.fanOutThreadMembers(
            event.threadId(),
            recipients,
            Operation.MEMBER_KICKED,
            ignored -> envelope
        );
    }

    @Transactional
    void relay(CommunityThreadMemberLeftEvent event) {
        boolean currentMembershipEpoch = loadMemberPort.findByThreadIdAndMemberIdForUpdate(
            event.threadId(),
            event.memberId()
        )
            .map(member -> member.getJoinedAt().equals(event.membershipJoinedAt()))
            .orElse(false);
        if (!currentMembershipEpoch) {
            delivery.recordSkippedFanOut(Operation.MEMBER_LEFT);
            return;
        }
        List<Long> recipients = delivery.terminalAudience(event.activeMemberIds(), event.memberId());
        CommunityThreadRealtimeEvent<CommunityThreadRealtimePayload.MemberLeft> envelope =
            delivery.envelope(
                event.eventId(),
                CommunityThreadRealtimeEventType.MEMBER_LEFT,
                event.threadId(),
                event.occurredAt(),
                new CommunityThreadRealtimePayload.MemberLeft(
                    event.memberId(),
                    recipients.size() - 1L
                )
            );
        delivery.fanOutThreadMembers(
            event.threadId(),
            recipients,
            Operation.MEMBER_LEFT,
            ignored -> envelope
        );
    }
}
