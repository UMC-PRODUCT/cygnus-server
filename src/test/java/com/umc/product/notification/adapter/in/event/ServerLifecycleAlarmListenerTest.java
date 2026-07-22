package com.umc.product.notification.adapter.in.event;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.then;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.umc.product.notification.application.port.in.SendWebhookAlarmUseCase;
import com.umc.product.notification.domain.WebhookPlatform;

@ExtendWith(MockitoExtension.class)
@DisplayName("ServerLifecycleAlarmListener")
class ServerLifecycleAlarmListenerTest {

    @Mock
    SendWebhookAlarmUseCase sendWebhookAlarmUseCase;

    @Test
    @DisplayName("애플리케이션 준비 완료 시 서버 시작 시각을 알린다")
    void 애플리케이션_준비_완료를_알린다() {
        ServerLifecycleAlarmListener sut = new ServerLifecycleAlarmListener(sendWebhookAlarmUseCase);

        sut.onApplicationReady();

        then(sendWebhookAlarmUseCase).should().send(argThat(command ->
            command.platforms().equals(java.util.List.of(WebhookPlatform.TELEGRAM, WebhookPlatform.DISCORD))
                && command.title().equals("서버 시작")
                && command.content().matches("시각: \\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}")
        ));
    }

    @Test
    @DisplayName("종료 직전 서버 종료 시각을 알린다")
    void 서버_종료를_알린다() {
        ServerLifecycleAlarmListener sut = new ServerLifecycleAlarmListener(sendWebhookAlarmUseCase);

        sut.onShutdown();

        then(sendWebhookAlarmUseCase).should().send(argThat(command ->
            command.platforms().equals(java.util.List.of(WebhookPlatform.TELEGRAM, WebhookPlatform.DISCORD))
                && command.title().equals("서버 종료")
                && command.content().matches("시각: \\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}")
        ));
    }
}
