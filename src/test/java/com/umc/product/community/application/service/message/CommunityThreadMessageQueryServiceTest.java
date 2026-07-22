package com.umc.product.community.application.service.message;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.inOrder;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.chat.application.port.in.query.GetChatMessageForViewersUseCase;
import com.umc.product.chat.application.port.in.query.GetChatMessageUseCase;
import com.umc.product.chat.application.port.in.query.GetChatMessagesUseCase;
import com.umc.product.chat.application.port.in.query.dto.ChatMessageCursorResult;
import com.umc.product.chat.application.port.in.query.dto.ChatMessageInfo;
import com.umc.product.chat.application.port.in.query.dto.ChatMessageReplyInfo;
import com.umc.product.chat.application.port.in.query.dto.ChatReactionInfo;
import com.umc.product.chat.application.port.in.query.dto.GetChatMessageForViewersQuery;
import com.umc.product.chat.application.port.in.query.dto.GetChatMessageQuery;
import com.umc.product.chat.application.port.in.query.dto.GetChatMessagesQuery;
import com.umc.product.chat.domain.MessageContentType;
import com.umc.product.community.application.port.in.query.thread.message.dto.CommunityThreadMessageHistoryQuery;
import com.umc.product.community.application.port.in.query.thread.message.dto.CommunityThreadMessageInfo;
import com.umc.product.community.application.port.in.query.thread.message.dto.CommunityThreadMessagePageInfo;
import com.umc.product.community.application.port.in.query.thread.message.dto.CommunityThreadMessageQuery;
import com.umc.product.community.application.port.in.query.thread.message.dto.CommunityThreadMessageRecipientsQuery;
import com.umc.product.community.application.port.in.query.thread.message.dto.CommunityThreadMessageRecoveryQuery;
import com.umc.product.community.application.port.out.thread.LoadCommunityThreadMemberPort;
import com.umc.product.community.application.port.out.thread.LoadCommunityThreadPort;
import com.umc.product.community.application.service.realtime.CommunityThreadRealtimeMetrics;
import com.umc.product.community.application.service.realtime.CommunityThreadRealtimeMetrics.Operation;
import com.umc.product.community.application.service.realtime.CommunityThreadRealtimeMetrics.Outcome;
import com.umc.product.community.domain.CommunityThread;
import com.umc.product.community.domain.CommunityThreadMember;
import com.umc.product.community.domain.enums.CommunityThreadCategory;
import com.umc.product.community.domain.exception.CommunityDomainException;
import com.umc.product.community.domain.exception.CommunityErrorCode;

@ExtendWith(MockitoExtension.class)
@DisplayName("CommunityThreadMessageQueryService")
class CommunityThreadMessageQueryServiceTest {

    private static final Long THREAD_ID = 11L;
    private static final Long ROOM_ID = 101L;
    private static final Long MEMBER_ID = 30L;
    private static final Instant NOW = Instant.parse("2026-07-18T00:00:00Z");

    @Mock
    LoadCommunityThreadPort loadThreadPort;
    @Mock
    LoadCommunityThreadMemberPort loadThreadMemberPort;
    @Mock
    GetChatMessagesUseCase getChatMessagesUseCase;
    @Mock
    GetChatMessageUseCase getChatMessageUseCase;
    @Mock
    GetChatMessageForViewersUseCase getChatMessageForViewersUseCase;
    @Mock
    CommunityThreadMessageInfoAssembler infoAssembler;
    @Mock
    CommunityThreadRealtimeMetrics realtimeMetrics;

    @InjectMocks
    CommunityThreadMessageQueryService sut;

    @Test
    @DisplayName("history는 Community ACTIVE 멤버 검증 뒤 Chat history를 호출하고 threadId만 포함한 page를 반환한다")
    void historyValidatesCommunityBeforeChat() {
        CommunityThread thread = thread();
        CommunityThreadMember member = activeMember(MEMBER_ID);
        ChatMessageInfo chatMessage = chatMessage(900L, MEMBER_ID, "본문");
        ChatMessageCursorResult chatPage = new ChatMessageCursorResult(List.of(chatMessage), 899L, true);
        CommunityThreadMessageInfo communityInfo = org.mockito.Mockito.mock(CommunityThreadMessageInfo.class);
        given(loadThreadPort.findById(THREAD_ID)).willReturn(Optional.of(thread));
        given(loadThreadMemberPort.findByThreadIdAndMemberId(THREAD_ID, MEMBER_ID))
            .willReturn(Optional.of(member));
        given(getChatMessagesUseCase.getMessages(any(GetChatMessagesQuery.class))).willReturn(chatPage);
        given(infoAssembler.assemble(THREAD_ID, List.of(chatMessage))).willReturn(List.of(communityInfo));

        CommunityThreadMessagePageInfo result = sut.getHistory(
            new CommunityThreadMessageHistoryQuery(THREAD_ID, MEMBER_ID, 1_000L, 30)
        );

        assertThat(result.messages()).containsExactly(communityInfo);
        assertThat(result.hasMore()).isTrue();
        assertThat(result.nextBefore()).isEqualTo(899L);
        then(getChatMessagesUseCase).should().getMessages(
            new GetChatMessagesQuery(ROOM_ID, MEMBER_ID, 1_000L, 30)
        );
        then(realtimeMetrics).should().recordBackfill(Operation.MESSAGE_HISTORY, Outcome.SUCCESS);
        InOrder order = inOrder(loadThreadPort, loadThreadMemberPort, getChatMessagesUseCase);
        order.verify(loadThreadPort).findById(THREAD_ID);
        order.verify(loadThreadMemberPort).findByThreadIdAndMemberId(THREAD_ID, MEMBER_ID);
        order.verify(getChatMessagesUseCase).getMessages(any(GetChatMessagesQuery.class));
    }

