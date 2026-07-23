package com.umc.product.community.application.service.realtime;

import org.springframework.stereotype.Service;

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

@Service
@RequiredArgsConstructor
public class CommunityThreadRealtimeFanOutService implements RelayCommunityThreadRealtimeEventUseCase {

    private final CommunityThreadChatRealtimeRelay chatRelay;
    private final CommunityThreadLifecycleRealtimeRelay lifecycleRelay;

    @Override
    public void relay(ChatMessageCreatedEvent event) {
        chatRelay.relay(event);
    }

    @Override
    public void relay(ChatMessageUpdatedEvent event) {
        chatRelay.relay(event);
    }

    @Override
    public void relay(ChatMessageDeletedEvent event) {
        chatRelay.relay(event);
    }

    @Override
    public void relay(ChatMessageReactionChangedEvent event) {
        chatRelay.relay(event);
    }

    @Override
    public void relay(ChatReadUpdatedEvent event) {
        chatRelay.relay(event);
    }

    @Override
    public void relay(CommunityThreadInvitedEvent event) {
        lifecycleRelay.relay(event);
    }

    @Override
    public void relay(CommunityThreadUpdatedEvent event) {
        lifecycleRelay.relay(event);
    }

    @Override
    public void relay(CommunityThreadDeletedEvent event) {
        lifecycleRelay.relay(event);
    }

    @Override
    public void relay(CommunityThreadMemberKickedEvent event) {
        lifecycleRelay.relay(event);
    }

    @Override
    public void relay(CommunityThreadMemberLeftEvent event) {
        lifecycleRelay.relay(event);
    }
}
