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
        CommunityThread thread = loadReadableThread(query.threadId(), query.requesterMemberId());
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
        CommunityThread thread = loadReadableThread(threadId, requesterMemberId);
        ChatMessageCursorResult chatPage = getChatMessagesUseCase.getMessages(
            new GetChatMessagesQuery(thread.getChatRoomId(), requesterMemberId, beforeMessageId, limit)
        );
        return new CommunityThreadMessagePageInfo(
            infoAssembler.assemble(threadId, chatPage.content()),
            chatPage.hasNext(),
            chatPage.nextCursor()
        );
    }

    private CommunityThread loadReadableThread(Long threadId, Long requesterMemberId) {
        CommunityThread thread = loadActiveThread(threadId);
        loadThreadMemberPort.findByThreadIdAndMemberId(threadId, requesterMemberId)
            .filter(member -> member.isActive())
            .orElseThrow(() -> new CommunityDomainException(CommunityErrorCode.THREAD_ACCESS_DENIED));
        return thread;
    }

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
