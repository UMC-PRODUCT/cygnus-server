package com.umc.product.chat.application.service.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;

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
import com.umc.product.chat.application.port.in.query.dto.ChatMessageInfo;
import com.umc.product.chat.application.port.in.query.dto.ChatMessageReadStatusInfo;
import com.umc.product.chat.application.port.in.query.dto.CheckChatMessageReadQuery;
import com.umc.product.chat.application.port.in.query.dto.GetChatMessageQuery;
import com.umc.product.chat.application.port.in.query.dto.GetChatMessagesForAuthorizedCallerQuery;
import com.umc.product.chat.application.port.in.query.dto.GetChatMessagesQuery;
import com.umc.product.chat.application.port.out.LoadChatMemberPort;
import com.umc.product.chat.application.port.out.LoadChatMessagePort;
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
    @Mock
    ChatMessageInfoAssembler chatMessageInfoAssembler;

    @InjectMocks
    ChatMessageQueryService sut;

    @Test
    @DisplayName("size+1개가 조회되면 hasNext=true, 초과분을 잘라내고 마지막 id를 다음 커서로 반환한다")
    void getMessages_hasNext() {
        givenAssembledMessages();
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
        givenAssembledMessages();
        given(loadChatMessagePort.listByRoomId(eq(1L), eq(null), anyInt()))
            .willReturn(List.of(message(30L, 1L), message(20L, 1L)));

        ChatMessageCursorResult result = sut.getMessages(new GetChatMessagesQuery(1L, 10L, null, 2));

        assertThat(result.hasNext()).isFalse();
        assertThat(result.content()).hasSize(2);
        assertThat(result.nextCursor()).isNull();
    }

    @Test
    @DisplayName("요청자가 방을 읽을 수 없으면 메시지를 조회하지 않고 접근 거부 예외를 던진다")
    void getMessages_accessDenied() {
        willThrow(new ChatDomainException(ChatErrorCode.CHAT_ROOM_ACCESS_DENIED))
            .given(chatRoomAccessPolicy).verifyReadable(1L, 10L);

        assertThatThrownBy(() -> sut.getMessages(new GetChatMessagesQuery(1L, 10L, null, 2)))
            .isInstanceOf(ChatDomainException.class)
            .extracting(e -> ((ChatDomainException) e).getBaseCode())
            .isEqualTo(ChatErrorCode.CHAT_ROOM_ACCESS_DENIED);

        then(loadChatMessagePort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("getMessages(GetChatMessagesQuery) 경로는 여전히 membership을 검사한다(회귀 없음)")
    void getMessages_stillVerifiesMembership_regression() {
        sut.getMessages(new GetChatMessagesQuery(1L, 10L, null, 2));

        then(chatRoomAccessPolicy).should().verifyMember(1L, 10L);
    }

    @Test
    @DisplayName("권한위임형 조회(getMessages(GetChatMessagesForAuthorizedCallerQuery))는 membership 검사를 하지 않는다")
    void getMessagesForAuthorizedCaller_skipsMembershipCheck() {
        given(loadChatMessagePort.listByRoomId(eq(1L), eq(null), anyInt()))
            .willReturn(List.of(message(30L, 1L), message(20L, 1L)));

        ChatMessageCursorResult result =
            sut.getMessages(new GetChatMessagesForAuthorizedCallerQuery(1L, null, 2));

        assertThat(result.content()).hasSize(2);
        then(chatRoomAccessPolicy).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("메시지 조회 결과에 답장 대상 메시지 id를 포함한다")
    void getMessages_replyToMessageId() {
        givenAssembledMessages();
        given(loadChatMessagePort.listByRoomId(eq(1L), eq(null), anyInt()))
            .willReturn(List.of(message(30L, 1L, 20L)));

        ChatMessageCursorResult result = sut.getMessages(new GetChatMessagesQuery(1L, 10L, null, 2));

        assertThat(result.content()).hasSize(1);
        assertThat(result.content().get(0).replyToMessageId()).isEqualTo(20L);
    }

    @Test
    @DisplayName("단건 조회도 방 읽기 권한 검증 뒤 enriched 메시지를 반환한다")
    void getMessage_success() {
        ChatMessage message = message(30L, 1L, 20L);
        ChatMessageInfo info = ChatMessageInfo.from(message);
        given(loadChatMessagePort.getByIdAndRoomId(30L, 1L)).willReturn(message);
        given(chatMessageInfoAssembler.assemble(message, 10L)).willReturn(info);

        ChatMessageInfo result = sut.getMessage(new GetChatMessageQuery(1L, 10L, 30L));

        assertThat(result).isSameAs(info);
        then(chatRoomAccessPolicy).should().verifyReadable(1L, 10L);
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
    @DisplayName("대상 멤버가 보낸 메시지도 lastReadMessageId가 null이면 안 읽음으로 반환한다")
    void checkRead_senderIsTarget_unreadWhenLastReadIsNull() {
        ChatMember targetMember = ChatMember.of(1L, 20L);
        given(loadChatMessagePort.getByIdAndRoomId(20L, 1L)).willReturn(messageFrom(20L, 1L, 20L));
        given(loadChatMemberPort.getByRoomIdAndMemberId(1L, 20L)).willReturn(targetMember);

        ChatMessageReadStatusInfo result =
            sut.checkRead(new CheckChatMessageReadQuery(1L, 20L, 10L, 20L));

        assertThat(result.read()).isFalse();
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

    private void givenAssembledMessages() {
        given(chatMessageInfoAssembler.assemble(anyList(), eq(10L)))
            .willAnswer(invocation -> invocation.<List<ChatMessage>>getArgument(0).stream()
                .map(ChatMessageInfo::from)
                .toList());
    }
}
