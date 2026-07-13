package com.umc.product.global.event.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.umc.product.global.event.adapter.out.EventPayloadDeserializer;
import com.umc.product.global.event.adapter.out.EventPayloadSerializer;
import com.umc.product.global.event.application.port.out.LoadEventOutboxPort;
import com.umc.product.global.event.application.port.out.SaveEventOutboxPort;
import com.umc.product.global.event.domain.DomainEvent;
import com.umc.product.global.event.domain.EventOutbox;
import com.umc.product.global.event.domain.EventOutboxStatus;
import com.umc.product.global.event.domain.OutboxDispatchMode;

import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.test.simple.SimpleSpan;
import io.micrometer.tracing.test.simple.SimpleTracer;

@DisplayName("EventOutboxRelayService")
class EventOutboxRelayServiceTest {

    @Test
    @DisplayName("publishable outbox를 DomainEvent로 복원해 Spring event bus로 발행하고 published 처리한다")
    void relay_성공() {
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        EventPayloadSerializer serializer = new EventPayloadSerializer(objectMapper);
        TestEvent event = TestEvent.create("test.created", "hello");
        EventOutbox outbox = EventOutbox.record(event, serializer.serialize(event));
        FakeLoadEventOutboxPort loadPort = new FakeLoadEventOutboxPort(List.of(outbox));
        FakeSaveEventOutboxPort savePort = new FakeSaveEventOutboxPort();
        CapturingApplicationEventPublisher publisher = new CapturingApplicationEventPublisher();
        EventOutboxRelayService relayService = new EventOutboxRelayService(
            loadPort,
            savePort,
            new EventPayloadDeserializer(objectMapper),
            publisher,
            new LocalTransactionManager(),
            Tracer.NOOP,
            100,
            3
        );

        relayService.relay();

        assertThat(outbox.getStatus()).isEqualTo(EventOutboxStatus.PUBLISHED);
        assertThat(publisher.events).hasSize(1);
        assertThat(publisher.events.getFirst()).isInstanceOf(TestEvent.class);
        assertThat(((TestEvent) publisher.events.getFirst()).message()).isEqualTo("hello");
        assertThat(savePort.savedStatuses).contains(EventOutboxStatus.PROCESSING, EventOutboxStatus.PUBLISHED);
    }

    @Test
    @DisplayName("이벤트 복원 또는 발행 실패 시 별도 상태 저장 트랜잭션에서 attempts를 증가시키고 pending으로 남긴다")
    void relay_실패_재시도() {
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        EventPayloadSerializer serializer = new EventPayloadSerializer(objectMapper);
        TestEvent event = TestEvent.create("test.created", "hello");
        EventOutbox outbox = EventOutbox.record(event, serializer.serialize(event));
        FakeLoadEventOutboxPort loadPort = new FakeLoadEventOutboxPort(List.of(outbox));
        FakeSaveEventOutboxPort savePort = new FakeSaveEventOutboxPort();
        ApplicationEventPublisher publisher = ignored -> {
            throw new IllegalStateException("publish failed");
        };
        EventOutboxRelayService relayService = new EventOutboxRelayService(
            loadPort,
            savePort,
            new EventPayloadDeserializer(objectMapper),
            publisher,
            new LocalTransactionManager(),
            Tracer.NOOP,
            100,
            3
        );

        relayService.relay();

        assertThat(outbox.getStatus()).isEqualTo(EventOutboxStatus.PENDING);
        assertThat(outbox.getAttempts()).isEqualTo(1);
        assertThat(outbox.getLastError()).contains("publish failed");
        assertThat(savePort.savedStatuses).contains(EventOutboxStatus.PROCESSING, EventOutboxStatus.PENDING);
    }

