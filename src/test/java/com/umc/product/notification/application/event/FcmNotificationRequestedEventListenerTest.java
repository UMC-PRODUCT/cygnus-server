package com.umc.product.notification.application.event;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.global.config.FcmProperties;
import com.umc.product.global.event.application.port.out.DomainEventPublisher;
import com.umc.product.global.event.domain.DomainEvent;
import com.umc.product.notification.application.port.out.LoadFcmPort;
import com.umc.product.notification.application.service.FcmAudienceResolver;
import com.umc.product.notification.domain.FcmToken;

@DisplayName("FCM 알림 요청 이벤트 리스너")
class FcmNotificationRequestedEventListenerTest {

    @Test
    @DisplayName("FCM이 비활성화되면 audience와 토큰을 조회하지 않는다")
    void disabled_skips_request() {
        FakeLoadFcmPort loadFcmPort = new FakeLoadFcmPort(createTokens(1));
        FakeDomainEventPublisher eventPublisher = new FakeDomainEventPublisher();
        FcmNotificationRequestedEventListener listener = new FcmNotificationRequestedEventListener(
            new FcmProperties(false, true),
            new FcmAudienceResolver(null, null, null),
            loadFcmPort,
            eventPublisher
        );

        listener.handle(event(List.of(1L)));

        assertThat(eventPublisher.published).isEmpty();
        assertThat(loadFcmPort.called).isFalse();
    }

    @Test
    @DisplayName("해석된 audience가 비어 있으면 토큰을 조회하지 않는다")
    void empty_audience_skips_token_lookup() {
        FakeLoadFcmPort loadFcmPort = new FakeLoadFcmPort(createTokens(1));
        FakeDomainEventPublisher eventPublisher = new FakeDomainEventPublisher();
        FcmNotificationRequestedEventListener listener = new FcmNotificationRequestedEventListener(
            new FcmProperties(true, true),
            new FcmAudienceResolver(null, null, null),
            loadFcmPort,
            eventPublisher
        );

        listener.handle(event(List.of()));

        assertThat(eventPublisher.published).isEmpty();
        assertThat(loadFcmPort.called).isFalse();
    }

    @Test
    @DisplayName("audience에 활성 토큰이 없으면 batch event를 발행하지 않는다")
    void no_active_token_skips_batch_event() {
        FakeLoadFcmPort loadFcmPort = new FakeLoadFcmPort(List.of());
        FakeDomainEventPublisher eventPublisher = new FakeDomainEventPublisher();
        FcmNotificationRequestedEventListener listener = new FcmNotificationRequestedEventListener(
            new FcmProperties(true, true),
            new FcmAudienceResolver(null, null, null),
            loadFcmPort,
            eventPublisher
        );

        listener.handle(event(List.of(1L)));

        assertThat(eventPublisher.published).isEmpty();
        assertThat(loadFcmPort.called).isTrue();
    }

    @Test
    @DisplayName("활성 토큰을 500개 단위 배치 이벤트로 분리한다")
    void batch_event_분리() {
        // given
        FakeLoadFcmPort loadFcmPort = new FakeLoadFcmPort(createTokens(501));
        FakeDomainEventPublisher eventPublisher = new FakeDomainEventPublisher();
        FcmNotificationRequestedEventListener listener = new FcmNotificationRequestedEventListener(
            new FcmProperties(true, true),
            new FcmAudienceResolver(null, null, null),
            loadFcmPort,
            eventPublisher
        );
        FcmNotificationRequestedEvent event = new FcmNotificationRequestedEvent(
            null,
            null,
            UUID.randomUUID(),
            null,
            List.of(1L, 2L),
            null,
            null,
            null,
            null,
            "제목",
            "본문",
            null,
            null,
            null
        );

        // when
        listener.handle(event);

        // then
        assertThat(eventPublisher.published).hasSize(2);
        assertThat((FcmSendBatchRequestedEvent) eventPublisher.published.get(0))
            .extracting(batch -> batch.tokenIds().size())
            .isEqualTo(500);
        assertThat((FcmSendBatchRequestedEvent) eventPublisher.published.get(1))
            .extracting(batch -> batch.tokenIds().size())
            .isEqualTo(1);
    }

    private List<FcmToken> createTokens(int count) {
        List<FcmToken> tokens = new ArrayList<>();
        for (long id = 1; id <= count; id++) {
            FcmToken token = FcmToken.create(id, "installation-" + id, "token-" + id);
            ReflectionTestUtils.setField(token, "id", id);
            tokens.add(token);
        }
        return tokens;
    }

    private FcmNotificationRequestedEvent event(List<Long> memberIds) {
        return new FcmNotificationRequestedEvent(
            null,
            null,
            UUID.randomUUID(),
            null,
            memberIds,
            null,
            null,
            null,
            null,
            "제목",
            "본문",
            null,
            null,
            null
        );
    }

    private static class FakeLoadFcmPort implements LoadFcmPort {

        private final List<FcmToken> tokens;
        private boolean called;

        private FakeLoadFcmPort(List<FcmToken> tokens) {
            this.tokens = tokens;
        }

        @Override
        public Optional<FcmToken> findByInstallationIdForUpdate(String installationId) {
            return tokens.stream().filter(token -> token.isInstalledAs(installationId)).findFirst();
        }

        @Override
        public List<FcmToken> listActiveByMemberId(Long memberId) {
            return List.of();
        }

        @Override
        public List<FcmToken> listActiveByMemberIds(List<Long> memberIds) {
            called = true;
            return tokens;
        }

        @Override
        public List<FcmToken> listActiveByToken(String fcmToken) {
            return List.of();
        }

        @Override
        public List<FcmToken> listActiveByIds(List<Long> ids) {
            return List.of();
        }

        @Override
        public List<FcmToken> listActiveForValidation(Instant validatedBefore, int limit) {
            return List.of();
        }
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
