package com.umc.product.notification.adapter.in.event;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.umc.product.global.config.notification.NotificationTransportProperties;
import com.umc.product.notification.application.port.in.SendWebhookAlarmUseCase;
import com.umc.product.notification.application.port.in.dto.SendWebhookAlarmCommand;
import com.umc.product.notification.domain.WebhookPlatform;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
//@Profile("!local")
public class ServerLifecycleAlarmListener {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final List<WebhookPlatform> PLATFORMS = List.of(WebhookPlatform.TELEGRAM, WebhookPlatform.DISCORD);

    private final SendWebhookAlarmUseCase sendWebhookAlarmUseCase;
    private final NotificationTransportProperties transportProperties;

    public ServerLifecycleAlarmListener(
        SendWebhookAlarmUseCase sendWebhookAlarmUseCase,
        NotificationTransportProperties transportProperties
    ) {
        this.sendWebhookAlarmUseCase = sendWebhookAlarmUseCase;
        this.transportProperties = transportProperties;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        if (!transportProperties.transport().sendsLocally()) {
            return;
        }
        String time = LocalDateTime.now().format(FORMATTER);

        SendWebhookAlarmCommand command = SendWebhookAlarmCommand.builder()
            .platforms(PLATFORMS)
            .title("서버 시작")
            .content("시각: " + time)
            .build();

        sendWebhookAlarmUseCase.send(command);
        log.info("서버 시작 알림을 전송했습니다: time={}", time);
    }

    @PreDestroy
    public void onShutdown() {
        if (!transportProperties.transport().sendsLocally()) {
            return;
        }
        String time = LocalDateTime.now().format(FORMATTER);

        SendWebhookAlarmCommand command = SendWebhookAlarmCommand.builder()
            .platforms(PLATFORMS)
            .title("서버 종료")
            .content("시각: " + time)
            .build();

        sendWebhookAlarmUseCase.send(command);
        log.info("서버 종료 알림을 전송했습니다: time={}", time);
    }
}
