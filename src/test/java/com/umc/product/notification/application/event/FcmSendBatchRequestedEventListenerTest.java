package com.umc.product.notification.application.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.global.config.FcmProperties;
import com.umc.product.global.event.application.port.out.DomainEventPublisher;
import com.umc.product.global.event.domain.DomainEvent;
import com.umc.product.global.logging.OperationalMetrics;
import com.umc.product.notification.application.port.out.LoadFcmPort;
import com.umc.product.notification.application.port.out.SaveFcmPort;
import com.umc.product.notification.application.port.out.SendFcmMessagePort;
import com.umc.product.notification.application.port.out.dto.FcmSendRequest;
import com.umc.product.notification.application.port.out.dto.FcmSendResult;
import com.umc.product.notification.domain.FcmToken;
import com.umc.product.notification.domain.exception.FcmDomainException;
import com.umc.product.notification.domain.exception.FcmErrorCode;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

@DisplayName("FCM 배치 발송 이벤트 리스너")
class FcmSendBatchRequestedEventListenerTest {

    @Test
    @DisplayName("UNREGISTERED 결과를 받은 토큰을 비활성화한다")
    void invalid_token_비활성화() {
        // given
        FcmToken active = token(1L, 10L, "token-1");
        FcmToken invalid = token(2L, 20L, "token-2");
        FakeLoadFcmPort loadFcmPort = new FakeLoadFcmPort(List.of(active, invalid));
        FakeSaveFcmPort saveFcmPort = new FakeSaveFcmPort();
        FakeSendFcmMessagePort sendFcmMessagePort = new FakeSendFcmMessagePort(
            FcmSendResult.of(1, 1, List.of(2L), List.of())
        );
        RecordingDomainEventPublisher eventPublisher = new RecordingDomainEventPublisher();
        FcmSendBatchRequestedEventListener listener = new FcmSendBatchRequestedEventListener(
            new FcmProperties(true, true),
            loadFcmPort,
            saveFcmPort,
            sendFcmMessagePort,
            eventPublisher,
            new OperationalMetrics(new SimpleMeterRegistry())
        );
        FcmSendBatchRequestedEvent event = new FcmSendBatchRequestedEvent(
            null,
            null,
            UUID.randomUUID(),
            List.of(1L, 2L),
            "제목",
            "본문",
            Map.of(),
            null,
            null
        );

        // when
        listener.handle(event);

        // then
        assertThat(sendFcmMessagePort.lastRequest.targets()).hasSize(2);
        assertThat(invalid.isActive()).isFalse();
        assertThat(active.isActive()).isTrue();
        assertThat(saveFcmPort.saved).containsExactly(invalid);
        assertThat(saveFcmPort.saveAllCallCount).isEqualTo(1);
        assertThat(eventPublisher.events).isEmpty();
    }

    @Test
    @DisplayName("재시도 가능한 부분 실패는 실패한 토큰만 새 배치 이벤트로 발행한다")
    void retryablePartialFailurePublishesSubsetEvent() {
        // given
        FcmToken success = token(1L, 10L, "token-1");
        FcmToken retryable = token(2L, 20L, "token-2");
        FakeLoadFcmPort loadFcmPort = new FakeLoadFcmPort(List.of(success, retryable));
        RecordingDomainEventPublisher eventPublisher = new RecordingDomainEventPublisher();
        FcmSendBatchRequestedEventListener listener = new FcmSendBatchRequestedEventListener(
            new FcmProperties(true, true),
            loadFcmPort,
            new FakeSaveFcmPort(),
            new FakeSendFcmMessagePort(FcmSendResult.of(1, 1, List.of(), List.of(2L))),
            eventPublisher,
            new OperationalMetrics(new SimpleMeterRegistry())
        );
        UUID requestId = UUID.randomUUID();
        FcmSendBatchRequestedEvent event = new FcmSendBatchRequestedEvent(
            null,
            null,
            requestId,
            List.of(1L, 2L),
            "제목",
            "본문",
            Map.of("type", "NOTICE"),
            "https://example.com/image.png",
            "umc://notices/1"
        );

        // when
        listener.handle(event);

        // then
        assertThat(eventPublisher.events).singleElement().satisfies(published -> {
            FcmSendBatchRequestedEvent retryEvent = (FcmSendBatchRequestedEvent) published;
            assertThat(retryEvent.eventId()).isNotEqualTo(event.eventId());
            assertThat(retryEvent.requestId()).isEqualTo(requestId);
            assertThat(retryEvent.tokenIds()).containsExactly(2L);
            assertThat(retryEvent.title()).isEqualTo("제목");
            assertThat(retryEvent.body()).isEqualTo("본문");
            assertThat(retryEvent.data()).containsEntry("type", "NOTICE");
            assertThat(retryEvent.imageUrl()).isEqualTo("https://example.com/image.png");
            assertThat(retryEvent.deepLink()).isEqualTo("umc://notices/1");
        });
    }

