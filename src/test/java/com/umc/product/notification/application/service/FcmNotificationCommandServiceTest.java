package com.umc.product.notification.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.LongStream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.umc.product.global.config.notification.NotificationTransport;
import com.umc.product.global.config.notification.NotificationTransportProperties;
import com.umc.product.global.event.application.port.out.DomainEventPublisher;
import com.umc.product.global.event.domain.DomainEvent;
import com.umc.product.notification.application.event.FcmNotificationRequestedEvent;
import com.umc.product.notification.application.event.FcmNotificationRequestedIntegrationEvent;
import com.umc.product.notification.application.port.in.dto.FcmNotificationRequestInfo;
import com.umc.product.notification.application.port.in.dto.RequestFcmNotificationCommand;

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

    @Test
    @DisplayName("external 모드는 producer가 확정한 member ID를 500명씩 나눠 integration event로 발행한다")
    void request_external_500명_chunk_발행() {
        FakeDomainEventPublisher eventPublisher = new FakeDomainEventPublisher();
        FcmAudienceResolver audienceResolver = mock(FcmAudienceResolver.class);
        FcmNotificationCommandService service = new FcmNotificationCommandService(
            eventPublisher,
            audienceResolver,
            new NotificationTransportProperties(NotificationTransport.EXTERNAL)
        );
        List<Long> memberIds = LongStream.rangeClosed(1, 1001).boxed().toList();
        given(audienceResolver.resolve(org.mockito.ArgumentMatchers.any())).willReturn(memberIds);
        RequestFcmNotificationCommand command = RequestFcmNotificationCommand.builder()
            .targetGisuId(1L)
            .title("공지")
            .body("본문")
            .build();

        FcmNotificationRequestInfo info = service.request(command);

        assertThat(eventPublisher.published)
            .hasSize(3)
            .allSatisfy(event -> assertThat(event)
                .isInstanceOf(FcmNotificationRequestedIntegrationEvent.class));
        List<FcmNotificationRequestedIntegrationEvent> events = eventPublisher.published.stream()
            .map(FcmNotificationRequestedIntegrationEvent.class::cast)
            .toList();
        assertThat(events).extracting(event -> event.detail().memberIds().size())
            .containsExactly(500, 500, 1);
        assertThat(events).extracting(event -> event.detail().chunkIndex())
            .containsExactly(0, 1, 2);
        assertThat(events).allSatisfy(event -> {
            assertThat(event.detail().chunkCount()).isEqualTo(3);
            assertThat(event.requestId()).isEqualTo(info.requestId());
        });
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
