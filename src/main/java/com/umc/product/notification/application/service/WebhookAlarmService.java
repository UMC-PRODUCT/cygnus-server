package com.umc.product.notification.application.service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import com.umc.product.global.event.application.port.out.DomainEventPublisher;
import com.umc.product.global.logging.OperationalMetrics;
import com.umc.product.notification.application.port.in.SendWebhookAlarmUseCase;
import com.umc.product.notification.application.port.in.dto.SendWebhookAlarmCommand;
import com.umc.product.notification.application.port.out.SendWebhookPort;
import com.umc.product.notification.domain.WebhookAlarmEvent;
import com.umc.product.notification.domain.WebhookPlatform;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class WebhookAlarmService implements SendWebhookAlarmUseCase {

    private final Map<WebhookPlatform, SendWebhookPort> adapterMap;
    private final String profilePrefix;
    private final OperationalMetrics operationalMetrics;
    private final DomainEventPublisher eventPublisher;

    public WebhookAlarmService(
        List<SendWebhookPort> adapters,
        Environment environment,
        OperationalMetrics operationalMetrics,
        DomainEventPublisher eventPublisher
    ) {
        this.adapterMap = adapters.stream()
            .collect(Collectors.toMap(SendWebhookPort::platform, Function.identity()));
        this.profilePrefix = buildProfilePrefix(environment);
        this.operationalMetrics = operationalMetrics;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public void send(SendWebhookAlarmCommand command) {
        String prefixedTitle = profilePrefix + command.title();
        for (WebhookPlatform platform : command.platforms()) {
            trySend(platform, prefixedTitle, command.content());
        }
    }

    @Override
    public void sendBuffered(SendWebhookAlarmCommand command) {
        eventPublisher.publish(WebhookAlarmEvent.of(command.platforms(), command.title(), command.content()));
        log.debug("웹훅 알람 이벤트 발행: platforms={}, contentLength={}",
            command.platforms(), command.content().length());
    }

    private void trySend(WebhookPlatform platform, String title, String content) {
        SendWebhookPort adapter = adapterMap.get(platform);
        if (adapter == null) {
            log.warn("웹훅 어댑터가 등록되지 않았습니다: platform={}", platform);
            operationalMetrics.recordNotification(platform.name(), "SEND_WEBHOOK", "missing_adapter", 1);
            return;
        }

        try {
            adapter.send(title, content);
            operationalMetrics.recordNotification(platform.name(), "SEND_WEBHOOK", "success", 1);
            log.info("웹훅 알람을 전송했습니다: platform={}", platform);
        } catch (Exception e) {
            operationalMetrics.recordNotification(platform.name(), "SEND_WEBHOOK", "failure", 1);
            log.warn("웹훅 알람 전송 실패: platform={}, error={}", platform, e.getMessage(), e);
        }
    }

    private static String buildProfilePrefix(Environment environment) {
        String[] profiles = environment.getActiveProfiles();
        if (profiles.length == 0) {
            return "[Default] ";
        }
        String capitalized = Arrays.stream(profiles)
            .map(p -> p.substring(0, 1).toUpperCase() + p.substring(1).toLowerCase())
            .collect(Collectors.joining(", "));
        return "[" + capitalized + "] ";
    }
}
