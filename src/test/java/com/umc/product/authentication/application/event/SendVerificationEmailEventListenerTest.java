package com.umc.product.authentication.application.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.umc.product.global.config.notification.NotificationTransport;
import com.umc.product.global.config.notification.NotificationTransportProperties;
import com.umc.product.global.event.application.port.out.DomainEventPublisher;
import com.umc.product.global.event.domain.DomainEvent;
import com.umc.product.notification.application.port.in.SendEmailUseCase;

@ExtendWith(MockitoExtension.class)
@DisplayName("SendVerificationEmailEventListener")
class SendVerificationEmailEventListenerTest {

    @Mock
    SendEmailUseCase sendEmailUseCase;

    @Mock
    DomainEventPublisher eventPublisher;

    @Test
    @DisplayName("external 모드는 로컬 provider 대신 만료 시각을 포함한 integration event를 기록한다")
    void external_인증_메일_event_발행() {
        SendVerificationEmailEventListener listener = new SendVerificationEmailEventListener(
            sendEmailUseCase,
            eventPublisher,
            new NotificationTransportProperties(NotificationTransport.EXTERNAL),
            600
        );
        SendVerificationEmailEvent event = new SendVerificationEmailEvent(
            null,
            Instant.parse("2026-07-23T00:00:00Z"),
            "user@example.com",
            "123456"
        );

        listener.handle(event);

        then(sendEmailUseCase).should(never()).sendVerificationEmail(org.mockito.ArgumentMatchers.any());
        ArgumentCaptor<DomainEvent> captor = ArgumentCaptor.forClass(DomainEvent.class);
        then(eventPublisher).should().publish(captor.capture());
        assertThat(captor.getValue())
            .isInstanceOfSatisfying(VerificationEmailRequestedIntegrationEvent.class, integrationEvent -> {
                assertThat(integrationEvent.requestId()).isEqualTo(event.eventId());
                assertThat(integrationEvent.detail().expiresAt()).isEqualTo("2026-07-23T00:10:00Z");
            });
    }
}
