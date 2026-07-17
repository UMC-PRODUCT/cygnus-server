package com.umc.product.chat.application.service.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.chat.application.port.in.command.dto.CreateChatRoomCommand;
import com.umc.product.chat.application.port.in.command.dto.PinChatRoomMessageCommand;
import com.umc.product.chat.application.port.out.LoadChatMessagePort;
import com.umc.product.chat.application.port.out.LoadChatRoomPort;
import com.umc.product.chat.application.port.out.SaveChatMemberPort;
import com.umc.product.chat.application.port.out.SaveChatRoomOwnershipPort;
import com.umc.product.chat.application.port.out.SaveChatRoomPort;
import com.umc.product.chat.application.service.ChatRoomOwnershipAccessService;
import com.umc.product.chat.domain.ChatMember;
import com.umc.product.chat.domain.ChatMessage;
import com.umc.product.chat.domain.ChatRoom;
import com.umc.product.chat.domain.ChatRoomActorContext;
import com.umc.product.chat.domain.ChatRoomOperation;
import com.umc.product.chat.domain.ChatRoomOwnerReference;
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
    SaveChatRoomOwnershipPort saveChatRoomOwnershipPort;
    @Mock
    ChatRoomOwnershipAccessService ownershipAccessService;
    @Mock
    DomainEventPublisher domainEventPublisher;

    @InjectMocks
    ChatRoomCommandService sut;

    @Test
    @DisplayName("authenticated standalone 생성은 room ID binding과 creator membership을 같은 흐름에 저장한다")
    void createStandalone() {
        ChatRoomActorContext actor = ChatRoomActorContext.actor(10L);
        ChatRoom room = room(1L);
        given(saveChatRoomPort.save(any(ChatRoom.class))).willReturn(room);

        var result = sut.create(new CreateChatRoomCommand(actor));

        assertThat(result.roomId()).isEqualTo(1L);
        assertThat(result.memberIds()).isEqualTo(List.of(10L));
        InOrder order = Mockito.inOrder(
            ownershipAccessService, saveChatRoomPort, saveChatRoomOwnershipPort, saveChatMemberPort);
        order.verify(ownershipAccessService).verifyAuthenticatedActor(actor);
        order.verify(saveChatRoomPort).save(any(ChatRoom.class));
        order.verify(saveChatRoomOwnershipPort).save(owner());
        order.verify(ownershipAccessService).verifyForUpdate(owner(), ChatRoomOperation.CREATE, actor);
        ArgumentCaptor<ChatMember> memberCaptor = ArgumentCaptor.forClass(ChatMember.class);
        order.verify(saveChatMemberPort).save(memberCaptor.capture());
        assertThat(memberCaptor.getValue().getRoomId()).isEqualTo(1L);
        assertThat(memberCaptor.getValue().getMemberId()).isEqualTo(10L);
    }

    @Test
    @DisplayName("인증 actor가 없으면 standalone room을 저장하기 전에 거부한다")
    void createUnauthenticated_deniedBeforeSave() {
        ChatRoomActorContext missingActor = ChatRoomActorContext.actor(null);
        willThrow(new ChatDomainException(ChatErrorCode.CHAT_ROOM_ACCESS_DENIED))
            .given(ownershipAccessService).verifyAuthenticatedActor(missingActor);

        assertThatThrownBy(() -> sut.create(new CreateChatRoomCommand(missingActor)))
            .isInstanceOf(ChatDomainException.class)
            .extracting(error -> ((ChatDomainException) error).getBaseCode())
            .isEqualTo(ChatErrorCode.CHAT_ROOM_ACCESS_DENIED);

        then(saveChatRoomPort).shouldHaveNoInteractions();
        then(saveChatRoomOwnershipPort).shouldHaveNoInteractions();
        then(saveChatMemberPort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("같은 방 메시지를 고정하고 채팅방을 저장한다")
    void pin() {
        ChatRoom room = room(1L);
        ChatMessage message = ChatMessage.create(1L, 10L, MessageContentType.TEXT, "공지", null);
        given(loadChatRoomPort.getById(1L)).willReturn(room);
        given(loadChatMessagePort.getByIdAndRoomId(100L, 1L)).willReturn(message);

        sut.pin(new PinChatRoomMessageCommand(owner(), actor(), 100L));

        assertThat(room.getPinnedMessageId()).isEqualTo(100L);
        then(ownershipAccessService).should().verifyForUpdate(owner(), ChatRoomOperation.PIN, actor());
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

        assertThatThrownBy(() -> sut.pin(new PinChatRoomMessageCommand(owner(), actor(), 100L)))
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

        sut.unpin(owner(), actor());

        assertThat(room.getPinnedMessageId()).isNull();
        then(ownershipAccessService).should().verifyForUpdate(owner(), ChatRoomOperation.UNPIN, actor());
        then(saveChatRoomPort).should().save(room);
        ArgumentCaptor<ChatRoomPinnedMessageChangedEvent> eventCaptor =
            ArgumentCaptor.forClass(ChatRoomPinnedMessageChangedEvent.class);
        then(domainEventPublisher).should().publish(eventCaptor.capture());
        assertThat(eventCaptor.getValue().roomId()).isEqualTo(1L);
        assertThat(eventCaptor.getValue().pinnedMessageId()).isNull();
    }

    @Test
    @DisplayName("standalone DELETE policy가 거부하면 방을 조회하거나 삭제하지 않는다")
    void delete_accessDenied() {
        willThrow(new ChatDomainException(ChatErrorCode.CHAT_ROOM_ACCESS_DENIED))
            .given(ownershipAccessService).verifyForUpdate(owner(), ChatRoomOperation.DELETE, actor());

        assertThatThrownBy(() -> sut.delete(owner(), actor()))
            .isInstanceOf(ChatDomainException.class)
            .extracting(error -> ((ChatDomainException) error).getBaseCode())
            .isEqualTo(ChatErrorCode.CHAT_ROOM_ACCESS_DENIED);

        then(loadChatRoomPort).shouldHaveNoInteractions();
        then(saveChatRoomPort).shouldHaveNoInteractions();
    }

    private ChatRoomOwnerReference owner() {
        return ChatRoomOwnerReference.standalone(1L);
    }

    private ChatRoomActorContext actor() {
        return ChatRoomActorContext.actor(10L);
    }

    private ChatRoom room(Long id) {
        ChatRoom room = ChatRoom.create();
        ReflectionTestUtils.setField(room, "id", id);
        return room;
    }
}
