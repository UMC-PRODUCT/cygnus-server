package com.umc.product.chat.application.service.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;

import com.umc.product.chat.application.port.in.command.dto.MarkChatRoomReadCommand;
import com.umc.product.chat.application.port.in.command.dto.SendChatMessageCommand;
import com.umc.product.chat.application.port.in.query.dto.ChatMessageInfo;
import com.umc.product.chat.application.port.out.LoadChatMemberPort;
import com.umc.product.chat.application.port.out.LoadChatMessagePort;
import com.umc.product.chat.application.port.out.SaveChatMemberPort;
import com.umc.product.chat.application.port.out.SaveChatMessagePort;
import com.umc.product.chat.domain.ChatMember;
import com.umc.product.chat.domain.ChatMessage;
import com.umc.product.chat.domain.MessageContentType;
import com.umc.product.chat.application.policy.ChatRoomAccessPolicy;
import com.umc.product.chat.domain.event.ChatMessageCreatedEvent;
import com.umc.product.chat.domain.exception.ChatDomainException;
import com.umc.product.chat.domain.exception.ChatErrorCode;
import com.umc.product.global.event.application.port.out.DomainEventPublisher;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChatMessageCommandService")
class ChatMessageCommandServiceTest {

    @Mock
    SaveChatMessagePort saveChatMessagePort;
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
    ChatMessageCommandService sut;

    @Test
    @DisplayName("정상 전송 시 메시지를 저장하고 생성 이벤트를 발행한다")
    void send_success() {
        SendChatMessageCommand command =
            new SendChatMessageCommand(1L, 10L, MessageContentType.TEXT, "안녕", null);
        ChatMessage saved = ChatMessage.create(1L, 10L, MessageContentType.TEXT, "안녕", null);
        ReflectionTestUtils.setField(saved, "id", 100L);
        Instant createdAt = Instant.parse("2026-06-13T00:00:00Z");
        ReflectionTestUtils.setField(saved, "createdAt", createdAt);
        given(saveChatMessagePort.save(any(ChatMessage.class))).willReturn(saved);

        ChatMessageInfo result = sut.send(command);

        assertThat(result.messageId()).isEqualTo(100L);
        assertThat(result.roomId()).isEqualTo(1L);
        assertThat(result.content()).isEqualTo("안녕");

        then(saveChatMessagePort).should().save(any(ChatMessage.class));

        ArgumentCaptor<ChatMessageCreatedEvent> captor = ArgumentCaptor.forClass(ChatMessageCreatedEvent.class);
        then(domainEventPublisher).should().publish(captor.capture());
        assertThat(captor.getValue().messageId()).isEqualTo(100L);
        assertThat(captor.getValue().roomId()).isEqualTo(1L);
        assertThat(captor.getValue().senderMemberId()).isEqualTo(10L);
        // 이벤트 발생 시각은 메시지의 저장 시각(createdAt)과 일치해야 한다
        assertThat(captor.getValue().occurredAt()).isEqualTo(createdAt);
    }

    @Test
    @DisplayName("SYSTEM 타입은 전송할 수 없다")
    void send_systemRejected() {
        SendChatMessageCommand command =
            new SendChatMessageCommand(1L, 10L, MessageContentType.SYSTEM, "x", null);

        assertThatThrownBy(() -> sut.send(command))
            .isInstanceOf(ChatDomainException.class)
            .extracting(e -> ((ChatDomainException) e).getBaseCode())
            .isEqualTo(ChatErrorCode.CHAT_MESSAGE_INVALID_CONTENT_TYPE);

        then(saveChatMessagePort).shouldHaveNoInteractions();
        then(domainEventPublisher).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("내용과 첨부가 모두 비어 있으면 전송할 수 없다")
    void send_emptyRejected() {
        SendChatMessageCommand command =
            new SendChatMessageCommand(1L, 10L, MessageContentType.TEXT, "   ", List.of());

        assertThatThrownBy(() -> sut.send(command))
            .isInstanceOf(ChatDomainException.class)
            .extracting(e -> ((ChatDomainException) e).getBaseCode())
            .isEqualTo(ChatErrorCode.CHAT_MESSAGE_EMPTY);

        then(saveChatMessagePort).shouldHaveNoInteractions();
        then(domainEventPublisher).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("방 멤버가 아니면 전송할 수 없고 저장/발행하지 않는다")
    void send_notMember() {
        SendChatMessageCommand command =
            new SendChatMessageCommand(1L, 10L, MessageContentType.TEXT, "안녕", null);
        willThrow(new ChatDomainException(ChatErrorCode.CHAT_ROOM_ACCESS_DENIED))
            .given(chatRoomAccessPolicy).verifyMember(1L, 10L);

        assertThatThrownBy(() -> sut.send(command))
            .isInstanceOf(ChatDomainException.class)
            .extracting(e -> ((ChatDomainException) e).getBaseCode())
            .isEqualTo(ChatErrorCode.CHAT_ROOM_ACCESS_DENIED);

        then(saveChatMessagePort).shouldHaveNoInteractions();
        then(domainEventPublisher).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("읽음 처리 시 멤버의 읽음 위치를 갱신하고 저장한다")
    void markRead() {
        ChatMember member = ChatMember.of(1L, 10L);
        given(loadChatMemberPort.getByRoomIdAndMemberId(1L, 10L)).willReturn(member);
        given(loadChatMessagePort.findLatestMessageId(1L)).willReturn(Optional.of(42L));

        sut.markRead(MarkChatRoomReadCommand.of(1L, 10L));

        assertThat(member.getLastReadMessageId()).isEqualTo(42L);
        then(saveChatMemberPort).should().save(member);
    }
}
