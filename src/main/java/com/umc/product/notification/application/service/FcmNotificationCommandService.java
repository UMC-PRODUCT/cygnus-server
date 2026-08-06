package com.umc.product.notification.application.service;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.global.event.application.port.out.DomainEventPublisher;
import com.umc.product.notification.application.event.FcmNotificationRequestedEvent;
import com.umc.product.notification.application.port.in.RequestFcmNotificationUseCase;
import com.umc.product.notification.application.port.in.dto.FcmNotificationRequestInfo;
import com.umc.product.notification.application.port.in.dto.RequestFcmNotificationCommand;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class FcmNotificationCommandService implements RequestFcmNotificationUseCase {

    private final DomainEventPublisher eventPublisher;

    @Override
    public FcmNotificationRequestInfo request(RequestFcmNotificationCommand command) {
        UUID requestId = UUID.randomUUID();
        Instant queuedAt = Instant.now();
        eventPublisher.publish(FcmNotificationRequestedEvent.from(requestId, queuedAt, command));
        log.info("FCM 알림 요청 이벤트를 발행했습니다: requestId={}, explicitMemberCount={}",
            requestId, command.memberIds().size());
        return FcmNotificationRequestInfo.of(requestId, queuedAt);
    }
}
