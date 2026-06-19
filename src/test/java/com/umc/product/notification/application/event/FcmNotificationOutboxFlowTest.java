package com.umc.product.notification.application.event;

import static org.assertj.core.api.Assertions.assertThat;

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
import com.umc.product.notification.application.port.in.dto.RequestFcmNotificationCommand;
import com.umc.product.notification.application.port.out.LoadFcmPort;
import com.umc.product.notification.application.service.FcmAudienceResolver;
import com.umc.product.notification.application.service.FcmNotificationCommandService;
import com.umc.product.notification.domain.FcmToken;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;

@DisplayName("FCM 알림 event outbox flow")
class FcmNotificationOutboxFlowTest {

    @Test
    @DisplayName("FCM 요청 이벤트는 event_outbox에 저장되고 relay 후 배치 이벤트도 event_outbox에 저장된다")
    void fcm_request_event_outbox_relay_flow() {
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        EventPayloadSerializer serializer = new EventPayloadSerializer(objectMapper);
        RecordingSaveEventOutboxPort requestOutboxPort = new RecordingSaveEventOutboxPort();
        FcmNotificationCommandService service = new FcmNotificationCommandService(
            new OutboxDomainEventPublisher(requestOutboxPort, serializer)
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
            new OutboxDomainEventPublisher(batchOutboxPort, serializer)
        );
        ApplicationEventPublisher springPublisher = event -> {
            if (event instanceof FcmNotificationRequestedEvent fcmEvent) {
                listener.handle(fcmEvent);
            }
        };
        EventOutboxRelayService relayService = new EventOutboxRelayService(
            new SingleEventOutboxPort(requestOutbox),
            new RecordingSaveEventOutboxPort(),
            new EventPayloadDeserializer(objectMapper),
            springPublisher,
            new LocalTransactionManager(),
            100,
            3
        );

        relayService.relay();

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

    private static List<FcmToken> createTokens(int count) {
        List<FcmToken> tokens = new ArrayList<>();
        for (long id = 1; id <= count; id++) {
            FcmToken token = FcmToken.create(id, "token-" + id);
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
        public Optional<FcmToken> findByMemberIdAndToken(Long memberId, String fcmToken) {
            return Optional.empty();
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
            return List.of();
        }

        @Override
        public List<FcmToken> listActiveForValidation(Instant validatedBefore, int limit) {
            return List.of();
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