    @Test
    @DisplayName("single은 Community ACTIVE 멤버 검증 뒤 Chat 단건 query를 호출한다")
    void singleValidatesCommunityBeforeChat() {
        CommunityThreadMember member = activeMember(MEMBER_ID);
        ChatMessageInfo chatMessage = chatMessage(900L, MEMBER_ID, "본문");
        CommunityThreadMessageInfo communityInfo = org.mockito.Mockito.mock(CommunityThreadMessageInfo.class);
        given(loadThreadPort.findById(THREAD_ID)).willReturn(Optional.of(thread()));
        given(loadThreadMemberPort.findByThreadIdAndMemberId(THREAD_ID, MEMBER_ID))
            .willReturn(Optional.of(member));
        given(getChatMessageUseCase.getMessage(any(GetChatMessageQuery.class))).willReturn(chatMessage);
        given(infoAssembler.assemble(THREAD_ID, chatMessage)).willReturn(communityInfo);

        CommunityThreadMessageInfo result = sut.getMessage(
            new CommunityThreadMessageQuery(THREAD_ID, MEMBER_ID, 900L)
        );

        assertThat(result).isSameAs(communityInfo);
        then(getChatMessageUseCase).should().getMessage(new GetChatMessageQuery(ROOM_ID, MEMBER_ID, 900L));
        then(realtimeMetrics).shouldHaveNoInteractions();
        InOrder order = inOrder(loadThreadPort, loadThreadMemberPort, getChatMessageUseCase);
        order.verify(loadThreadPort).findById(THREAD_ID);
        order.verify(loadThreadMemberPort).findByThreadIdAndMemberId(THREAD_ID, MEMBER_ID);
        order.verify(getChatMessageUseCase).getMessage(any(GetChatMessageQuery.class));
    }

    @Test
    @DisplayName("recovery도 Community ACTIVE 멤버 검증 뒤 Chat public history query만 호출한다")
    void recoveryValidatesCommunityBeforeChat() {
        CommunityThreadMember member = activeMember(MEMBER_ID);
        ChatMessageInfo chatMessage = chatMessage(900L, MEMBER_ID, "복구");
        ChatMessageCursorResult chatPage = new ChatMessageCursorResult(List.of(chatMessage), null, false);
        CommunityThreadMessageInfo communityInfo = org.mockito.Mockito.mock(CommunityThreadMessageInfo.class);
        given(loadThreadPort.findById(THREAD_ID)).willReturn(Optional.of(thread()));
        given(loadThreadMemberPort.findByThreadIdAndMemberId(THREAD_ID, MEMBER_ID))
            .willReturn(Optional.of(member));
        given(getChatMessagesUseCase.getMessages(any(GetChatMessagesQuery.class))).willReturn(chatPage);
        given(infoAssembler.assemble(THREAD_ID, List.of(chatMessage))).willReturn(List.of(communityInfo));

        CommunityThreadMessagePageInfo result = sut.recover(
            new CommunityThreadMessageRecoveryQuery(THREAD_ID, MEMBER_ID, null, 30)
        );

        assertThat(result.messages()).containsExactly(communityInfo);
        assertThat(result.hasMore()).isFalse();
        assertThat(result.nextBefore()).isNull();
        then(getChatMessagesUseCase).should().getMessages(new GetChatMessagesQuery(ROOM_ID, MEMBER_ID, null, 30));
        then(realtimeMetrics).should().recordBackfill(Operation.MESSAGE_HISTORY, Outcome.SUCCESS);
    }

