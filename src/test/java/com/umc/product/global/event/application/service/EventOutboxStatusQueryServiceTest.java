package com.umc.product.global.event.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.umc.product.global.event.application.port.in.query.GetEventOutboxStatusUseCase;
import com.umc.product.global.event.application.port.in.query.dto.EventOutboxStatusInfo;
import com.umc.product.global.event.application.port.out.LoadEventOutboxPort;
import com.umc.product.global.event.domain.DomainEvent;
import com.umc.product.global.event.domain.EventOutbox;
import com.umc.product.global.event.domain.EventOutboxErrorCode;
import com.umc.product.global.event.domain.EventOutboxNotFoundException;
import com.umc.product.global.event.domain.EventOutboxStatus;

@DisplayName("EventOutboxStatusQueryService")
class EventOutboxStatusQueryServiceTest {

    private static final Instant AVAILABLE_AT = Instant.parse("2026-07-18T01:00:00.123456Z");

    @Test
    @DisplayName("eventId로 outbox 상태만 조회하고 payload 필드는 노출하지 않는다")
    void getByEventIdReturnsStatusWithoutPayload() {
        EventOutbox outbox = EventOutbox.record(
            TestEvent.create(),
            "{\"privateBusinessValue\":\"must-not-leak\"}",
            "a".repeat(64),
            AVAILABLE_AT
        );
        EventOutboxStatusQueryService service = new EventOutboxStatusQueryService(
            new FakeLoadEventOutboxPort(outbox)
        );

        EventOutboxStatusInfo info = service.getByEventId(outbox.getEventId());

        assertThat(info.eventId()).isEqualTo(outbox.getEventId());
        assertThat(info.status()).isEqualTo(EventOutboxStatus.PENDING);
        assertThat(info.attempts()).isZero();
        assertThat(info.availableAt()).isEqualTo(AVAILABLE_AT);
        assertThat(info.nextAttemptAt()).isEqualTo(AVAILABLE_AT);
        assertThat(info.leaseUntil()).isNull();
        assertThat(info.failureCode()).isNull();
        assertThat(info.publishedAt()).isNull();
    }

    @Test
    @DisplayName("없는 eventId 조회는 stable not-found error를 반환한다")
    void getByEventIdMissingThrowsStableError() {
        EventOutboxStatusQueryService service = new EventOutboxStatusQueryService(
            new FakeLoadEventOutboxPort(null)
        );

        assertThatThrownBy(() -> service.getByEventId(UUID.randomUUID()))
            .isInstanceOfSatisfying(EventOutboxNotFoundException.class, exception -> {
                assertThat(exception.getBaseCode()).isEqualTo(EventOutboxErrorCode.EVENT_NOT_FOUND);
                assertThat(exception.getBaseCode().getCode()).isEqualTo("EVENT-OUTBOX-0002");
            });
    }

    @Test
    @DisplayName("Spring proxy는 load port를 active read-only transaction 안에서 호출한다")
    void queryServiceUsesReadOnlyTransactionThroughProxy() {
        try (AnnotationConfigApplicationContext context =
                 new AnnotationConfigApplicationContext(QueryServiceTestConfiguration.class)) {
            FakeLoadEventOutboxPort loadPort = context.getBean(FakeLoadEventOutboxPort.class);
            EventOutbox outbox = EventOutbox.record(TestEvent.create(), "{}");
            loadPort.outbox = outbox;
            GetEventOutboxStatusUseCase service = context.getBean(GetEventOutboxStatusUseCase.class);

            EventOutboxStatusInfo info = service.getByEventId(outbox.getEventId());

            assertThat(info.status()).isEqualTo(EventOutboxStatus.PENDING);
            assertThat(loadPort.transactionActive).isTrue();
            assertThat(loadPort.transactionReadOnly).isTrue();
        }
    }

    private static class FakeLoadEventOutboxPort implements LoadEventOutboxPort {

        private EventOutbox outbox;
        private boolean transactionActive;
        private boolean transactionReadOnly;

        private FakeLoadEventOutboxPort(EventOutbox outbox) {
            this.outbox = outbox;
        }

        @Override
        public Optional<EventOutbox> findByEventId(UUID eventId) {
            transactionActive = TransactionSynchronizationManager.isActualTransactionActive();
            transactionReadOnly = TransactionSynchronizationManager.isCurrentTransactionReadOnly();
            if (outbox == null || !outbox.getEventId().equals(eventId)) {
                return Optional.empty();
            }
            return Optional.of(outbox);
        }

        @Override
        public List<EventOutbox> listPublishable(int limit, Instant now) {
            return List.of();
        }
    }

    @Configuration(proxyBeanMethods = false)
    @EnableTransactionManagement
    static class QueryServiceTestConfiguration {

        @Bean
        FakeLoadEventOutboxPort loadEventOutboxPort() {
            return new FakeLoadEventOutboxPort(null);
        }

        @Bean
        EventOutboxStatusQueryService eventOutboxStatusQueryService(LoadEventOutboxPort loadEventOutboxPort) {
            return new EventOutboxStatusQueryService(loadEventOutboxPort);
        }

        @Bean
        PlatformTransactionManager transactionManager() {
            return new LocalTransactionManager();
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

    private record TestEvent(UUID eventId, Instant occurredAt) implements DomainEvent {

        static TestEvent create() {
            return new TestEvent(UUID.randomUUID(), Instant.parse("2026-07-18T00:00:00Z"));
        }

        @Override
        public String eventType() {
            return "test.status.requested";
        }
    }
}
