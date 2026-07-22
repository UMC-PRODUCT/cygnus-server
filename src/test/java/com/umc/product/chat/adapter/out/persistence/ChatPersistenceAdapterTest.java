package com.umc.product.chat.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.chat.application.port.out.dto.RoomUnreadCount;
import com.umc.product.chat.domain.ChatMember;
import com.umc.product.chat.domain.ChatMessage;
import com.umc.product.chat.domain.ChatRoom;
import com.umc.product.chat.domain.MessageContentType;
import com.umc.product.chat.domain.exception.ChatDomainException;
import com.umc.product.chat.domain.exception.ChatErrorCode;

@ExtendWith(MockitoExtension.class)
@DisplayName("chat persistence adapter")
class ChatPersistenceAdapterTest {

    @Mock
    ChatRoomJpaRepository roomRepository;
    @Mock
    ChatMemberJpaRepository memberRepository;
    @Mock
    ChatMessageJpaRepository messageRepository;
    @Mock
    ChatMessageQueryRepository messageQueryRepository;
    @InjectMocks
    ChatRoomPersistenceAdapter roomAdapter;
    @InjectMocks
    ChatMessagePersistenceAdapter messageAdapter;

    @Test
    @DisplayName("채팅방 저장·삭제·일반/lock 조회를 repository에 위임한다")
    void room_crud() {
        ChatRoom room = room(1L);
        given(roomRepository.save(room)).willReturn(room);
        given(roomRepository.findById(1L)).willReturn(Optional.of(room));
        given(roomRepository.findByIdForUpdate(1L)).willReturn(Optional.of(room));

        assertThat(roomAdapter.save(room)).isSameAs(room);
        assertThat(roomAdapter.getById(1L)).isSameAs(room);
        assertThat(roomAdapter.getByIdForUpdate(1L)).isSameAs(room);
        roomAdapter.delete(room);

        then(roomRepository).should().delete(room);
    }

    @Test
    @DisplayName("일반/lock 조회에서 채팅방이 없으면 동일한 not-found 예외를 던진다")
    void room_not_found() {
        given(roomRepository.findById(1L)).willReturn(Optional.empty());
        given(roomRepository.findByIdForUpdate(1L)).willReturn(Optional.empty());

        assertChatError(() -> roomAdapter.getById(1L), ChatErrorCode.CHAT_ROOM_NOT_FOUND);
        assertChatError(() -> roomAdapter.getByIdForUpdate(1L), ChatErrorCode.CHAT_ROOM_NOT_FOUND);
    }

    @Test
    @DisplayName("채팅 멤버 저장·중복 방지·삭제·존재·목록·읽음 갱신을 repository에 위임한다")
    void member_operations() {
        ChatMember first = member(1L, 10L);
        ChatMember second = member(2L, 10L);
        given(memberRepository.save(first)).willReturn(first);
        given(memberRepository.insertIfAbsent(1L, 10L)).willReturn(1);
        given(memberRepository.insertIfAbsent(2L, 10L)).willReturn(0);
        given(memberRepository.existsByRoomIdAndMemberId(1L, 10L)).willReturn(true);
        given(memberRepository.findByRoomIdAndMemberId(1L, 10L)).willReturn(Optional.of(first));
        given(memberRepository.findAllByRoomId(1L)).willReturn(List.of(first));
        given(memberRepository.findAllByMemberIdAndRoomIdIn(10L, List.of(1L, 2L)))
            .willReturn(List.of(first, second));

        assertThat(roomAdapter.save(first)).isSameAs(first);
        assertThat(roomAdapter.saveIfAbsent(first)).isTrue();
        assertThat(roomAdapter.saveIfAbsent(second)).isFalse();
        assertThat(roomAdapter.existsByRoomIdAndMemberId(1L, 10L)).isTrue();
        assertThat(roomAdapter.getByRoomIdAndMemberId(1L, 10L)).isSameAs(first);
        assertThat(roomAdapter.listByRoomId(1L)).containsExactly(first);
        assertThat(roomAdapter.listRoomIdsByMemberIdAndRoomIdIn(10L, List.of(1L, 2L)))
            .containsExactly(1L, 2L);
        roomAdapter.delete(1L, 10L);
        roomAdapter.bumpLastReadMessageId(1L, 10L, 99L);

        then(memberRepository).should().deleteByRoomIdAndMemberId(1L, 10L);
        then(memberRepository).should().bumpLastReadMessageId(1L, 10L, 99L);
    }

