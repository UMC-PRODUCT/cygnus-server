package com.umc.product.chat.application.service.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.chat.application.port.in.command.dto.CreateChatRoomCommand;
import com.umc.product.chat.application.port.in.command.dto.PinChatRoomMessageCommand;
import com.umc.product.chat.application.port.out.LoadChatMessagePort;
import com.umc.product.chat.application.port.out.LoadChatRoomPort;
import com.umc.product.chat.application.port.out.SaveChatMemberPort;
import com.umc.product.chat.application.port.out.SaveChatRoomPort;
import com.umc.product.chat.domain.ChatMessage;
import com.umc.product.chat.domain.ChatRoom;
import com.umc.product.chat.domain.ChatRoomReadScope;
import com.umc.product.chat.domain.MessageContentType;
import com.umc.product.chat.domain.event.ChatRoomPinnedMessageChangedEvent;
import com.umc.product.chat.domain.exception.ChatDomainException;
import com.umc.product.chat.domain.exception.ChatErrorCode;
import com.umc.product.global.event.application.port.out.DomainEventPublisher;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChatRoomCommandService")
class ChatRoomCommandServiceTest {

    @Mock
    SaveChatRoomPort saveChatRoomPort;
    @Mock
    LoadChatRoomPort loadChatRoomPort;
    @Mock
    LoadChatMessagePort loadChatMessagePort;
    @Mock
    SaveChatMemberPort saveChatMemberPort;
    @Mock
    DomainEventPublisher domainEventPublisher;

    @InjectMocks
    ChatRoomCommandService sut;

    @Test
    @DisplayName("같은 방 메시지를 고정하고 채팅방을 저장한다")
    void pin() {
        ChatRoom room = room(1L);
        ChatMessage message = ChatMessage.create(1L, 10L, MessageContentType.TEXT, "공지", null);
        given(loadChatRoomPort.getById(1L)).willReturn(room);
        given(loadChatMessagePort.getByIdAndRoomId(100L, 1L)).willReturn(message);

        sut.pin(new PinChatRoomMessageCommand(1L, 100L));

        assertThat(room.getPinnedMessageId()).isEqualTo(100L);
        then(saveChatRoomPort).should().save(room);
        ArgumentCaptor<ChatRoomPinnedMessageChangedEvent> eventCaptor =
            ArgumentCaptor.forClass(ChatRoomPinnedMessageChangedEvent.class);
        then(domainEventPublisher).should().publish(eventCaptor.capture());
        assertThat(eventCaptor.getValue().roomId()).isEqualTo(1L);
        assertThat(eventCaptor.getValue().pinnedMessageId()).isEqualTo(100L);
        assertThat(eventCaptor.getValue().eventType()).isEqualTo("chat.room.pinned-message.changed");
    }

    @Test
    @DisplayName("다른 방에 속하거나 없는 메시지는 고정할 수 없다")
    void pin_messageNotFound() {
        ChatRoom room = room(1L);
        given(loadChatRoomPort.getById(1L)).willReturn(room);
        willThrow(new ChatDomainException(ChatErrorCode.CHAT_MESSAGE_NOT_FOUND))
            .given(loadChatMessagePort).getByIdAndRoomId(100L, 1L);

        assertThatThrownBy(() -> sut.pin(new PinChatRoomMessageCommand(1L, 100L)))
            .isInstanceOf(ChatDomainException.class)
            .extracting(e -> ((ChatDomainException) e).getBaseCode())
            .isEqualTo(ChatErrorCode.CHAT_MESSAGE_NOT_FOUND);

        then(saveChatRoomPort).shouldHaveNoInteractions();
        then(domainEventPublisher).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("고정 메시지를 해제하고 채팅방을 저장한다")
    void unpin() {
        ChatRoom room = room(1L);
        room.pinMessage(100L);
        given(loadChatRoomPort.getById(1L)).willReturn(room);

        sut.unpin(1L);

        assertThat(room.getPinnedMessageId()).isNull();
        then(saveChatRoomPort).should().save(room);
        ArgumentCaptor<ChatRoomPinnedMessageChangedEvent> eventCaptor =
            ArgumentCaptor.forClass(ChatRoomPinnedMessageChangedEvent.class);
        then(domainEventPublisher).should().publish(eventCaptor.capture());
        assertThat(eventCaptor.getValue().roomId()).isEqualTo(1L);
        assertThat(eventCaptor.getValue().pinnedMessageId()).isNull();
    }

    @Test
    @DisplayName("조회 범위를 명시하지 않으면 멤버 전용 방으로 만든다")
    void create_defaultsToMemberOnly() {
        given(saveChatRoomPort.save(any(ChatRoom.class))).willAnswer(invocation -> {
            ChatRoom saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 1L);
            return saved;
        });

        sut.create(CreateChatRoomCommand.from(10L));

        assertThat(capturedRoom().getReadScope()).isEqualTo(ChatRoomReadScope.MEMBER_ONLY);
    }

    @Test
    @DisplayName("소비 도메인이 요청한 조회 범위를 방에 그대로 저장한다")
    void create_appliesRequestedReadScope() {
        given(saveChatRoomPort.save(any(ChatRoom.class))).willAnswer(invocation -> {
            ChatRoom saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 1L);
            return saved;
        });

        sut.create(CreateChatRoomCommand.of(10L, ChatRoomReadScope.PUBLIC));

        assertThat(capturedRoom().getReadScope()).isEqualTo(ChatRoomReadScope.PUBLIC);
    }

    private ChatRoom capturedRoom() {
        ArgumentCaptor<ChatRoom> captor = ArgumentCaptor.forClass(ChatRoom.class);
        then(saveChatRoomPort).should().save(captor.capture());
        return captor.getValue();
    }

    private ChatRoom room(Long id) {
        ChatRoom room = ChatRoom.create();
        ReflectionTestUtils.setField(room, "id", id);
        return room;
    }
}