    @Test
    @DisplayName("최대 재시도 횟수에 도달하면 failed 상태로 저장한다")
    void relay_최대_재시도_도달() {
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        EventPayloadSerializer serializer = new EventPayloadSerializer(objectMapper);
        TestEvent event = TestEvent.create("test.created", "hello");
        EventOutbox outbox = EventOutbox.record(event, serializer.serialize(event));
        outbox.recordFailure("previous", Instant.now(), 2);
        FakeLoadEventOutboxPort loadPort = new FakeLoadEventOutboxPort(List.of(outbox));
        FakeSaveEventOutboxPort savePort = new FakeSaveEventOutboxPort();
        ApplicationEventPublisher publisher = ignored -> {
            throw new IllegalStateException("publish failed");
        };
        EventOutboxRelayService relayService = new EventOutboxRelayService(
            loadPort,
            savePort,
            new EventPayloadDeserializer(objectMapper),
            publisher,
            new LocalTransactionManager(),
            Tracer.NOOP,
            100,
            2
        );

        relayService.relay();

        assertThat(outbox.getStatus()).isEqualTo(EventOutboxStatus.FAILED);
        assertThat(outbox.getAttempts()).isEqualTo(2);
        assertThat(savePort.savedStatuses).contains(EventOutboxStatus.PROCESSING, EventOutboxStatus.FAILED);
    }

    @Test
    @DisplayName("저장된 traceparent가 있으면 relay 처리 span에 원 요청 trace로의 span link를 부착한다")
    void relay_span_link_부착() {
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        EventPayloadSerializer serializer = new EventPayloadSerializer(objectMapper);
        TestEvent event = TestEvent.create("test.created", "hello");
        String traceId = "0af7651916cd43dd8448eb211c80319c";
        String spanId = "b7ad6b7169203331";
        String traceparent = "00-" + traceId + "-" + spanId + "-01";
        EventOutbox outbox = EventOutbox.record(event, serializer.serialize(event), traceparent);
        FakeLoadEventOutboxPort loadPort = new FakeLoadEventOutboxPort(List.of(outbox));
        FakeSaveEventOutboxPort savePort = new FakeSaveEventOutboxPort();
        SimpleTracer tracer = new SimpleTracer();
        EventOutboxRelayService relayService = new EventOutboxRelayService(
            loadPort,
            savePort,
            new EventPayloadDeserializer(objectMapper),
            new CapturingApplicationEventPublisher(),
            new LocalTransactionManager(),
            tracer,
            100,
            3
        );

        relayService.relay();

        SimpleSpan relaySpan = tracer.getSpans().stream()
            .filter(span -> "outbox.relay.publish".equals(span.getName()))
            .findFirst()
            .orElseThrow();
        assertThat(relaySpan.getLinks())
            .extracting(link -> link.getTraceContext().traceId())
            .containsExactly(traceId);
        assertThat(outbox.getStatus()).isEqualTo(EventOutboxStatus.PUBLISHED);
    }

    @Test
    @DisplayName("traceparent가 없으면 link 없이 relay 처리 span만 생성하고 정상 발행한다")
    void relay_traceparent_없음_link_미부착() {
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        EventPayloadSerializer serializer = new EventPayloadSerializer(objectMapper);
        TestEvent event = TestEvent.create("test.created", "hello");
        EventOutbox outbox = EventOutbox.record(event, serializer.serialize(event));
        FakeLoadEventOutboxPort loadPort = new FakeLoadEventOutboxPort(List.of(outbox));
        FakeSaveEventOutboxPort savePort = new FakeSaveEventOutboxPort();
        SimpleTracer tracer = new SimpleTracer();
        EventOutboxRelayService relayService = new EventOutboxRelayService(
            loadPort,
            savePort,
            new EventPayloadDeserializer(objectMapper),
            new CapturingApplicationEventPublisher(),
            new LocalTransactionManager(),
            tracer,
            100,
            3
        );

        relayService.relay();

        SimpleSpan relaySpan = tracer.getSpans().stream()
            .filter(span -> "outbox.relay.publish".equals(span.getName()))
            .findFirst()
            .orElseThrow();
        assertThat(relaySpan.getLinks()).isEmpty();
        assertThat(outbox.getStatus()).isEqualTo(EventOutboxStatus.PUBLISHED);
    }

