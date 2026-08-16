package com.umc.product.community.application.service.message;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.chat.application.port.in.query.GetChatMessageForViewersUseCase;
import com.umc.product.chat.application.port.in.query.GetChatMessageUseCase;
import com.umc.product.chat.application.port.in.query.GetChatMessagesUseCase;
import com.umc.product.chat.application.port.in.query.dto.ChatMessageCursorResult;
import com.umc.product.chat.application.port.in.query.dto.ChatMessageInfo;
import com.umc.product.chat.application.port.in.query.dto.GetChatMessageForViewersQuery;
import com.umc.product.chat.application.port.in.query.dto.GetChatMessageQuery;
import com.umc.product.chat.application.port.in.query.dto.GetChatMessagesQuery;
import com.umc.product.community.application.port.in.query.thread.message.GetCommunityThreadMessageForRecipientsUseCase;
import com.umc.product.community.application.port.in.query.thread.message.GetCommunityThreadMessageHistoryUseCase;
import com.umc.product.community.application.port.in.query.thread.message.GetCommunityThreadMessageUseCase;
import com.umc.product.community.application.port.in.query.thread.message.RecoverCommunityThreadMessagesUseCase;
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
import com.umc.product.community.domain.exception.CommunityDomainException;
import com.umc.product.community.domain.exception.CommunityErrorCode;

import lombok.RequiredArgsConstructor;

