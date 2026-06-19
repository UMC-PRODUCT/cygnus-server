package com.umc.product.notification.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.umc.product.global.event.application.port.out.DomainEventPublisher;
import com.umc.product.global.event.domain.DomainEvent;
import com.umc.product.notification.application.event.FcmNotificationRequestedEvent;
import com.umc.product.notification.application.port.in.dto.FcmNotificationRequestInfo;
import com.umc.product.notification.application.port.in.dto.RequestFcmNotificationCommand;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("FCM 알림 요청 서비스")
class FcmNotificationCommandServiceTest {

    @Test
    @DisplayName("요청을 직접 발송하지 않고 FCM 요청 이벤트로 발행한다")
    void request_이벤트_발행() {
        // given
        FakeDomainEventPublisher eventPublisher = new FakeDomainEventPublisher();
        FcmNotificationCommandService service = new FcmNotificationCommandService(eventPublisher);
        RequestFcmNotificationCommand command = RequestFcmNotificationCommand.builder()
            .memberIds(List.of(1L, 1L, 2L))
            .title("공지")
            .body("본문")
            .build();

        // when
        FcmNotificationRequestInfo info = service.request(command);

        // then
        assertThat(info.requestId()).isNotNull();
        assertThat(info.queuedAt()).isNotNull();
        assertThat(eventPublisher.published).hasSize(1);
        FcmNotificationRequestedEvent event = (FcmNotificationRequestedEvent) eventPublisher.published.getFirst();
        assertThat(event.eventType()).isEqualTo("notification.fcm.requested");
        assertThat(event.requestId()).isEqualTo(info.requestId());
        assertThat(event.memberIds()).containsExactly(1L, 2L);
        assertThat(event.title()).isEqualTo("공지");
        assertThat(event.body()).isEqualTo("본문");
    }

    private static class FakeDomainEventPublisher implements DomainEventPublisher {

        private final List<DomainEvent> published = new ArrayList<>();

        @Override
        public void publish(DomainEvent event) {
            published.add(event);
        }

        @Override
        public void publishAll(Collection<? extends DomainEvent> events) {
            published.addAll(events);
        }
    }
}
