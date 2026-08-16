package com.umc.product.notification.adapter.in.event;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.umc.product.notification.application.port.in.SendWebhookAlarmUseCase;
import com.umc.product.notification.application.port.in.dto.SendWebhookAlarmCommand;
import com.umc.product.notification.domain.WebhookAlarmEvent;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class WebhookAlarmEventListener {

    private final SendWebhookAlarmUseCase sendWebhookAlarmUseCase;

    @Async("webhookTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void handle(WebhookAlarmEvent event) {
        sendWebhookAlarmUseCase.send(
            SendWebhookAlarmCommand.builder()
                .platforms(event.platforms())
                .title(event.title())
                .content(event.content())
                .build()
        );
    }
}
