package com.umc.product.chat.application.service.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.inOrder;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.umc.product.chat.application.policy.ChatRoomAccessPolicy;
import com.umc.product.chat.application.port.in.command.dto.ChatReadMutationResult;
import com.umc.product.chat.application.port.in.command.dto.UpdateChatReadCommand;
import com.umc.product.chat.application.port.out.LoadChatMemberPort;
import com.umc.product.chat.application.port.out.LoadChatMessagePort;
import com.umc.product.chat.application.port.out.LoadChatRoomPort;
import com.umc.product.chat.application.port.out.SaveChatMemberPort;
import com.umc.product.chat.domain.ChatMember;
import com.umc.product.chat.domain.event.ChatReadUpdatedEvent;
import com.umc.product.global.event.application.port.out.DomainEventPublisher;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChatReadCommandService")
class ChatReadCommandServiceTest {

    @Mock
    LoadChatRoomPort loadChatRoomPort;
    @Mock
    LoadChatMessagePort loadChatMessagePort;
    @Mock
    LoadChatMemberPort loadChatMemberPort;
    @Mock
    SaveChatMemberPort saveChatMemberPort;
    @Mock
    ChatRoomAccessPolicy chatRoomAccessPolicy;
    @Mock
    DomainEventPublisher domainEventPublisher;

    @InjectMocks
    ChatReadCommandService sut;

    @Test
    @DisplayName("room lock 아래에서 exact message와 membership을 확인하고 watermark를 전진시킨다")
    void update_advances() {
        ChatMember member = ChatMember.of(1L, 10L);
        member.markRead(30L);
        given(loadChatMemberPort.getByRoomIdAndMemberId(1L, 10L)).willReturn(member);

        ChatReadMutationResult result = sut.update(new UpdateChatReadCommand(1L, 10L, 40L));

        assertThat(result.deduplicated()).isFalse();
        assertThat(result.lastReadMessageId()).isEqualTo(40L);
        then(saveChatMemberPort).should().bumpLastReadMessageId(1L, 10L, 40L);
        ArgumentCaptor<ChatReadUpdatedEvent> event = ArgumentCaptor.forClass(ChatReadUpdatedEvent.class);
        then(domainEventPublisher).should().publish(event.capture());
        assertThat(event.getValue().eventId()).isNotNull();
        assertThat(event.getValue().lastReadMessageId()).isEqualTo(40L);

        InOrder order = inOrder(
            loadChatRoomPort,
            chatRoomAccessPolicy,
            loadChatMessagePort,
            loadChatMemberPort,
            saveChatMemberPort
        );
        order.verify(loadChatRoomPort).getByIdForUpdate(1L);
        order.verify(chatRoomAccessPolicy).verifyMember(1L, 10L);
        order.verify(loadChatMessagePort).getByIdAndRoomId(40L, 1L);
        order.verify(loadChatMemberPort).getByRoomIdAndMemberId(1L, 10L);
        order.verify(saveChatMemberPort).bumpLastReadMessageId(1L, 10L, 40L);
    }

    @Test
    @DisplayName("낮거나 같은 watermark 재시도는 현재 exact watermark를 반환하는 no-op이다")
    void update_nonAdvancingDeduplicated() {
        ChatMember member = ChatMember.of(1L, 10L);
        member.markRead(50L);
        given(loadChatMemberPort.getByRoomIdAndMemberId(1L, 10L)).willReturn(member);

        ChatReadMutationResult result = sut.update(new UpdateChatReadCommand(1L, 10L, 40L));

        assertThat(result.deduplicated()).isTrue();
        assertThat(result.lastReadMessageId()).isEqualTo(50L);
        then(saveChatMemberPort).shouldHaveNoInteractions();
        then(domainEventPublisher).shouldHaveNoInteractions();
    }
}
