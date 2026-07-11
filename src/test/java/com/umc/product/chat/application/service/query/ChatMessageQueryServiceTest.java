package com.umc.product.chat.application.service.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.times;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.chat.application.policy.ChatRoomAccessPolicy;
import com.umc.product.chat.application.port.in.query.dto.ChatMessageCursorResult;
import com.umc.product.chat.application.port.in.query.dto.ChatMessageReadStatusInfo;
import com.umc.product.chat.application.port.in.query.dto.ChatRoomSummaryInfo;
import com.umc.product.chat.application.port.in.query.dto.CheckChatMessageReadQuery;
import com.umc.product.chat.application.port.in.query.dto.GetChatMessagesQuery;
import com.umc.product.chat.application.port.out.LoadChatMemberPort;
import com.umc.product.chat.application.port.out.LoadChatMessagePort;
import com.umc.product.chat.application.port.out.dto.RoomUnreadCount;
import com.umc.product.chat.domain.ChatMember;
import com.umc.product.chat.domain.ChatMessage;
import com.umc.product.chat.domain.MessageContentType;
import com.umc.product.chat.domain.exception.ChatDomainException;
import com.umc.product.chat.domain.exception.ChatErrorCode;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChatMessageQueryService")
class ChatMessageQueryServiceTest {

    @Mock
    LoadChatMessagePort loadChatMessagePort;
    @Mock
    LoadChatMemberPort loadChatMemberPort;
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
    @DisplayName("메시지 조회 결과에 답장 대상 메시지 id를 포함한다")
    void getMessages_replyToMessageId() {
        given(loadChatMessagePort.listByRoomId(eq(1L), eq(null), anyInt()))
            .willReturn(List.of(message(30L, 1L, 20L)));

        ChatMessageCursorResult result = sut.getMessages(new GetChatMessagesQuery(1L, 10L, null, 2));

        assertThat(result.content()).hasSize(1);
        assertThat(result.content().get(0).replyToMessageId()).isEqualTo(20L);
    }

    @Test
    @DisplayName("대상 멤버의 lastReadMessageId가 메시지 id 이상이면 읽음으로 반환한다")
    void checkRead_read() {
        ChatMember targetMember = ChatMember.of(1L, 20L);
        targetMember.markRead(30L);
        given(loadChatMessagePort.getByIdAndRoomId(20L, 1L)).willReturn(messageFrom(20L, 1L, 99L));
        given(loadChatMemberPort.getByRoomIdAndMemberId(1L, 20L)).willReturn(targetMember);

        ChatMessageReadStatusInfo result =
            sut.checkRead(new CheckChatMessageReadQuery(1L, 20L, 10L, 20L));

        assertThat(result.roomId()).isEqualTo(1L);
        assertThat(result.messageId()).isEqualTo(20L);
        assertThat(result.targetMemberId()).isEqualTo(20L);
        assertThat(result.read()).isTrue();
    }

    @Test
    @DisplayName("대상 멤버의 lastReadMessageId가 null이면 안 읽음으로 반환한다")
    void checkRead_unread_nullLastRead() {
        ChatMember targetMember = ChatMember.of(1L, 20L);
        given(loadChatMessagePort.getByIdAndRoomId(20L, 1L)).willReturn(messageFrom(20L, 1L, 99L));
        given(loadChatMemberPort.getByRoomIdAndMemberId(1L, 20L)).willReturn(targetMember);

        ChatMessageReadStatusInfo result =
            sut.checkRead(new CheckChatMessageReadQuery(1L, 20L, 10L, 20L));

        assertThat(result.read()).isFalse();
    }

