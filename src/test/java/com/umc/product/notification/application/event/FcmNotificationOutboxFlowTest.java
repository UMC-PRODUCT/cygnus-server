package com.umc.product.notification.application.event;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.umc.product.global.config.FcmProperties;
import com.umc.product.global.event.adapter.out.EventPayloadDeserializer;
import com.umc.product.global.event.adapter.out.EventPayloadSerializer;
import com.umc.product.global.event.adapter.out.OutboxDomainEventPublisher;
import com.umc.product.global.event.application.port.out.LoadEventOutboxPort;
import com.umc.product.global.event.application.port.out.SaveEventOutboxPort;
import com.umc.product.global.event.application.service.EventOutboxRelayService;
import com.umc.product.global.event.domain.EventOutbox;
import com.umc.product.global.event.domain.EventOutboxStatus;
import com.umc.product.global.logging.OperationalMetrics;
import com.umc.product.notification.application.port.in.dto.RequestFcmNotificationCommand;
import com.umc.product.notification.application.port.out.LoadFcmPort;
import com.umc.product.notification.application.port.out.SaveFcmPort;
import com.umc.product.notification.application.port.out.dto.FcmSendResult;
import com.umc.product.notification.application.service.FcmAudienceResolver;
import com.umc.product.notification.application.service.FcmNotificationCommandService;
import com.umc.product.notification.domain.FcmToken;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.tracing.Tracer;

@DisplayName("FCM 알림 event outbox flow")
class FcmNotificationOutboxFlowTest {

    @Test
    @DisplayName("FCM 요청 이벤트는 event_outbox에 저장되고 relay 후 배치 이벤트도 event_outbox에 저장된다")
    void fcm_request_event_outbox_relay_flow() {
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        EventPayloadSerializer serializer = new EventPayloadSerializer(objectMapper);
        RecordingSaveEventOutboxPort requestOutboxPort = new RecordingSaveEventOutboxPort();
        FcmNotificationCommandService service = new FcmNotificationCommandService(
            new OutboxDomainEventPublisher(requestOutboxPort, serializer, Tracer.NOOP)
        );

        service.request(RequestFcmNotificationCommand.builder()
            .memberIds(List.of(1L, 2L))
            .title("공지")
            .body("본문")
            .build());

        assertThat(requestOutboxPort.saved).hasSize(1);
        EventOutbox requestOutbox = requestOutboxPort.saved.getFirst();
        assertThat(requestOutbox.getEventType()).isEqualTo("notification.fcm.requested");
        assertThat(requestOutbox.getEventClass()).isEqualTo(FcmNotificationRequestedEvent.class.getName());
        assertThat(requestOutbox.getPayload()).contains("\"memberIds\":[1,2]");

        RecordingSaveEventOutboxPort batchOutboxPort = new RecordingSaveEventOutboxPort();
        FcmNotificationRequestedEventListener listener = new FcmNotificationRequestedEventListener(
            new FcmProperties(true, true),
            new FcmAudienceResolver(null, null, null),
            new FakeLoadFcmPort(createTokens(501)),
            new OutboxDomainEventPublisher(batchOutboxPort, serializer, Tracer.NOOP)
        );
        AtomicBoolean transactionActiveDuringListener = new AtomicBoolean(false);
        ApplicationEventPublisher springPublisher = event -> {
            if (event instanceof FcmNotificationRequestedEvent fcmEvent) {
                transactionActiveDuringListener.set(TransactionSynchronizationManager.isActualTransactionActive());
                listener.handle(fcmEvent);
            }
        };
        EventOutboxRelayService relayService = new EventOutboxRelayService(
            new SingleEventOutboxPort(requestOutbox),
            new RecordingSaveEventOutboxPort(),
            new EventPayloadDeserializer(objectMapper),
            springPublisher,
            new LocalTransactionManager(),
            new DefaultListableBeanFactory().getBeanProvider(Tracer.class),
            100,
            3
        );

        relayService.relay();

        assertThat(transactionActiveDuringListener).isTrue();
        assertThat(requestOutbox.getStatus()).isEqualTo(EventOutboxStatus.PUBLISHED);
        assertThat(batchOutboxPort.saved)
            .hasSize(2)
            .extracting(EventOutbox::getEventType)
            .containsOnly("notification.fcm.batch.requested");
        assertThat(batchOutboxPort.saved)
            .extracting(EventOutbox::getEventClass)
            .containsOnly(FcmSendBatchRequestedEvent.class.getName());
        assertThat(batchOutboxPort.saved.getFirst().getPayload()).contains("\"tokenIds\":[1,2");
        assertThat(batchOutboxPort.saved.get(1).getPayload()).contains("\"tokenIds\":[501]");
    }

