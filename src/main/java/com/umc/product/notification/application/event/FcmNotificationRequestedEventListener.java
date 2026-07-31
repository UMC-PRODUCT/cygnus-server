package com.umc.product.notification.application.event;

import java.util.ArrayList;
import java.util.List;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.umc.product.global.config.FcmProperties;
import com.umc.product.global.event.application.port.out.DomainEventPublisher;
import com.umc.product.notification.application.port.out.LoadFcmPort;
import com.umc.product.notification.application.service.FcmAudienceResolver;
import com.umc.product.notification.domain.FcmToken;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class FcmNotificationRequestedEventListener {

    private static final int FCM_MULTICAST_BATCH_SIZE = 500;

    private final FcmProperties fcmProperties;
    private final FcmAudienceResolver fcmAudienceResolver;
    private final LoadFcmPort loadFcmPort;
    private final DomainEventPublisher eventPublisher;

    @EventListener
    public void handle(FcmNotificationRequestedEvent event) {
        if (!fcmProperties.enabled()) {
            log.info("[FCM 비활성화] FCM 알림 요청 이벤트를 건너뜁니다: requestId={}", event.requestId());
            return;
        }

        List<Long> memberIds = fcmAudienceResolver.resolve(event);
        if (memberIds.isEmpty()) {
            log.info("FCM 알림 발송 대상 없음: requestId={}", event.requestId());
            return;
        }

        List<FcmToken> tokens = loadFcmPort.listActiveByMemberIds(memberIds);
        if (tokens.isEmpty()) {
            log.info("활성 FCM 토큰 없음: requestId={}, memberCount={}", event.requestId(), memberIds.size());
            return;
        }

        List<FcmSendBatchRequestedEvent> batchEvents = partition(tokenIds(tokens), FCM_MULTICAST_BATCH_SIZE)
            .stream()
            .map(tokenIds -> FcmSendBatchRequestedEvent.create(event.requestId(), tokenIds, event))
            .toList();
        eventPublisher.publishAll(batchEvents);
        log.info("FCM 발송 배치 이벤트를 발행했습니다: requestId={}, memberCount={}, tokenCount={}, batchCount={}",
            event.requestId(), memberIds.size(), tokens.size(), batchEvents.size());
    }

    private List<Long> tokenIds(List<FcmToken> tokens) {
        return tokens.stream()
            .map(FcmToken::getId)
            .toList();
    }

    private <T> List<List<T>> partition(List<T> list, int size) {
        List<List<T>> result = new ArrayList<>();
        for (int i = 0; i < list.size(); i += size) {
            result.add(list.subList(i, Math.min(i + size, list.size())));
        }
        return result;
    }
}