    @Test
    @DisplayName("non-transactional listener 성공 후 published 저장이 실패하면 재시도 대상(PENDING)으로 남긴다")
    void relay_published_저장_실패_재시도() {
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        EventPayloadSerializer serializer = new EventPayloadSerializer(objectMapper);
        NonTransactionalTestEvent event = NonTransactionalTestEvent.create("test.external.created", "hello");
        EventOutbox outbox = EventOutbox.record(event, serializer.serialize(event));
        FakeLoadEventOutboxPort loadPort = new FakeLoadEventOutboxPort(List.of(outbox));
        FailOnPublishedSaveEventOutboxPort savePort = new FailOnPublishedSaveEventOutboxPort();
        CapturingApplicationEventPublisher publisher = new CapturingApplicationEventPublisher();
        EventOutboxRelayService relayService = new EventOutboxRelayService(
            loadPort,
            savePort,
            new EventPayloadDeserializer(objectMapper),
            publisher,
            new LocalTransactionManager(),
            Tracer.NOOP,
            100,
            3
        );

        relayService.relay();

        assertThat(outbox.getStatus()).isEqualTo(EventOutboxStatus.PENDING);
        assertThat(outbox.getAttempts()).isEqualTo(1);
        assertThat(publisher.events).hasSize(1);
        assertThat(savePort.savedStatuses).contains(EventOutboxStatus.PROCESSING, EventOutboxStatus.PENDING);
    }

    @Test
    @DisplayName("non-transactional 이벤트는 listener 실행 후 별도 트랜잭션으로 published 처리한다")
    void relay_non_transactional_dispatch() {
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        EventPayloadSerializer serializer = new EventPayloadSerializer(objectMapper);
        NonTransactionalTestEvent event = NonTransactionalTestEvent.create("test.external.created", "hello");
        EventOutbox outbox = EventOutbox.record(event, serializer.serialize(event));
        FakeSaveEventOutboxPort savePort = new FakeSaveEventOutboxPort();
        AtomicBoolean transactionActiveDuringPublish = new AtomicBoolean(true);
        ApplicationEventPublisher publisher = ignored ->
            transactionActiveDuringPublish.set(TransactionSynchronizationManager.isActualTransactionActive());
        EventOutboxRelayService relayService = new EventOutboxRelayService(
            new FakeLoadEventOutboxPort(List.of(outbox)),
            savePort,
            new EventPayloadDeserializer(objectMapper),
            publisher,
            new LocalTransactionManager(),
            Tracer.NOOP,
            100,
            3
        );

        relayService.relay();

        assertThat(transactionActiveDuringPublish).isFalse();
        assertThat(outbox.getStatus()).isEqualTo(EventOutboxStatus.PUBLISHED);
        assertThat(savePort.savedStatuses).contains(EventOutboxStatus.PROCESSING, EventOutboxStatus.PUBLISHED);
    }

    @Test
    @DisplayName("non-transactional listener 예외는 attempts를 증가시키고 pending 재시도로 연결한다")
    void relay_non_transactional_listener_failure() {
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        EventPayloadSerializer serializer = new EventPayloadSerializer(objectMapper);
        NonTransactionalTestEvent event = NonTransactionalTestEvent.create("test.external.created", "hello");
        EventOutbox outbox = EventOutbox.record(event, serializer.serialize(event));
        FakeSaveEventOutboxPort savePort = new FakeSaveEventOutboxPort();
        ApplicationEventPublisher publisher = ignored -> {
            throw new IllegalStateException("external call failed");
        };
        EventOutboxRelayService relayService = new EventOutboxRelayService(
            new FakeLoadEventOutboxPort(List.of(outbox)),
            savePort,
            new EventPayloadDeserializer(objectMapper),
            publisher,
            new LocalTransactionManager(),
            Tracer.NOOP,
            100,
            3
        );

        relayService.relay();

        assertThat(outbox.getStatus()).isEqualTo(EventOutboxStatus.PENDING);
        assertThat(outbox.getAttempts()).isEqualTo(1);
        assertThat(outbox.getLastError()).contains("external call failed");
        assertThat(savePort.savedStatuses).contains(EventOutboxStatus.PROCESSING, EventOutboxStatus.PENDING);
    }