    @Test
    @DisplayName("대상 멤버의 lastReadMessageId가 메시지 id보다 작으면 안 읽음으로 반환한다")
    void checkRead_unread_lowerLastRead() {
        ChatMember targetMember = ChatMember.of(1L, 20L);
        targetMember.markRead(19L);
        given(loadChatMessagePort.getByIdAndRoomId(20L, 1L)).willReturn(messageFrom(20L, 1L, 99L));
        given(loadChatMemberPort.getByRoomIdAndMemberId(1L, 20L)).willReturn(targetMember);

        ChatMessageReadStatusInfo result =
            sut.checkRead(new CheckChatMessageReadQuery(1L, 20L, 10L, 20L));

        assertThat(result.read()).isFalse();
    }

    @Test
    @DisplayName("대상 멤버가 보낸 메시지는 읽음으로 반환한다")
    void checkRead_senderIsTarget_read() {
        ChatMember targetMember = ChatMember.of(1L, 20L);
        given(loadChatMessagePort.getByIdAndRoomId(20L, 1L)).willReturn(messageFrom(20L, 1L, 20L));
        given(loadChatMemberPort.getByRoomIdAndMemberId(1L, 20L)).willReturn(targetMember);

        ChatMessageReadStatusInfo result =
            sut.checkRead(new CheckChatMessageReadQuery(1L, 20L, 10L, 20L));

        assertThat(result.read()).isTrue();
    }