    @Test
    @DisplayName("FCM 발송 transient 실패는 공용 outbox relay 재시도로 이어지도록 예외를 전파한다")
    void transient_failure_예외_전파() {
        // given
        FcmToken active = token(1L, 10L, "token-1");
        FakeLoadFcmPort loadFcmPort = new FakeLoadFcmPort(List.of(active));
        FakeSaveFcmPort saveFcmPort = new FakeSaveFcmPort();
        RecordingDomainEventPublisher eventPublisher = new RecordingDomainEventPublisher();
        FcmSendBatchRequestedEventListener listener = new FcmSendBatchRequestedEventListener(
            new FcmProperties(true, true),
            loadFcmPort,
            saveFcmPort,
            request -> {
                throw new FcmDomainException(FcmErrorCode.FCM_SEND_FAILED);
            },
            eventPublisher,
            new OperationalMetrics(new SimpleMeterRegistry())
        );
        FcmSendBatchRequestedEvent event = new FcmSendBatchRequestedEvent(
            null,
            null,
            UUID.randomUUID(),
            List.of(1L),
            "제목",
            "본문",
            Map.of(),
            null,
            null
        );

        // when & then
        assertThatThrownBy(() -> listener.handle(event))
            .isInstanceOf(FcmDomainException.class);
        assertThat(saveFcmPort.saved).isEmpty();
        assertThat(saveFcmPort.saveAllCallCount).isZero();
        assertThat(eventPublisher.events).isEmpty();
    }

    private FcmToken token(Long id, Long memberId, String value) {
        FcmToken token = FcmToken.create(memberId, "installation-" + memberId, value);
        ReflectionTestUtils.setField(token, "id", id);
        return token;
    }

    private static class FakeLoadFcmPort implements LoadFcmPort {

        private final List<FcmToken> tokens;

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
            return List.of();
        }

        @Override
        public List<FcmToken> listActiveByToken(String fcmToken) {
            return List.of();
        }

        @Override
        public List<FcmToken> listActiveByIds(List<Long> ids) {
            return tokens;
        }

        @Override
        public List<FcmToken> listActiveForValidation(Instant validatedBefore, int limit) {
            return List.of();
        }
    }

    private static class FakeSaveFcmPort implements SaveFcmPort {

        private final List<FcmToken> saved = new ArrayList<>();
        private int saveAllCallCount;

        @Override
        public void save(FcmToken newToken) {
            saved.add(newToken);
        }

        @Override
        public void saveAll(List<FcmToken> fcmTokens) {
            saveAllCallCount++;
            saved.addAll(fcmTokens);
        }
    }

    private static class FakeSendFcmMessagePort implements SendFcmMessagePort {

        private final FcmSendResult result;
        private FcmSendRequest lastRequest;

        private FakeSendFcmMessagePort(FcmSendResult result) {
            this.result = result;
        }

        @Override
        public FcmSendResult send(FcmSendRequest request) {
            this.lastRequest = request;
            return result;
        }
    }

    private static class RecordingDomainEventPublisher implements DomainEventPublisher {

        private final List<DomainEvent> events = new ArrayList<>();

        @Override
        public void publish(DomainEvent event) {
            events.add(event);
        }

        @Override
        public void publishAll(Collection<? extends DomainEvent> events) {
            this.events.addAll(events);
        }
    }
}