    @Test
    @DisplayName("FCM 배치 부분 실패는 relay 트랜잭션 밖에서 실패 토큰만 새 outbox에 저장한다")
    void fcm_batch_delivery_runs_without_relay_transaction() {
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        EventPayloadSerializer serializer = new EventPayloadSerializer(objectMapper);
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
        EventOutbox outbox = EventOutbox.record(event, serializer.serialize(event));
        AtomicBoolean transactionActiveDuringSend = new AtomicBoolean(true);
        RecordingSaveEventOutboxPort retryOutboxPort = new RecordingSaveEventOutboxPort();
        FcmSendBatchRequestedEventListener listener = new FcmSendBatchRequestedEventListener(
            new FcmProperties(true, true),
            new FakeLoadFcmPort(createTokens(2)),
            new NoopSaveFcmPort(),
            request -> {
                transactionActiveDuringSend.set(TransactionSynchronizationManager.isActualTransactionActive());
                return FcmSendResult.of(1, 1, List.of(), List.of(2L));
            },
            new OutboxDomainEventPublisher(retryOutboxPort, serializer, Tracer.NOOP),
            new OperationalMetrics(new SimpleMeterRegistry())
        );
        EventOutboxRelayService relayService = new EventOutboxRelayService(
            new SingleEventOutboxPort(outbox),
            new RecordingSaveEventOutboxPort(),
            new EventPayloadDeserializer(objectMapper),
            publishedEvent -> listener.handle((FcmSendBatchRequestedEvent) publishedEvent),
            new LocalTransactionManager(),
            new DefaultListableBeanFactory().getBeanProvider(Tracer.class),
            100,
            3
        );

        relayService.relay();

        assertThat(transactionActiveDuringSend).isFalse();
        assertThat(outbox.getStatus()).isEqualTo(EventOutboxStatus.PUBLISHED);
        assertThat(retryOutboxPort.saved).singleElement().satisfies(retryOutbox -> {
            assertThat(retryOutbox.getEventType()).isEqualTo("notification.fcm.batch.requested");
            assertThat(retryOutbox.getPayload()).contains("\"tokenIds\":[2]");
        });
    }

    private static List<FcmToken> createTokens(int count) {
        List<FcmToken> tokens = new ArrayList<>();
        for (long id = 1; id <= count; id++) {
            FcmToken token = FcmToken.create(id, "installation-" + id, "token-" + id);
            ReflectionTestUtils.setField(token, "id", id);
            tokens.add(token);
        }
        return tokens;
    }

    private static class SingleEventOutboxPort implements LoadEventOutboxPort {

        private final EventOutbox eventOutbox;

        private SingleEventOutboxPort(EventOutbox eventOutbox) {
            this.eventOutbox = eventOutbox;
        }

        @Override
        public List<EventOutbox> listPublishable(int limit, Instant now) {
            return List.of(eventOutbox);
        }
    }

    private static class RecordingSaveEventOutboxPort implements SaveEventOutboxPort {

        private final List<EventOutbox> saved = new ArrayList<>();

        @Override
        public void save(EventOutbox eventOutbox) {
            saved.add(eventOutbox);
        }

        @Override
        public void saveAll(Collection<EventOutbox> eventOutboxes) {
            saved.addAll(eventOutboxes);
        }
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
            return tokens;
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

    private static class NoopSaveFcmPort implements SaveFcmPort {

        @Override
        public void save(FcmToken fcmToken) {
        }
    }

    private static class LocalTransactionManager extends AbstractPlatformTransactionManager {

        @Override
        protected Object doGetTransaction() throws TransactionException {
            return new Object();
        }

        @Override
        protected void doBegin(Object transaction, TransactionDefinition definition) throws TransactionException {
        }

        @Override
        protected void doCommit(DefaultTransactionStatus status) throws TransactionException {
        }

        @Override
        protected void doRollback(DefaultTransactionStatus status) throws TransactionException {
        }

        @Override
        protected boolean isExistingTransaction(Object transaction) throws TransactionException {
            return false;
        }
    }
}