    @Test
    @DisplayName("삭제된 thread는 history Chat query 전에 THREAD_NOT_FOUND로 거절한다")
    void deletedThreadIsRejectedBeforeHistory() {
        CommunityThread deleted = thread();
        deleted.delete(NOW);
        given(loadThreadPort.findById(THREAD_ID)).willReturn(Optional.of(deleted));

        assertThatThrownBy(() -> sut.getHistory(new CommunityThreadMessageHistoryQuery(
            THREAD_ID, MEMBER_ID, null, 30
        )))
            .isInstanceOf(CommunityDomainException.class)
            .extracting(error -> ((CommunityDomainException) error).getBaseCode())
            .isEqualTo(CommunityErrorCode.THREAD_NOT_FOUND);
        then(loadThreadMemberPort).shouldHaveNoInteractions();
        then(getChatMessagesUseCase).shouldHaveNoInteractions();
        then(realtimeMetrics).should().recordBackfill(Operation.MESSAGE_HISTORY, Outcome.FAILURE);
    }

    @Test
    @DisplayName("history 실패는 failure metric 기록 뒤 같은 예외를 전파한다")
    void historyFailureRecordsMetricAndPropagates() {
        RuntimeException failure = new IllegalStateException("chat query failed");
        given(loadThreadPort.findById(THREAD_ID)).willReturn(Optional.of(thread()));
        given(loadThreadMemberPort.findByThreadIdAndMemberId(THREAD_ID, MEMBER_ID))
            .willReturn(Optional.of(activeMember(MEMBER_ID)));
        given(getChatMessagesUseCase.getMessages(any(GetChatMessagesQuery.class))).willThrow(failure);

        assertThatThrownBy(() -> sut.getHistory(new CommunityThreadMessageHistoryQuery(
            THREAD_ID, MEMBER_ID, null, 30
        ))).isSameAs(failure);

        then(realtimeMetrics).should().recordBackfill(Operation.MESSAGE_HISTORY, Outcome.FAILURE);
    }

    @Test
    @DisplayName("recover 실패는 failure metric 기록 뒤 같은 예외를 전파한다")
    void recoveryFailureRecordsMetricAndPropagates() {
        RuntimeException failure = new IllegalStateException("chat recovery failed");
        given(loadThreadPort.findById(THREAD_ID)).willReturn(Optional.of(thread()));
        given(loadThreadMemberPort.findByThreadIdAndMemberId(THREAD_ID, MEMBER_ID))
            .willReturn(Optional.of(activeMember(MEMBER_ID)));
        given(getChatMessagesUseCase.getMessages(any(GetChatMessagesQuery.class))).willThrow(failure);

        assertThatThrownBy(() -> sut.recover(new CommunityThreadMessageRecoveryQuery(
            THREAD_ID, MEMBER_ID, null, 30
        ))).isSameAs(failure);

        then(realtimeMetrics).should().recordBackfill(Operation.MESSAGE_HISTORY, Outcome.FAILURE);
    }