/**
 * Community thread 메시지 조회.
 *
 * <p>사용자 조회 경로({@code getHistory}, {@code recover}, {@code getMessage})는 스레드 상세와 같은
 * 공개 범위를 따른다. 삭제되지 않은 스레드면 비참여자도 읽을 수 있고, 강퇴된 요청자만 차단한다.
 * Chat engine은 방의 조회 범위로 이를 다시 확인한다. 메시지 생성/수정/삭제, 리액션, 읽음, 신고와
 * 실시간 수신은 ACTIVE 멤버 전용이며 각 command 경로가 검증한다.</p>
 *
 * <p>반면 실시간 fan-out용 {@code getMessageForRecipients}는 수신자가 모두 ACTIVE 멤버인지 검증한다.
 * 공개 조회와 달리 이 경로는 전달 대상 자체가 멤버로 한정되기 때문이다.</p>
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommunityThreadMessageQueryService implements
    GetCommunityThreadMessageHistoryUseCase,
    GetCommunityThreadMessageUseCase,
    GetCommunityThreadMessageForRecipientsUseCase,
    RecoverCommunityThreadMessagesUseCase {

    private final LoadCommunityThreadPort loadThreadPort;
    private final LoadCommunityThreadMemberPort loadThreadMemberPort;
    private final GetChatMessagesUseCase getChatMessagesUseCase;
    private final GetChatMessageUseCase getChatMessageUseCase;
    private final GetChatMessageForViewersUseCase getChatMessageForViewersUseCase;
    private final CommunityThreadMessageInfoAssembler infoAssembler;
    private final CommunityThreadRealtimeMetrics realtimeMetrics;

    @Override
    public CommunityThreadMessagePageInfo getHistory(CommunityThreadMessageHistoryQuery query) {
        return getPageWithBackfillMetric(
            query.threadId(),
            query.requesterMemberId(),
            query.beforeMessageId(),
            query.limit()
        );
    }

    @Override
    public CommunityThreadMessageInfo getMessage(CommunityThreadMessageQuery query) {
        CommunityThread thread = loadPubliclyReadableThread(query.threadId(), query.requesterMemberId());
        ChatMessageInfo message = getChatMessageUseCase.getMessage(
            new GetChatMessageQuery(thread.getChatRoomId(), query.requesterMemberId(), query.messageId())
        );
        return infoAssembler.assemble(query.threadId(), message);
    }

    @Override
    public Map<Long, CommunityThreadMessageInfo> getMessageForRecipients(
        CommunityThreadMessageRecipientsQuery query
    ) {
        if (query.recipientMemberIds().isEmpty()) {
            return Map.of();
        }

        CommunityThread thread = loadActiveThread(query.threadId());
        verifyActiveRecipients(query.threadId(), query.recipientMemberIds());
        Map<Long, ChatMessageInfo> messagesByRecipient = getChatMessageForViewersUseCase
            .getMessageForViewers(new GetChatMessageForViewersQuery(
                thread.getChatRoomId(),
                query.messageId(),
                query.recipientMemberIds()
            ));
        return infoAssembler.assembleForRecipients(query.threadId(), messagesByRecipient);
    }

    @Override
    public CommunityThreadMessagePageInfo recover(CommunityThreadMessageRecoveryQuery query) {
        return getPageWithBackfillMetric(
            query.threadId(),
            query.requesterMemberId(),
            query.beforeMessageId(),
            query.limit()
        );
    }

    private CommunityThreadMessagePageInfo getPageWithBackfillMetric(
        Long threadId,
        Long requesterMemberId,
        Long beforeMessageId,
        int limit
    ) {
        CommunityThreadMessagePageInfo page;
        try {
            page = getPage(
                threadId,
                requesterMemberId,
                beforeMessageId,
                limit
            );
        } catch (RuntimeException exception) {
            realtimeMetrics.recordBackfill(Operation.MESSAGE_HISTORY, Outcome.FAILURE);
            throw exception;
        }
        realtimeMetrics.recordBackfill(Operation.MESSAGE_HISTORY, Outcome.SUCCESS);
        return page;
    }

    private CommunityThreadMessagePageInfo getPage(
        Long threadId,
        Long requesterMemberId,
        Long beforeMessageId,
        int limit
    ) {
        CommunityThread thread = loadPubliclyReadableThread(threadId, requesterMemberId);
        ChatMessageCursorResult chatPage = getChatMessagesUseCase.getMessages(
            new GetChatMessagesQuery(thread.getChatRoomId(), requesterMemberId, beforeMessageId, limit)
        );
        return new CommunityThreadMessagePageInfo(
            infoAssembler.assemble(threadId, chatPage.content()),
            chatPage.hasNext(),
            chatPage.nextCursor()
        );
    }

    /**
     * 참여 여부와 무관하게 읽을 수 있는 스레드를 반환한다. 강퇴된 요청자만 차단한다.
     */
    private CommunityThread loadPubliclyReadableThread(Long threadId, Long requesterMemberId) {
        CommunityThread thread = loadActiveThread(threadId);
        boolean kicked = loadThreadMemberPort.findByThreadIdAndMemberId(threadId, requesterMemberId)
            .filter(CommunityThreadMember::isKicked)
            .isPresent();
        if (kicked) {
            throw new CommunityDomainException(CommunityErrorCode.THREAD_ACCESS_DENIED);
        }
        return thread;
    }

    /**
     * 삭제되지 않은 스레드를 조회한다. 멤버십은 검증하지 않으므로 호출부가 필요한 권한을 직접 확인한다.
     */
    private CommunityThread loadActiveThread(Long threadId) {
        CommunityThread thread = loadThreadPort.findById(threadId)
            .orElseThrow(() -> new CommunityDomainException(CommunityErrorCode.THREAD_NOT_FOUND));
        if (thread.isDeleted()) {
            throw new CommunityDomainException(CommunityErrorCode.THREAD_NOT_FOUND);
        }
        return thread;
    }

    private void verifyActiveRecipients(Long threadId, List<Long> recipientMemberIds) {
        Set<Long> recipients = Set.copyOf(recipientMemberIds);
        Set<Long> activeRecipients = loadThreadMemberPort
            .listByThreadIdAndMemberIds(threadId, recipients)
            .stream()
            .filter(CommunityThreadMember::isActive)
            .map(CommunityThreadMember::getMemberId)
            .collect(Collectors.toSet());
        if (!activeRecipients.equals(recipients)) {
            throw new CommunityDomainException(CommunityErrorCode.THREAD_ACCESS_DENIED);
        }
    }
}
