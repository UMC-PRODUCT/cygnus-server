package com.umc.product.community.application.service.realtime;

import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

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

@DisplayName("Community thread realtime fan-out facade")
class CommunityThreadRealtimeFanOutServiceTest {

    @Test
    @DisplayName("모든 Chat·Community event 유형을 전용 relay에 그대로 위임한다")
    void relay_delegatesEverySupportedEventType() {
        CommunityThreadChatRealtimeRelay chatRelay = mock(CommunityThreadChatRealtimeRelay.class);
        CommunityThreadLifecycleRealtimeRelay lifecycleRelay =
            mock(CommunityThreadLifecycleRealtimeRelay.class);
        CommunityThreadRealtimeFanOutService sut = new CommunityThreadRealtimeFanOutService(
            chatRelay,
            lifecycleRelay
        );
        ChatMessageCreatedEvent created = mock(ChatMessageCreatedEvent.class);
        ChatMessageUpdatedEvent updated = mock(ChatMessageUpdatedEvent.class);
        ChatMessageDeletedEvent deleted = mock(ChatMessageDeletedEvent.class);
        ChatMessageReactionChangedEvent reactionChanged =
            mock(ChatMessageReactionChangedEvent.class);
        ChatReadUpdatedEvent readUpdated = mock(ChatReadUpdatedEvent.class);
        CommunityThreadInvitedEvent invited = mock(CommunityThreadInvitedEvent.class);
        CommunityThreadUpdatedEvent threadUpdated = mock(CommunityThreadUpdatedEvent.class);
        CommunityThreadDeletedEvent threadDeleted = mock(CommunityThreadDeletedEvent.class);
        CommunityThreadMemberKickedEvent kicked = mock(CommunityThreadMemberKickedEvent.class);
        CommunityThreadMemberLeftEvent left = mock(CommunityThreadMemberLeftEvent.class);

        sut.relay(created);
        sut.relay(updated);
        sut.relay(deleted);
        sut.relay(reactionChanged);
        sut.relay(readUpdated);
        sut.relay(invited);
        sut.relay(threadUpdated);
        sut.relay(threadDeleted);
        sut.relay(kicked);
        sut.relay(left);

        then(chatRelay).should().relay(created);
        then(chatRelay).should().relay(updated);
        then(chatRelay).should().relay(deleted);
        then(chatRelay).should().relay(reactionChanged);
        then(chatRelay).should().relay(readUpdated);
        then(lifecycleRelay).should().relay(invited);
        then(lifecycleRelay).should().relay(threadUpdated);
        then(lifecycleRelay).should().relay(threadDeleted);
        then(lifecycleRelay).should().relay(kicked);
        then(lifecycleRelay).should().relay(left);
    }
}