    @Test
    @DisplayName("lease 소유권을 잃은 worker는 published 상태 저장 실패를 재시도로 덮어쓰지 않는다")
    void relay_optimistic_lock_failure_does_not_overwrite_new_owner() {
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        EventPayloadSerializer serializer = new EventPayloadSerializer(objectMapper);
        NonTransactionalTestEvent event = NonTransactionalTestEvent.create("test.external.created", "hello");
        EventOutbox outbox = EventOutbox.record(event, serializer.serialize(event));
        LoseLeaseOnPublishedSaveEventOutboxPort savePort = new LoseLeaseOnPublishedSaveEventOutboxPort();
        CapturingApplicationEventPublisher publisher = new CapturingApplicationEventPublisher();
        EventOutboxRelayService relayService = new EventOutboxRelayService(
            new FakeLoadEventOutboxPort(List.of(outbox)),
            savePort,
            new EventPayloadDeserializer(objectMapper),
            publisher,
            new LocalTransactionManager(),
            Tracer.NOOP,
            100,
            3
        );

        relayService.relay();

        assertThat(publisher.events).hasSize(1);
        assertThat(savePort.savedStatuses).containsExactly(EventOutboxStatus.PROCESSING);
    }

    private static class FailOnPublishedSaveEventOutboxPort implements SaveEventOutboxPort {

        private final List<EventOutboxStatus> savedStatuses = new ArrayList<>();

        @Override
        public void save(EventOutbox eventOutbox) {
            // published 상태 저장(= markPublished 영속화)만 실패시켜, 발행 단위 트랜잭션 실패를 재현한다.
            if (eventOutbox.getStatus() == EventOutboxStatus.PUBLISHED) {
                throw new IllegalStateException("published 저장 실패");
            }
            savedStatuses.add(eventOutbox.getStatus());
        }

        @Override
        public void saveAll(Collection<EventOutbox> eventOutboxes) {
            eventOutboxes.forEach(eventOutbox -> savedStatuses.add(eventOutbox.getStatus()));
        }
    }

    private static class LoseLeaseOnPublishedSaveEventOutboxPort implements SaveEventOutboxPort {

        private final List<EventOutboxStatus> savedStatuses = new ArrayList<>();

        @Override
        public void save(EventOutbox eventOutbox) {
            if (eventOutbox.getStatus() == EventOutboxStatus.PUBLISHED) {
                throw new OptimisticLockingFailureException("outbox lease 소유권 변경");
            }
            savedStatuses.add(eventOutbox.getStatus());
        }

        @Override
        public void saveAll(Collection<EventOutbox> eventOutboxes) {
            eventOutboxes.forEach(eventOutbox -> savedStatuses.add(eventOutbox.getStatus()));
        }
    }

    private static class FakeLoadEventOutboxPort implements LoadEventOutboxPort {

        private final List<EventOutbox> outboxes;

        private FakeLoadEventOutboxPort(List<EventOutbox> outboxes) {
            this.outboxes = outboxes;
        }

        @Override
        public List<EventOutbox> listPublishable(int limit, Instant now) {
            return outboxes;
        }
    }

    private static class FakeSaveEventOutboxPort implements SaveEventOutboxPort {

        private final List<EventOutboxStatus> savedStatuses = new ArrayList<>();

        @Override
        public void save(EventOutbox eventOutbox) {
            savedStatuses.add(eventOutbox.getStatus());
        }

        @Override
        public void saveAll(Collection<EventOutbox> eventOutboxes) {
            eventOutboxes.forEach(eventOutbox -> savedStatuses.add(eventOutbox.getStatus()));
        }
    }

    private static class CapturingApplicationEventPublisher implements ApplicationEventPublisher {

        private final List<Object> events = new ArrayList<>();

        @Override
        public void publishEvent(Object event) {
            events.add(event);
        }
    }

    public record TestEvent(
        UUID eventId,
        Instant occurredAt,
        String eventType,
        String message
    ) implements DomainEvent {

        static TestEvent create(String eventType, String message) {
            return new TestEvent(UUID.randomUUID(), Instant.now(), eventType, message);
        }
    }

    public record NonTransactionalTestEvent(
        UUID eventId,
        Instant occurredAt,
        String eventType,
        String message
    ) implements DomainEvent {

        static NonTransactionalTestEvent create(String eventType, String message) {
            return new NonTransactionalTestEvent(UUID.randomUUID(), Instant.now(), eventType, message);
        }

        @Override
        public OutboxDispatchMode outboxDispatchMode() {
            return OutboxDispatchMode.NON_TRANSACTIONAL;
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
