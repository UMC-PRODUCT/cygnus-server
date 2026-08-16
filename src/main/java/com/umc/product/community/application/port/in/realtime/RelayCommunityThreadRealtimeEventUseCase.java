package com.umc.product.community.application.port.in.realtime;

import com.umc.product.chat.domain.event.ChatMessageCreatedEvent;
import com.umc.product.chat.domain.event.ChatMessageDeletedEvent;
import com.umc.product.chat.domain.event.ChatMessageReactionChangedEvent;
import com.umc.product.chat.domain.event.ChatMessageUpdatedEvent;
import com.umc.product.chat.domain.event.ChatReadUpdatedEvent;
import com.umc.product.community.domain.event.CommunityThreadDeletedEvent;
import com.umc.product.community.domain.event.CommunityThreadInvitedEvent;
import com.umc.product.community.domain.event.CommunityThreadMemberKickedEvent;
import com.umc.product.community.domain.event.CommunityThreadMemberLeftEvent;
import com.umc.product.community.domain.event.CommunityThreadUpdatedEvent;

public interface RelayCommunityThreadRealtimeEventUseCase {

    void relay(ChatMessageCreatedEvent event);

    void relay(ChatMessageUpdatedEvent event);

    void relay(ChatMessageDeletedEvent event);

    void relay(ChatMessageReactionChangedEvent event);

    void relay(ChatReadUpdatedEvent event);

    void relay(CommunityThreadInvitedEvent event);

    void relay(CommunityThreadUpdatedEvent event);

    void relay(CommunityThreadDeletedEvent event);

    void relay(CommunityThreadMemberKickedEvent event);

    void relay(CommunityThreadMemberLeftEvent event);
}