    @Test
    @DisplayName("LEFT 멤버는 single Chat query 전에 THREAD_ACCESS_DENIED로 거절한다")
    void inactiveMemberIsRejectedBeforeSingle() {
        CommunityThreadMember left = activeMember(MEMBER_ID);
        left.leave(NOW.minusSeconds(60));
        given(loadThreadPort.findById(THREAD_ID)).willReturn(Optional.of(thread()));
        given(loadThreadMemberPort.findByThreadIdAndMemberId(THREAD_ID, MEMBER_ID))
            .willReturn(Optional.of(left));

        assertThatThrownBy(() -> sut.getMessage(new CommunityThreadMessageQuery(THREAD_ID, MEMBER_ID, 900L)))
            .isInstanceOf(CommunityDomainException.class)
            .extracting(error -> ((CommunityDomainException) error).getBaseCode())
            .isEqualTo(CommunityErrorCode.THREAD_ACCESS_DENIED);
        then(getChatMessageUseCase).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("빈 recipient batch는 thread와 Chat을 조회하지 않고 빈 결과를 반환한다")
    void recipients_emptyBatchShortCircuits() {
        Map<Long, CommunityThreadMessageInfo> result = sut.getMessageForRecipients(
            new CommunityThreadMessageRecipientsQuery(THREAD_ID, 900L, List.of())
        );

        assertThat(result).isEmpty();
        then(loadThreadPort).shouldHaveNoInteractions();
        then(getChatMessageForViewersUseCase).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("recipient batch는 모든 ACTIVE membership을 검증한 뒤 Chat viewer 결과를 조립한다")
    void recipients_validatesAllMembersBeforeChatQuery() {
        Long secondMemberId = 40L;
        CommunityThread thread = thread();
        CommunityThreadMember first = activeMember(MEMBER_ID);
        CommunityThreadMember second = activeMember(secondMemberId);
        ChatMessageInfo firstMessage = chatMessage(900L, MEMBER_ID, "첫 번째");
        ChatMessageInfo secondMessage = chatMessage(900L, MEMBER_ID, "두 번째");
        CommunityThreadMessageInfo firstInfo = org.mockito.Mockito.mock(CommunityThreadMessageInfo.class);
        CommunityThreadMessageInfo secondInfo = org.mockito.Mockito.mock(CommunityThreadMessageInfo.class);
        Map<Long, ChatMessageInfo> chatResult = Map.of(
            MEMBER_ID, firstMessage,
            secondMemberId, secondMessage
        );
        Map<Long, CommunityThreadMessageInfo> assembled = Map.of(
            MEMBER_ID, firstInfo,
            secondMemberId, secondInfo
        );
        given(loadThreadPort.findById(THREAD_ID)).willReturn(Optional.of(thread));
        given(loadThreadMemberPort.listByThreadIdAndMemberIds(
            THREAD_ID,
            Set.of(MEMBER_ID, secondMemberId)
        )).willReturn(List.of(first, second));
        given(getChatMessageForViewersUseCase.getMessageForViewers(
            new GetChatMessageForViewersQuery(ROOM_ID, 900L, List.of(MEMBER_ID, secondMemberId))
        )).willReturn(chatResult);
        given(infoAssembler.assembleForRecipients(THREAD_ID, chatResult)).willReturn(assembled);

        Map<Long, CommunityThreadMessageInfo> result = sut.getMessageForRecipients(
            new CommunityThreadMessageRecipientsQuery(
                THREAD_ID,
                900L,
                List.of(MEMBER_ID, secondMemberId)
            )
        );

        assertThat(result).isEqualTo(assembled);
        InOrder order = inOrder(loadThreadPort, loadThreadMemberPort, getChatMessageForViewersUseCase);
        order.verify(loadThreadPort).findById(THREAD_ID);
        order.verify(loadThreadMemberPort).listByThreadIdAndMemberIds(
            THREAD_ID,
            Set.of(MEMBER_ID, secondMemberId)
        );
        order.verify(getChatMessageForViewersUseCase).getMessageForViewers(any());
    }

    @Test
    @DisplayName("recipient 중 하나라도 비활성이면 전체 viewer 조회를 fail-closed한다")
    void recipients_inactiveMemberRejectsWholeBatch() {
        Long inactiveMemberId = 40L;
        CommunityThreadMember active = activeMember(MEMBER_ID);
        CommunityThreadMember inactive = activeMember(inactiveMemberId);
        inactive.leave(NOW.minusSeconds(1));
        given(loadThreadPort.findById(THREAD_ID)).willReturn(Optional.of(thread()));
        given(loadThreadMemberPort.listByThreadIdAndMemberIds(
            THREAD_ID,
            Set.of(MEMBER_ID, inactiveMemberId)
        )).willReturn(List.of(active, inactive));

        assertThatThrownBy(() -> sut.getMessageForRecipients(
            new CommunityThreadMessageRecipientsQuery(
                THREAD_ID,
                900L,
                List.of(MEMBER_ID, inactiveMemberId)
            )
        ))
            .isInstanceOf(CommunityDomainException.class)
            .extracting(error -> ((CommunityDomainException) error).getBaseCode())
            .isEqualTo(CommunityErrorCode.THREAD_ACCESS_DENIED);
        then(getChatMessageForViewersUseCase).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("존재하지 않는 thread는 membership과 Chat 조회 전에 THREAD_NOT_FOUND로 실패한다")
    void single_missingThreadRejected() {
        given(loadThreadPort.findById(THREAD_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> sut.getMessage(
            new CommunityThreadMessageQuery(THREAD_ID, MEMBER_ID, 900L)
        ))
            .isInstanceOf(CommunityDomainException.class)
            .extracting(error -> ((CommunityDomainException) error).getBaseCode())
            .isEqualTo(CommunityErrorCode.THREAD_NOT_FOUND);
        then(loadThreadMemberPort).shouldHaveNoInteractions();
        then(getChatMessageUseCase).shouldHaveNoInteractions();
    }

    private CommunityThread thread() {
        CommunityThread thread = CommunityThread.create(
            ROOM_ID,
            "스레드",
            null,
            CommunityThreadCategory.FREE,
            "💬",
            10L,
            NOW
        );
        ReflectionTestUtils.setField(thread, "id", THREAD_ID);
        return thread;
    }

    private CommunityThreadMember activeMember(Long memberId) {
        return CommunityThreadMember.createMember(THREAD_ID, memberId, NOW);
    }

    private ChatMessageInfo chatMessage(Long messageId, Long senderId, String content) {
        return new ChatMessageInfo(
            messageId,
            ROOM_ID,
            senderId,
            MessageContentType.TEXT,
            content,
            List.of(),
            NOW,
            700L,
            null,
            null,
            null,
            List.of(20L),
            new ChatMessageReplyInfo(700L, 20L, "답글"),
            List.of(new ChatReactionInfo("👍", 2L, false))
        );
    }
}
