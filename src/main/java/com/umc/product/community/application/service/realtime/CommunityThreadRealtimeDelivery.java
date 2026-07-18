package com.umc.product.community.application.service.realtime;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

import org.springframework.stereotype.Component;

import com.umc.product.community.application.port.in.query.thread.GetCommunityThreadDetailUseCase;
import com.umc.product.community.application.port.in.query.thread.dto.GetThreadDetailQuery;
import com.umc.product.community.application.port.in.query.thread.dto.ThreadDetailInfo;
import com.umc.product.community.application.port.in.query.thread.dto.ThreadSummaryInfo;
import com.umc.product.community.application.port.in.query.thread.message.GetCommunityThreadMessageForRecipientsUseCase;
import com.umc.product.community.application.port.in.query.thread.message.dto.CommunityThreadMessageInfo;
import com.umc.product.community.application.port.in.query.thread.message.dto.CommunityThreadMessageRecipientsQuery;
import com.umc.product.community.application.port.in.realtime.dto.CommunityThreadRealtimeEvent;
import com.umc.product.community.application.port.in.realtime.dto.CommunityThreadRealtimeEventType;
import com.umc.product.community.application.port.in.realtime.dto.CommunityThreadRealtimePayload;
import com.umc.product.community.application.port.out.realtime.CommunityThreadRealtimeBroadcastPort;
import com.umc.product.community.application.port.out.thread.CommunityThreadQueryPort;
import com.umc.product.community.application.port.out.thread.LoadCommunityThreadPort;
import com.umc.product.community.application.service.realtime.CommunityThreadRealtimeMetrics.Operation;
import com.umc.product.community.application.service.realtime.CommunityThreadRealtimeMetrics.Outcome;
import com.umc.product.community.application.service.realtime.CommunityThreadRealtimeMetrics.Reason;
import com.umc.product.community.domain.CommunityThread;
import com.umc.product.community.domain.CommunityThreadProperties;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
class CommunityThreadRealtimeDelivery {

    private final LoadCommunityThreadPort loadThreadPort;
    private final CommunityThreadQueryPort threadQueryPort;
    private final GetCommunityThreadMessageForRecipientsUseCase getMessageForRecipientsUseCase;
    private final GetCommunityThreadDetailUseCase getThreadDetailUseCase;
    private final CommunityThreadRealtimeBroadcastPort broadcastPort;
    private final CommunityThreadProperties properties;
    private final CommunityThreadRealtimeMetrics metrics;

    Optional<CommunityThread> activeThreadByChatRoomId(Long chatRoomId) {
        return loadThreadPort.findByChatRoomId(chatRoomId)
            .filter(thread -> !thread.isDeleted());
    }

    Optional<CommunityThread> activeThread(Long threadId) {
        return loadThreadPort.findById(threadId)
            .filter(thread -> !thread.isDeleted());
    }

    List<Long> activeRecipients(Long threadId) {
        return audience(threadQueryPort.listActiveMemberIdsByThreadId(
            threadId,
            Math.addExact(properties.maxMembers(), 1)
        ));
    }

    List<Long> currentlyActiveRecipients(Long threadId, List<Long> candidates) {
        Set<Long> activeMemberIds = Set.copyOf(activeRecipients(threadId));
        return audience(candidates).stream()
            .filter(activeMemberIds::contains)
            .toList();
    }

    List<Long> terminalAudience(List<Long> activeMemberIds, Long affectedMemberId) {
        List<Long> recipients = audience(activeMemberIds);
        if (!recipients.contains(affectedMemberId)) {
            throw new IllegalStateException("terminal audience must contain the affected member");
        }
        return recipients;
    }

    List<Long> audience(List<Long> memberIds) {
        List<Long> recipients = memberIds.stream()
            .map(this::requirePositiveMemberId)
            .distinct()
            .toList();
        if (!properties.allowsFanOutRecipientCount(recipients.size())) {
            throw new IllegalStateException("fan-out audience exceeds the configured maximum");
        }
        return recipients;
    }

