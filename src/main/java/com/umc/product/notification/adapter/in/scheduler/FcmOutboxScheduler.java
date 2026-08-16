package com.umc.product.notification.adapter.in.scheduler;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.umc.product.notification.application.port.in.ProcessFcmOutboxUseCase;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.fcm.enabled", havingValue = "true")
public class FcmOutboxScheduler {

    private final ProcessFcmOutboxUseCase processFcmOutboxUseCase;

    @Scheduled(fixedRateString = "${app.fcm.outbox-interval-ms}")
    public void processPendingEvents() {
        log.debug("FCM outbox 처리 스케줄 실행");
        processFcmOutboxUseCase.process();
    }
}
