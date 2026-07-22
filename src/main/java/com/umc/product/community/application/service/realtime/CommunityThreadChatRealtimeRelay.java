package com.umc.product.community.application.service.realtime;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.umc.product.chat.domain.event.ChatMessageCreatedEvent;
import com.umc.product.chat.domain.event.ChatMessageDeletedEvent;
import com.umc.product.chat.domain.event.ChatMessageReactionChangedEvent;
import com.umc.product.chat.domain.event.ChatMessageUpdatedEvent;
import com.umc.product.chat.domain.event.ChatReadUpdatedEvent;
import com.umc.product.community.application.port.in.query.thread.message.dto.CommunityThreadMessageInfo;
import com.umc.product.community.application.port.in.realtime.dto.CommunityThreadRealtimeEvent;
import com.umc.product.community.application.port.in.realtime.dto.CommunityThreadRealtimeEventType;
import com.umc.product.community.application.port.in.realtime.dto.CommunityThreadRealtimePayload;
import com.umc.product.community.application.service.realtime.CommunityThreadRealtimeMetrics.Operation;
import com.umc.product.community.domain.CommunityThread;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
class CommunityThreadChatRealtimeRelay {

    private final CommunityThreadRealtimeDelivery delivery;

    void relay(ChatMessageCreatedEvent event) {
        delivery.activeThreadByChatRoomId(event.roomId()).ifPresent(thread -> {
            List<Long> recipients = delivery.activeRecipients(thread.getId());
            Map<Long, CommunityThreadMessageInfo> messages = delivery.messages(
                thread.getId(), recipients, event.messageId()
            );
            delivery.fanOutMembers(
                recipients,
                Operation.MESSAGE_CREATED,
                memberId -> delivery.envelope(
                    event.eventId(),
                    CommunityThreadRealtimeEventType.MESSAGE_CREATED,
                    thread.getId(),
                    event.occurredAt(),
                    new CommunityThreadRealtimePayload.MessageCreated(
                        messages.get(memberId),
                        event.clientMessageId()
                    )
                )
            );
        });
    }

    void relay(ChatMessageUpdatedEvent event) {
        delivery.activeThreadByChatRoomId(event.message().roomId()).ifPresent(thread -> {
            List<Long> recipients = delivery.activeRecipients(thread.getId());
            Map<Long, CommunityThreadMessageInfo> messages = delivery.messages(
                thread.getId(), recipients, event.message().messageId()
            );
            delivery.fanOutMembers(
                recipients,
                Operation.MESSAGE_UPDATED,
                memberId -> delivery.envelope(
                    event.eventId(),
                    CommunityThreadRealtimeEventType.MESSAGE_UPDATED,
                    thread.getId(),
                    event.occurredAt(),
                    new CommunityThreadRealtimePayload.MessageUpdated(
                        messages.get(memberId)
                    )
                )
            );
        });
    }

    void relay(ChatMessageDeletedEvent event) {
        delivery.activeThreadByChatRoomId(event.message().roomId()).ifPresent(thread -> {
            List<Long> recipients = delivery.activeRecipients(thread.getId());
            Map<Long, CommunityThreadMessageInfo> messages = delivery.messages(
                thread.getId(), recipients, event.message().messageId()
            );
            delivery.fanOutMembers(
                recipients,
                Operation.MESSAGE_DELETED,
                memberId -> delivery.envelope(
                    event.eventId(),
                    CommunityThreadRealtimeEventType.MESSAGE_DELETED,
                    thread.getId(),
                    event.occurredAt(),
                    new CommunityThreadRealtimePayload.MessageDeleted(
                        messages.get(memberId)
                    )
                )
            );
        });
    }

    void relay(ChatMessageReactionChangedEvent event) {
        delivery.activeThreadByChatRoomId(event.roomId()).ifPresent(thread -> {
            List<Long> recipients = delivery.activeRecipients(thread.getId());
            Map<Long, CommunityThreadMessageInfo> messages = delivery.messages(
                thread.getId(), recipients, event.messageId()
            );
            delivery.fanOutMembers(
                recipients,
                Operation.REACTION_CHANGED,
                memberId -> reactionEnvelope(event, thread, messages.get(memberId))
            );
        });
    }

    void relay(ChatReadUpdatedEvent event) {
        delivery.activeThreadByChatRoomId(event.roomId()).ifPresent(thread -> {
            List<Long> recipients = delivery.activeRecipients(thread.getId());
            CommunityThreadRealtimeEvent<CommunityThreadRealtimePayload.ReadUpdated> envelope =
                delivery.envelope(
                    event.eventId(),
                    CommunityThreadRealtimeEventType.READ_UPDATED,
                    thread.getId(),
                    event.occurredAt(),
                    new CommunityThreadRealtimePayload.ReadUpdated(
                        event.memberId(),
                        event.lastReadMessageId()
                    )
                );
            delivery.fanOutMembers(
                recipients,
                Operation.READ_UPDATED,
                ignored -> envelope
            );
        });
    }

    private CommunityThreadRealtimeEvent<CommunityThreadRealtimePayload.ReactionChanged> reactionEnvelope(
        ChatMessageReactionChangedEvent event,
        CommunityThread thread,
        CommunityThreadMessageInfo message
    ) {
        return delivery.envelope(
            event.eventId(),
            CommunityThreadRealtimeEventType.REACTION_CHANGED,
            thread.getId(),
            event.occurredAt(),
            new CommunityThreadRealtimePayload.ReactionChanged(
                event.messageId(),
                message.reactions()
            )
        );
    }
}
