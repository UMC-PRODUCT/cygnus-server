package com.umc.product.chat.application.service.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.inOrder;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.chat.application.authorization.ChatPolicyAction;
import com.umc.product.chat.application.policy.ChatReactionPolicy;
import com.umc.product.chat.application.policy.ChatRoomAccessPolicy;
import com.umc.product.chat.application.port.in.command.dto.ChangeChatMessageReactionCommand;
import com.umc.product.chat.application.port.in.command.dto.ChatReactionMutationResult;
import com.umc.product.chat.application.port.out.LoadChatMessagePort;
import com.umc.product.chat.application.port.out.LoadChatMessageReactionPort;
import com.umc.product.chat.application.port.out.LoadChatRoomPort;
import com.umc.product.chat.application.port.out.SaveChatMessageReactionPort;
import com.umc.product.chat.application.port.out.dto.ChatReactionSummary;
import com.umc.product.chat.domain.ChatMessage;
import com.umc.product.chat.domain.MessageContentType;
import com.umc.product.chat.domain.event.ChatMessageReactionChangedEvent;
import com.umc.product.chat.domain.exception.ChatDomainException;
import com.umc.product.chat.domain.exception.ChatErrorCode;
import com.umc.product.global.event.application.port.out.DomainEventPublisher;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChatReactionCommandService")
class ChatReactionCommandServiceTest {

    @Mock
    LoadChatRoomPort loadChatRoomPort;
    @Mock
    LoadChatMessagePort loadChatMessagePort;
    @Mock
    LoadChatMessageReactionPort loadChatMessageReactionPort;
    @Mock
    SaveChatMessageReactionPort saveChatMessageReactionPort;
    @Mock
    ChatRoomAccessPolicy chatRoomAccessPolicy;
    @Mock
    ChatReactionPolicy chatReactionPolicy;
    @Mock
    DomainEventPublisher domainEventPublisher;

    @InjectMocks
    ChatReactionCommandService sut;

    @Test
    @DisplayName("room lock 아래에서 새 reaction을 추가하고 stable event를 발행한다")
    void add_fresh() {
        ChangeChatMessageReactionCommand command = command();
        ChatMessage message = message();
        given(loadChatMessagePort.getByIdAndRoomId(100L, 1L)).willReturn(message);
        given(saveChatMessageReactionPort.addIfAbsent(100L, 10L, "👍")).willReturn(true);
        given(loadChatMessageReactionPort.summarizeByMessageIds(List.of(100L), 10L))
            .willReturn(List.of(new ChatReactionSummary(100L, "👍", 1L, true)));

        ChatReactionMutationResult result = sut.add(command);

        assertThat(result.deduplicated()).isFalse();
        assertThat(result.reactions()).hasSize(1);
        assertThat(result.reactions().get(0).reactedByMe()).isTrue();
        ArgumentCaptor<ChatMessageReactionChangedEvent> event =
            ArgumentCaptor.forClass(ChatMessageReactionChangedEvent.class);
        then(domainEventPublisher).should().publish(event.capture());
        assertThat(event.getValue().eventId()).isNotNull();
        assertThat(event.getValue().added()).isTrue();

        InOrder order = inOrder(
            loadChatRoomPort,
            chatRoomAccessPolicy,
            loadChatMessagePort,
            saveChatMessageReactionPort
        );
        order.verify(loadChatRoomPort).getByIdForUpdate(1L);
        order.verify(chatRoomAccessPolicy)
            .verifyMember(ChatPolicyAction.REACTION_UPDATE, 1L, 10L);
        order.verify(loadChatMessagePort).getByIdAndRoomId(100L, 1L);
        order.verify(saveChatMessageReactionPort).addIfAbsent(100L, 10L, "👍");
    }

    @Test
    @DisplayName("중복 reaction 추가는 상태 idempotent no-op이다")
    void add_duplicate() {
        given(loadChatMessagePort.getByIdAndRoomId(100L, 1L)).willReturn(message());
        given(saveChatMessageReactionPort.addIfAbsent(100L, 10L, "👍")).willReturn(false);
        given(loadChatMessageReactionPort.summarizeByMessageIds(List.of(100L), 10L))
            .willReturn(List.of(new ChatReactionSummary(100L, "👍", 2L, true)));

        ChatReactionMutationResult result = sut.add(command());

        assertThat(result.deduplicated()).isTrue();
        then(domainEventPublisher).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("없는 reaction 제거는 상태 idempotent no-op이다")
    void remove_missing() {
        given(loadChatMessagePort.getByIdAndRoomId(100L, 1L)).willReturn(message());
        given(saveChatMessageReactionPort.remove(100L, 10L, "👍")).willReturn(false);
        given(loadChatMessageReactionPort.summarizeByMessageIds(List.of(100L), 10L))
            .willReturn(List.of());

        ChatReactionMutationResult result = sut.remove(command());

        assertThat(result.deduplicated()).isTrue();
        assertThat(result.reactions()).isEmpty();
        then(domainEventPublisher).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("SYSTEM tombstone에는 reaction을 추가할 수 없다")
    void add_tombstoneRejected() {
        ChatMessage message = message();
        message.tombstone();
        given(loadChatMessagePort.getByIdAndRoomId(100L, 1L)).willReturn(message);

        assertThatThrownBy(() -> sut.add(command()))
            .isInstanceOf(ChatDomainException.class)
            .extracting(error -> ((ChatDomainException) error).getBaseCode())
            .isEqualTo(ChatErrorCode.CHAT_MESSAGE_REACTION_NOT_ALLOWED);

        then(saveChatMessageReactionPort).shouldHaveNoInteractions();
        then(domainEventPublisher).shouldHaveNoInteractions();
    }

    private ChangeChatMessageReactionCommand command() {
        return new ChangeChatMessageReactionCommand(1L, 100L, 10L, "👍");
    }

    private ChatMessage message() {
        ChatMessage message = ChatMessage.create(1L, 20L, MessageContentType.TEXT, "본문", List.of());
        ReflectionTestUtils.setField(message, "id", 100L);
        return message;
    }
}
