package com.umc.product.notification.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
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
    @DisplayName("활성 profile을 제목에 표시하고 플랫폼별 성공·실패·미등록 결과를 격리한다")
    void 플랫폼별_전송_결과를_격리한다() {
        SendWebhookPort failingPort = mock(SendWebhookPort.class);
        given(sendWebhookPort.platform()).willReturn(WebhookPlatform.TELEGRAM);
        given(failingPort.platform()).willReturn(WebhookPlatform.DISCORD);
        given(environment.getActiveProfiles()).willReturn(new String[]{"LOCAL", "prod"});
        RuntimeException providerFailure = new RuntimeException("provider failure");
        org.mockito.BDDMockito.willThrow(providerFailure)
            .given(failingPort).send("[Local, Prod] 배포 알림", "본문");
        DomainEventPublisher eventPublisher = mock(DomainEventPublisher.class);
        WebhookAlarmService sut = new WebhookAlarmService(
            List.of(sendWebhookPort, failingPort),
            environment,
            operationalMetrics,
            eventPublisher
        );
        SendWebhookAlarmCommand command = SendWebhookAlarmCommand.builder()
            .platforms(List.of(WebhookPlatform.TELEGRAM, WebhookPlatform.DISCORD, WebhookPlatform.SLACK))
            .title("배포 알림")
            .content("본문")
            .build();

        sut.send(command);

        then(sendWebhookPort).should().send("[Local, Prod] 배포 알림", "본문");
        then(failingPort).should().send("[Local, Prod] 배포 알림", "본문");
        then(operationalMetrics).should().recordNotification("TELEGRAM", "SEND_WEBHOOK", "success", 1);
        then(operationalMetrics).should().recordNotification("DISCORD", "SEND_WEBHOOK", "failure", 1);
        then(operationalMetrics).should().recordNotification("SLACK", "SEND_WEBHOOK", "missing_adapter", 1);
        then(eventPublisher).shouldHaveNoInteractions();
    }

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