    @Test
    @DisplayName("채팅 멤버가 없으면 not-found 예외를 던진다")
    void member_not_found() {
        given(memberRepository.findByRoomIdAndMemberId(1L, 10L)).willReturn(Optional.empty());

        assertChatError(
            () -> roomAdapter.getByRoomIdAndMemberId(1L, 10L),
            ChatErrorCode.CHAT_MEMBER_NOT_FOUND
        );
    }

    @Test
    @DisplayName("메시지 저장·조회·존재·cursor/unread 조회를 repository에 위임한다")
    void message_operations() {
        ChatMessage message = message(100L, 1L);
        List<ChatMessage> messages = List.of(message);
        List<RoomUnreadCount> unread = List.of(new RoomUnreadCount(1L, 2L));
        given(messageRepository.save(message)).willReturn(message);
        given(messageRepository.findById(100L)).willReturn(Optional.of(message));
        given(messageRepository.findByIdAndRoomId(100L, 1L)).willReturn(Optional.of(message));
        given(messageRepository.existsByIdAndRoomId(100L, 1L)).willReturn(true);
        given(messageQueryRepository.listByRoomId(1L, 90L, 20)).willReturn(messages);
        given(messageQueryRepository.listLatestPerRoom(List.of(1L))).willReturn(messages);
        given(messageQueryRepository.countUnreadByRooms(10L, List.of(1L))).willReturn(unread);

        assertThat(messageAdapter.save(message)).isSameAs(message);
        assertThat(messageAdapter.getById(100L)).isSameAs(message);
        assertThat(messageAdapter.getByIdAndRoomId(100L, 1L)).isSameAs(message);
        assertThat(messageAdapter.existsByIdAndRoomId(100L, 1L)).isTrue();
        assertThat(messageAdapter.listByRoomId(1L, 90L, 20)).isEqualTo(messages);
        assertThat(messageAdapter.listLatestPerRoom(List.of(1L))).isEqualTo(messages);
        assertThat(messageAdapter.countUnreadByRooms(10L, List.of(1L))).isEqualTo(unread);
    }

    @Test
    @DisplayName("ID 또는 방 조건으로 메시지가 없으면 동일한 not-found 예외를 던진다")
    void message_not_found() {
        given(messageRepository.findById(100L)).willReturn(Optional.empty());
        given(messageRepository.findByIdAndRoomId(100L, 1L)).willReturn(Optional.empty());

        assertChatError(() -> messageAdapter.getById(100L), ChatErrorCode.CHAT_MESSAGE_NOT_FOUND);
        assertChatError(
            () -> messageAdapter.getByIdAndRoomId(100L, 1L),
            ChatErrorCode.CHAT_MESSAGE_NOT_FOUND
        );
    }

    private void assertChatError(org.assertj.core.api.ThrowableAssert.ThrowingCallable callable, ChatErrorCode code) {
        assertThatThrownBy(callable)
            .isInstanceOf(ChatDomainException.class)
            .extracting(error -> ((ChatDomainException) error).getBaseCode())
            .isEqualTo(code);
    }

    private ChatRoom room(Long id) {
        ChatRoom room = ChatRoom.create();
        ReflectionTestUtils.setField(room, "id", id);
        return room;
    }

    private ChatMember member(Long roomId, Long memberId) {
        return ChatMember.of(roomId, memberId);
    }

    private ChatMessage message(Long id, Long roomId) {
        ChatMessage message = ChatMessage.create(
            roomId,
            10L,
            MessageContentType.TEXT,
            "message",
            null
        );
        ReflectionTestUtils.setField(message, "id", id);
        return message;
    }
}
