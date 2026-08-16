package com.umc.product.notification.adapter.in.event;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.then;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.umc.product.notification.application.port.in.SendWebhookAlarmUseCase;
import com.umc.product.notification.domain.WebhookAlarmEvent;
import com.umc.product.notification.domain.WebhookPlatform;

@ExtendWith(MockitoExtension.class)
@DisplayName("WebhookAlarmEventListener")
class WebhookAlarmEventListenerTest {

    @Mock
    SendWebhookAlarmUseCase sendWebhookAlarmUseCase;

    @Test
    @DisplayName("WebhookAlarmEvent를 기존 send usecase로 위임한다")
    void webhookAlarmEvent를_send_usecase로_위임한다() {
        WebhookAlarmEventListener sut = new WebhookAlarmEventListener(sendWebhookAlarmUseCase);
        WebhookAlarmEvent event = WebhookAlarmEvent.of(
            List.of(WebhookPlatform.TELEGRAM),
            "테스트 알림",
            "테스트 본문"
        );

        sut.handle(event);

        then(sendWebhookAlarmUseCase).should().send(argThat(command ->
            command.platforms().equals(List.of(WebhookPlatform.TELEGRAM))
                && command.title().equals("테스트 알림")
                && command.content().equals("테스트 본문")
        ));
    }
}
