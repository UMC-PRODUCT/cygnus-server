package com.umc.product.chat.application.service.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.times;

import com.umc.product.chat.application.policy.ChatRoomAccessPolicy;
import com.umc.product.chat.application.port.in.query.dto.ChatMessageCursorResult;
import com.umc.product.chat.application.port.in.query.dto.ChatRoomSummaryInfo;
import com.umc.product.chat.application.port.in.query.dto.GetChatMessagesQuery;
import com.umc.product.chat.application.port.out.LoadChatMessagePort;
import com.umc.product.chat.application.port.out.LoadChatRoomPort;
import com.umc.product.chat.application.port.out.dto.RoomUnreadCount;
import com.umc.product.chat.domain.ChatMessage;
import com.umc.product.chat.domain.ChatRoom;
import com.umc.product.chat.domain.exception.ChatDomainException;
import com.umc.product.chat.domain.exception.ChatErrorCode;
import com.umc.product.chat.domain.MessageContentType;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChatMessageQueryService")
class ChatMessageQueryServiceTest {

    @Mock
    LoadChatMessagePort loadChatMessagePort;
    @Mock
    LoadChatRoomPort loadChatRoomPort;
    @Mock
    ChatRoomAccessPolicy chatRoomAccessPolicy;

    @InjectMocks
    ChatMessageQueryService sut;

    @Test
    @DisplayName("size+1개가 조회되면 hasNext=true, 초과분을 잘라내고 마지막 id를 다음 커서로 반환한다")
    void getMessages_hasNext() {
        given(loadChatMessagePort.listByRoomId(eq(1L), eq(null), anyInt()))
            .willReturn(List.of(message(30L, 1L), message(20L, 1L), message(10L, 1L)));

        ChatMessageCursorResult result = sut.getMessages(new GetChatMessagesQuery(1L, 10L, null, 2));

        assertThat(result.hasNext()).isTrue();
        assertThat(result.content()).hasSize(2);
        assertThat(result.content()).extracting("messageId").containsExactly(30L, 20L);
        assertThat(result.nextCursor()).isEqualTo(20L);
    }

    @Test
    @DisplayName("size 이하로 조회되면 hasNext=false, nextCursor는 null이다")
    void getMessages_noNext() {
        given(loadChatMessagePort.listByRoomId(eq(1L), eq(null), anyInt()))
            .willReturn(List.of(message(30L, 1L), message(20L, 1L)));

        ChatMessageCursorResult result = sut.getMessages(new GetChatMessagesQuery(1L, 10L, null, 2));

        assertThat(result.hasNext()).isFalse();
        assertThat(result.content()).hasSize(2);
        assertThat(result.nextCursor()).isNull();
    }

    @Test
    @DisplayName("요청자가 방 멤버가 아니면 메시지를 조회하지 않고 접근 거부 예외를 던진다")
    void getMessages_accessDenied() {
        willThrow(new ChatDomainException(ChatErrorCode.CHAT_ROOM_ACCESS_DENIED))
            .given(chatRoomAccessPolicy).verifyMember(1L, 10L);

        assertThatThrownBy(() -> sut.getMessages(new GetChatMessagesQuery(1L, 10L, null, 2)))
            .isInstanceOf(ChatDomainException.class)
            .extracting(e -> ((ChatDomainException) e).getBaseCode())
            .isEqualTo(ChatErrorCode.CHAT_ROOM_ACCESS_DENIED);

        then(loadChatMessagePort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("속한 방이 없으면 빈 목록을 반환하고 메시지 포트를 호출하지 않는다")
    void getMyChatRooms_empty() {
        given(loadChatRoomPort.listByMemberId(10L)).willReturn(List.of());

        List<ChatRoomSummaryInfo> result = sut.getMyChatRooms(10L);

        assertThat(result).isEmpty();
        then(loadChatMessagePort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("방별 마지막 메시지와 안 읽은 수를 조립하고, 배치 쿼리를 각각 1회만 호출한다(N+1 없음)")
    void getMyChatRooms_assemble() {
        given(loadChatRoomPort.listByMemberId(10L))
            .willReturn(List.of(room(1L), room(2L), room(3L)));
        // room1: 마지막 메시지 100, room2: 마지막 메시지 90, room3: 메시지 없음
        given(loadChatMessagePort.listLatestPerRoom(List.of(1L, 2L, 3L)))
            .willReturn(List.of(message(100L, 1L), message(90L, 2L)));
        // room1만 안 읽은 메시지 5개
        given(loadChatMessagePort.countUnreadByRooms(10L, List.of(1L, 2L, 3L)))
            .willReturn(List.of(new RoomUnreadCount(1L, 5L)));

        List<ChatRoomSummaryInfo> result = sut.getMyChatRooms(10L);

        // 마지막 메시지 최신순 정렬: room1(100) > room2(90) > room3(없음)
        assertThat(result).extracting("roomId").containsExactly(1L, 2L, 3L);

        assertThat(result.get(0).lastMessage().messageId()).isEqualTo(100L);
        assertThat(result.get(0).unreadCount()).isEqualTo(5L);

        assertThat(result.get(1).lastMessage().messageId()).isEqualTo(90L);
        assertThat(result.get(1).unreadCount()).isZero();

        assertThat(result.get(2).lastMessage()).isNull();
        assertThat(result.get(2).unreadCount()).isZero();

        then(loadChatMessagePort).should(times(1)).listLatestPerRoom(List.of(1L, 2L, 3L));
        then(loadChatMessagePort).should(times(1)).countUnreadByRooms(10L, List.of(1L, 2L, 3L));
    }

    private ChatMessage message(Long id, Long roomId) {
        ChatMessage message = ChatMessage.create(roomId, 99L, MessageContentType.TEXT, "msg", null);
        ReflectionTestUtils.setField(message, "id", id);
        return message;
    }

    private ChatRoom room(Long id) {
        ChatRoom room = ChatRoom.create();
        ReflectionTestUtils.setField(room, "id", id);
        return room;
    }
}