    Map<Long, CommunityThreadMessageInfo> messages(
        Long threadId,
        List<Long> recipientMemberIds,
        Long messageId
    ) {
        return getMessageForRecipientsUseCase.getMessageForRecipients(
            new CommunityThreadMessageRecipientsQuery(
                threadId,
                messageId,
                recipientMemberIds
            )
        );
    }

    ThreadSummaryInfo threadSummary(Long threadId, Long memberId) {
        ThreadDetailInfo detail = getThreadDetailUseCase.getThread(
            new GetThreadDetailQuery(threadId, memberId)
        );
        return new ThreadSummaryInfo(
            detail.threadId(), detail.title(), detail.description(), detail.category(), detail.icon(),
            detail.memberCount(), detail.unreadCount(), detail.maxMembers(), detail.isPinned(),
            detail.isMuted(), detail.myRole(), detail.lastMessage(), detail.createdBy(),
            detail.createdAt(), detail.updatedAt()
        );
    }

    int maxMembers() {
        return properties.maxMembers();
    }

    <P extends CommunityThreadRealtimePayload> CommunityThreadRealtimeEvent<P> envelope(
        UUID eventId,
        CommunityThreadRealtimeEventType type,
        Long threadId,
        Instant occurredAt,
        P payload
    ) {
        return CommunityThreadRealtimeEvent.of(eventId, type, threadId, occurredAt, payload);
    }

    void fanOutThreadMembers(
        Long threadId,
        List<Long> recipients,
        Operation operation,
        Function<Long, CommunityThreadRealtimeEvent<? extends CommunityThreadRealtimePayload>> eventFactory
    ) {
        fanOut(threadId, recipients, operation, eventFactory, false);
    }

    void fanOutPersonalMembers(
        Long threadId,
        List<Long> recipients,
        Operation operation,
        Function<Long, CommunityThreadRealtimeEvent<? extends CommunityThreadRealtimePayload>> eventFactory
    ) {
        fanOut(threadId, recipients, operation, eventFactory, true);
    }

    private void fanOut(
        Long threadId,
        List<Long> recipients,
        Operation operation,
        Function<Long, CommunityThreadRealtimeEvent<? extends CommunityThreadRealtimePayload>> eventFactory,
        boolean personal
    ) {
        List<RuntimeException> failures = new ArrayList<>();
        for (Long memberId : recipients) {
            CommunityThreadRealtimeEvent<? extends CommunityThreadRealtimePayload> event;
            try {
                event = eventFactory.apply(memberId);
            } catch (RuntimeException exception) {
                failures.add(exception);
                continue;
            }
            try {
                if (personal) {
                    broadcastPort.broadcastToMember(memberId, event);
                } else {
                    broadcastPort.broadcastToThreadMember(threadId, memberId, event);
                }
            } catch (RuntimeException exception) {
                metrics.recordBroadcastFailure(operation, Reason.BROKER_UNAVAILABLE);
                failures.add(exception);
            }
        }

        Outcome outcome = failures.isEmpty() ? Outcome.SUCCESS : Outcome.FAILURE;
        metrics.recordFanOut(operation, outcome, recipients.size());
        if (!failures.isEmpty()) {
            throw aggregateFailure(operation, failures);
        }
    }

    private IllegalStateException aggregateFailure(Operation operation, List<RuntimeException> failures) {
        IllegalStateException aggregate = new IllegalStateException(
            "Community realtime fan-out failed: operation=" + operation.value()
                + ", failures=" + failures.size()
        );
        failures.forEach(aggregate::addSuppressed);
        return aggregate;
    }

    private Long requirePositiveMemberId(Long memberId) {
        if (memberId == null || memberId <= 0) {
            throw new IllegalArgumentException("memberId must be positive");
        }
        return memberId;
    }
}