    @Test
    @DisplayName("요청자가 방 멤버가 아니면 읽음 여부를 조회하지 않고 접근 거부 예외를 던진다")
    void checkRead_requesterAccessDenied() {
        willThrow(new ChatDomainException(ChatErrorCode.CHAT_ROOM_ACCESS_DENIED))
            .given(chatRoomAccessPolicy).verifyMember(1L, 10L);

        assertThatThrownBy(() -> sut.checkRead(new CheckChatMessageReadQuery(1L, 20L, 10L, 30L)))
            .isInstanceOf(ChatDomainException.class)
            .extracting(e -> ((ChatDomainException) e).getBaseCode())
            .isEqualTo(ChatErrorCode.CHAT_ROOM_ACCESS_DENIED);

        then(loadChatMessagePort).shouldHaveNoInteractions();
        then(loadChatMemberPort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("메시지가 없거나 다른 방 메시지이면 읽음 여부를 조회하지 않고 메시지 없음 예외를 던진다")
    void checkRead_messageNotFound() {
        given(loadChatMessagePort.getByIdAndRoomId(20L, 1L))
            .willThrow(new ChatDomainException(ChatErrorCode.CHAT_MESSAGE_NOT_FOUND));

        assertThatThrownBy(() -> sut.checkRead(new CheckChatMessageReadQuery(1L, 20L, 10L, 30L)))
            .isInstanceOf(ChatDomainException.class)
            .extracting(e -> ((ChatDomainException) e).getBaseCode())
            .isEqualTo(ChatErrorCode.CHAT_MESSAGE_NOT_FOUND);

        then(loadChatMemberPort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("대상 멤버가 방 멤버가 아니면 멤버 없음 예외를 던진다")
    void checkRead_targetMemberNotFound() {
        given(loadChatMessagePort.getByIdAndRoomId(20L, 1L)).willReturn(messageFrom(20L, 1L, 99L));
        given(loadChatMemberPort.getByRoomIdAndMemberId(1L, 30L))
            .willThrow(new ChatDomainException(ChatErrorCode.CHAT_MEMBER_NOT_FOUND));

        assertThatThrownBy(() -> sut.checkRead(new CheckChatMessageReadQuery(1L, 20L, 10L, 30L)))
            .isInstanceOf(ChatDomainException.class)
            .extracting(e -> ((ChatDomainException) e).getBaseCode())
            .isEqualTo(ChatErrorCode.CHAT_MEMBER_NOT_FOUND);
    }

    @Test
    @DisplayName("roomId 집합이 비어 있으면 어떤 포트도 호출하지 않고 빈 목록을 반환한다")
    void listRoomSummaries_emptyInput() {
        List<ChatRoomSummaryInfo> result = sut.listRoomSummaries(10L, List.of());

        assertThat(result).isEmpty();
        then(loadChatMemberPort).shouldHaveNoInteractions();
        then(loadChatMessagePort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("전달한 roomId 중 참여 중인 방이 없으면 빈 목록을 반환하고 메시지 포트를 호출하지 않는다")
    void listRoomSummaries_noMembership_empty() {
        given(loadChatMemberPort.listRoomIdsByMemberIdAndRoomIdIn(10L, List.of(1L, 2L)))
            .willReturn(List.of());

        List<ChatRoomSummaryInfo> result = sut.listRoomSummaries(10L, List.of(1L, 2L));

        assertThat(result).isEmpty();
        then(loadChatMessagePort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("참여 중인 방만 마지막 메시지와 안 읽은 수를 조립하고, 배치 쿼리를 각각 1회만 호출한다(N+1 없음)")
    void listRoomSummaries_assemble() {
        List<Long> ownedRoomIds = List.of(1L, 2L, 3L);
        given(loadChatMemberPort.listRoomIdsByMemberIdAndRoomIdIn(10L, ownedRoomIds))
            .willReturn(List.of(1L, 2L, 3L));
        // room1: 마지막 메시지 100, room2: 마지막 메시지 90, room3: 메시지 없음
        given(loadChatMessagePort.listLatestPerRoom(List.of(1L, 2L, 3L)))
            .willReturn(List.of(message(100L, 1L), message(90L, 2L)));
        // room1만 안 읽은 메시지 5개
        given(loadChatMessagePort.countUnreadByRooms(10L, List.of(1L, 2L, 3L)))
            .willReturn(List.of(new RoomUnreadCount(1L, 5L)));

        List<ChatRoomSummaryInfo> result = sut.listRoomSummaries(10L, ownedRoomIds);

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

    @Test
    @DisplayName("소비 도메인이 소유한 roomId만 넘기면 다른 소비 도메인의 방은 결과·쿼리에 섞이지 않는다(도메인 간 격리)")
    void listRoomSummaries_isolatesConsumerScopes() {
        // 멤버 10은 inquiry 방(1)과 community 방(99)에 모두 참여하지만, inquiry consumer 는 자기 방(1)만 넘긴다.
        List<Long> inquiryOwnedRoomIds = List.of(1L);
        given(loadChatMemberPort.listRoomIdsByMemberIdAndRoomIdIn(10L, inquiryOwnedRoomIds))
            .willReturn(List.of(1L));
        given(loadChatMessagePort.listLatestPerRoom(List.of(1L)))
            .willReturn(List.of(message(100L, 1L)));
        given(loadChatMessagePort.countUnreadByRooms(10L, List.of(1L)))
            .willReturn(List.of());

        List<ChatRoomSummaryInfo> result = sut.listRoomSummaries(10L, inquiryOwnedRoomIds);

        // community 방(99)은 결과에도, 어떤 배치 쿼리에도 등장하지 않는다.
        assertThat(result).extracting("roomId").containsExactly(1L);
        then(loadChatMessagePort).should(times(1)).listLatestPerRoom(List.of(1L));
        then(loadChatMessagePort).should(times(1)).countUnreadByRooms(10L, List.of(1L));
    }

    private ChatMessage message(Long id, Long roomId) {
        ChatMessage message = ChatMessage.create(roomId, 99L, MessageContentType.TEXT, "msg", null);
        ReflectionTestUtils.setField(message, "id", id);
        return message;
    }

    private ChatMessage message(Long id, Long roomId, Long replyToMessageId) {
        ChatMessage message = ChatMessage.create(roomId, 99L, MessageContentType.TEXT, "msg", null, replyToMessageId);
        ReflectionTestUtils.setField(message, "id", id);
        return message;
    }

    private ChatMessage messageFrom(Long id, Long roomId, Long senderMemberId) {
        ChatMessage message = ChatMessage.create(roomId, senderMemberId, MessageContentType.TEXT, "msg", null);
        ReflectionTestUtils.setField(message, "id", id);
        return message;
    }
}
