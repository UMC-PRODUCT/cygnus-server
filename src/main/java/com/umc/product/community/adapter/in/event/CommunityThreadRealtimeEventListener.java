package com.umc.product.community.adapter.in.event;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.umc.product.chat.domain.event.ChatMessageCreatedEvent;
import com.umc.product.chat.domain.event.ChatMessageDeletedEvent;
import com.umc.product.chat.domain.event.ChatMessageReactionChangedEvent;
import com.umc.product.chat.domain.event.ChatMessageUpdatedEvent;
import com.umc.product.chat.domain.event.ChatReadUpdatedEvent;
import com.umc.product.community.application.port.in.realtime.RelayCommunityThreadRealtimeEventUseCase;
import com.umc.product.community.domain.event.CommunityThreadDeletedEvent;
import com.umc.product.community.domain.event.CommunityThreadInvitedEvent;
import com.umc.product.community.domain.event.CommunityThreadMemberKickedEvent;
import com.umc.product.community.domain.event.CommunityThreadMemberLeftEvent;
import com.umc.product.community.domain.event.CommunityThreadUpdatedEvent;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class CommunityThreadRealtimeEventListener {

    private final RelayCommunityThreadRealtimeEventUseCase relayUseCase;

    @EventListener
    public void onMessageCreated(ChatMessageCreatedEvent event) {
        relayUseCase.relay(event);
    }

    @EventListener
    public void onMessageUpdated(ChatMessageUpdatedEvent event) {
        relayUseCase.relay(event);
    }

    @EventListener
    public void onMessageDeleted(ChatMessageDeletedEvent event) {
        relayUseCase.relay(event);
    }

    @EventListener
    public void onReactionChanged(ChatMessageReactionChangedEvent event) {
        relayUseCase.relay(event);
    }

    @EventListener
    public void onReadUpdated(ChatReadUpdatedEvent event) {
        relayUseCase.relay(event);
    }

    @EventListener
    public void onThreadInvited(CommunityThreadInvitedEvent event) {
        relayUseCase.relay(event);
    }

    @EventListener
    public void onThreadUpdated(CommunityThreadUpdatedEvent event) {
        relayUseCase.relay(event);
    }

    @EventListener
    public void onThreadDeleted(CommunityThreadDeletedEvent event) {
        relayUseCase.relay(event);
    }

    @EventListener
    public void onMemberKicked(CommunityThreadMemberKickedEvent event) {
        relayUseCase.relay(event);
    }

    @EventListener
    public void onMemberLeft(CommunityThreadMemberLeftEvent event) {
        relayUseCase.relay(event);
    }
}
