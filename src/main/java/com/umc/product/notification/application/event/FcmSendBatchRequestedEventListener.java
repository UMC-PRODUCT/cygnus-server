package com.umc.product.notification.application.event;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.umc.product.global.config.FcmProperties;
import com.umc.product.global.event.application.port.out.DomainEventPublisher;
import com.umc.product.global.logging.OperationalMetrics;
import com.umc.product.notification.application.port.out.LoadFcmPort;
import com.umc.product.notification.application.port.out.SaveFcmPort;
import com.umc.product.notification.application.port.out.SendFcmMessagePort;
import com.umc.product.notification.application.port.out.dto.FcmSendRequest;
import com.umc.product.notification.application.port.out.dto.FcmSendResult;
import com.umc.product.notification.application.port.out.dto.FcmSendTarget;
import com.umc.product.notification.domain.FcmToken;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class FcmSendBatchRequestedEventListener {

    private final FcmProperties fcmProperties;
    private final LoadFcmPort loadFcmPort;
    private final SaveFcmPort saveFcmPort;
    private final SendFcmMessagePort sendFcmMessagePort;
    private final DomainEventPublisher eventPublisher;
    private final OperationalMetrics operationalMetrics;

    @EventListener
    public void handle(FcmSendBatchRequestedEvent event) {
        if (!fcmProperties.enabled()) {
            log.info("[FCM 비활성화] FCM 배치 발송 이벤트를 건너뜁니다: requestId={}", event.requestId());
            return;
        }

        List<FcmToken> tokens = loadFcmPort.listActiveByIds(event.tokenIds());
        if (tokens.isEmpty()) {
            log.info("발송할 활성 FCM 토큰 없음: requestId={}", event.requestId());
            return;
        }

        FcmSendResult result = sendFcmMessagePort.send(FcmSendRequest.of(
            targets(tokens),
            event.title(),
            event.body(),
            event.data(),
            event.imageUrl(),
            event.deepLink()
        ));
        deactivateInvalidTokens(tokens, result.invalidTokenIds());
        publishRetryBatch(event, result.retryableTokenIds());
        recordFcmMetric(result);
        log.info("FCM 배치 발송 결과: requestId={}, success={}, failure={}, retryable={}",
            event.requestId(), result.successCount(), result.failureCount(), result.retryableTokenIds().size());
    }

    private List<FcmSendTarget> targets(List<FcmToken> tokens) {
        return tokens.stream()
            .map(token -> FcmSendTarget.of(token.getId(), token.getFcmToken()))
            .toList();
    }

    private void deactivateInvalidTokens(List<FcmToken> tokens, List<Long> invalidTokenIds) {
        if (invalidTokenIds.isEmpty()) {
            return;
        }
        Set<Long> invalidTokenIdSet = new HashSet<>(invalidTokenIds);
        List<FcmToken> deactivatedTokens = tokens.stream()
            .filter(token -> invalidTokenIdSet.contains(token.getId()))
            .peek(token -> {
                token.deactivate();
                log.info("유효하지 않은 FCM 토큰 비활성화 tokenId={}, memberId={}",
                    token.getId(), token.getMemberId());
            })
            .toList();
        if (!deactivatedTokens.isEmpty()) {
            saveFcmPort.saveAll(deactivatedTokens);
        }
    }

    private void publishRetryBatch(FcmSendBatchRequestedEvent event, List<Long> retryableTokenIds) {
        if (retryableTokenIds.isEmpty()) {
            return;
        }
        eventPublisher.publish(FcmSendBatchRequestedEvent.retry(retryableTokenIds, event));
        log.info("FCM 부분 실패 재시도 배치 이벤트를 발행했습니다: requestId={}, tokenCount={}",
            event.requestId(), retryableTokenIds.size());
    }

    private void recordFcmMetric(FcmSendResult result) {
        operationalMetrics.recordNotification("FCM", "SEND_BATCH", "success", result.successCount());
        operationalMetrics.recordNotification("FCM", "SEND_BATCH", "failure", result.failureCount());
        operationalMetrics.recordNotification("FCM", "SEND_BATCH", "retry", result.retryableTokenIds().size());
    }
}
