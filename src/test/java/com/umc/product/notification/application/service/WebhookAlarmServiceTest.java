package com.umc.product.notification.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;

import com.umc.product.global.event.application.port.out.DomainEventPublisher;
import com.umc.product.global.event.domain.DomainEvent;
import com.umc.product.global.logging.OperationalMetrics;
import com.umc.product.notification.application.port.in.dto.SendWebhookAlarmCommand;
import com.umc.product.notification.application.port.out.SendWebhookPort;
import com.umc.product.notification.domain.WebhookAlarmEvent;
import com.umc.product.notification.domain.WebhookPlatform;

@ExtendWith(MockitoExtension.class)
@DisplayName("WebhookAlarmService")
class WebhookAlarmServiceTest {

    @Mock
    Environment environment;
    @Mock
    OperationalMetrics operationalMetrics;
    @Mock
    SendWebhookPort sendWebhookPort;

    @Test
    @DisplayName("sendBuffered는 외부 웹훅을 즉시 호출하지 않고 이벤트를 발행한다")
    void sendBuffered는_웹훅을_즉시_호출하지_않고_이벤트를_발행한다() {
        given(sendWebhookPort.platform()).willReturn(WebhookPlatform.TELEGRAM);
        given(environment.getActiveProfiles()).willReturn(new String[0]);
        CapturingDomainEventPublisher eventPublisher = new CapturingDomainEventPublisher();
        WebhookAlarmService sut = new WebhookAlarmService(
            List.of(sendWebhookPort),
            environment,
            operationalMetrics,
            eventPublisher
        );
        SendWebhookAlarmCommand command = SendWebhookAlarmCommand.builder()
            .platforms(List.of(WebhookPlatform.TELEGRAM, WebhookPlatform.DISCORD))
            .title("테스트 알림")
            .content("테스트 본문")
            .build();

        sut.sendBuffered(command);

        assertThat(eventPublisher.events()).hasSize(1);
        assertThat(eventPublisher.events().get(0))
            .isInstanceOfSatisfying(WebhookAlarmEvent.class, event -> {
                assertThat(event.platforms()).containsExactly(WebhookPlatform.TELEGRAM, WebhookPlatform.DISCORD);
                assertThat(event.title()).isEqualTo("테스트 알림");
                assertThat(event.content()).isEqualTo("테스트 본문");
            });
        then(sendWebhookPort).should(never()).send("테스트 알림", "테스트 본문");
    }

    private static class CapturingDomainEventPublisher implements DomainEventPublisher {

        private final List<DomainEvent> events = new java.util.ArrayList<>();

        @Override
        public void publish(DomainEvent event) {
            events.add(event);
        }

        @Override
        public void publishAll(java.util.Collection<? extends DomainEvent> events) {
            this.events.addAll(events);
        }

        List<DomainEvent> events() {
            return events;
        }
    }
}
