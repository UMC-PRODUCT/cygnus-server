package com.umc.product.global.event.adapter.out;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.umc.product.global.event.application.port.out.LoadEventOutboxPort;
import com.umc.product.global.event.application.port.out.SaveEventOutboxPort;
import com.umc.product.global.event.domain.DomainEvent;
import com.umc.product.global.event.domain.EventOutbox;
import com.umc.product.global.event.domain.EventOutboxStatus;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.test.simple.SimpleTracer;

@DisplayName("OutboxDomainEventPublisher")
class OutboxDomainEventPublisherTest {

    @Test
    @DisplayName("publish는 도메인 이벤트를 직발행하지 않고 event outbox로 저장한다")
    void testCase001() {
        FakeSaveEventOutboxPort savePort = new FakeSaveEventOutboxPort();
        OutboxDomainEventPublisher publisher = new OutboxDomainEventPublisher(
            savePort,
            savePort,
            new EventPayloadSerializer(new ObjectMapper().findAndRegisterModules()),
            Tracer.NOOP
        );
        TestEvent event = TestEvent.create("test.created", "hello");

        publisher.publish(event);

        assertThat(savePort.saved).hasSize(1);
        EventOutbox outbox = savePort.saved.getFirst();
        assertThat(outbox.getEventId()).isEqualTo(event.eventId());
        assertThat(outbox.getEventType()).isEqualTo("test.created");
        assertThat(outbox.getEventClass()).isEqualTo(TestEvent.class.getName());
        assertThat(outbox.getStatus()).isEqualTo(EventOutboxStatus.PENDING);
        assertThat(outbox.getAttempts()).isZero();
        assertThat(outbox.getPayloadFingerprint()).matches("[0-9a-f]{64}");
        assertThat(outbox.getAvailableAt()).isNotNull();
        assertThat(outbox.getNextAttemptAt()).isEqualTo(outbox.getAvailableAt());
        assertThat(outbox.getPayload()).contains(
            "\"eventId\":\"" + event.eventId() + "\"",
            "\"occurredAt\":",
            "\"eventType\":\"test.created\"",
            "\"message\":\"hello\""
        );
    }

    @Test
    @DisplayName("publishAll은 입력 순서대로 모든 이벤트를 일괄 저장한다")
    void testCase002() {
        FakeSaveEventOutboxPort savePort = new FakeSaveEventOutboxPort();
        OutboxDomainEventPublisher publisher = new OutboxDomainEventPublisher(
            savePort,
            savePort,
            new EventPayloadSerializer(new ObjectMapper().findAndRegisterModules()),
            Tracer.NOOP
        );
        TestEvent first = TestEvent.create("test.first", "first");
        TestEvent second = TestEvent.create("test.second", "second");

        publisher.publishAll(List.of(first, second));

        assertThat(savePort.saved)
            .extracting(EventOutbox::getEventId)
            .containsExactly(first.eventId(), second.eventId());
        assertThat(savePort.saved)
            .extracting(EventOutbox::getStatus)
            .containsOnly(EventOutboxStatus.PENDING);
        assertThat(savePort.saved)
            .allSatisfy(outbox -> {
                assertThat(outbox.getPayloadFingerprint()).matches("[0-9a-f]{64}");
                assertThat(outbox.getAvailableAt()).isNotNull();
                assertThat(outbox.getNextAttemptAt()).isEqualTo(outbox.getAvailableAt());
            });
        assertThat(savePort.saveAllCalled).isTrue();
    }

    @Test
    @DisplayName("활성 span이 있으면 현재 trace의 W3C traceparent를 outbox에 캡처해 저장한다")
    void testCase003() {
        FakeSaveEventOutboxPort savePort = new FakeSaveEventOutboxPort();
        SimpleTracer tracer = new SimpleTracer();
        OutboxDomainEventPublisher publisher = new OutboxDomainEventPublisher(
            savePort,
            savePort,
            new EventPayloadSerializer(new ObjectMapper().findAndRegisterModules()),
            tracer
        );
        TestEvent event = TestEvent.create("test.created", "hello");
        Span span = tracer.nextSpan().name("origin").start();
        try (Tracer.SpanInScope ignored = tracer.withSpan(span)) {
            publisher.publish(event);
        } finally {
            span.end();
        }

        EventOutbox outbox = savePort.saved.getFirst();
        assertThat(outbox.getTraceparent())
            .isNotNull()
            .startsWith("00-" + span.context().traceId() + "-" + span.context().spanId() + "-");
    }

    @Test
    @DisplayName("활성 span이 없으면 traceparent를 null로 저장한다")
    void testCase004() {
        FakeSaveEventOutboxPort savePort = new FakeSaveEventOutboxPort();
        OutboxDomainEventPublisher publisher = new OutboxDomainEventPublisher(
            savePort,
            savePort,
            new EventPayloadSerializer(new ObjectMapper().findAndRegisterModules()),
            new SimpleTracer()
        );
        TestEvent event = TestEvent.create("test.created", "hello");

        publisher.publish(event);

        assertThat(savePort.saved.getFirst().getTraceparent()).isNull();
    }

    private static class FakeSaveEventOutboxPort implements SaveEventOutboxPort, LoadEventOutboxPort {

        private final List<EventOutbox> saved = new ArrayList<>();

        @Override
        public void save(EventOutbox eventOutbox) {
            saved.add(eventOutbox);
        }

        @Override
        public boolean saveIfAbsent(EventOutbox eventOutbox) {
            if (saved.stream().anyMatch(savedOutbox -> savedOutbox.getEventId().equals(eventOutbox.getEventId()))) {
                return false;
            }
            save(eventOutbox);
            return true;
        }

        @Override
        public void saveAll(Collection<EventOutbox> eventOutboxes) {
            saveAllCalled = true;
            saved.addAll(eventOutboxes);
        }

        @Override
        public Optional<EventOutbox> findByEventId(UUID eventId) {
            return saved.stream()
                .filter(outbox -> outbox.getEventId().equals(eventId))
                .findFirst();
        }

        @Override
        public List<EventOutbox> listPublishable(int limit, Instant now) {
            return saved.stream().limit(limit).toList();
        }

        private boolean saveAllCalled;
    }

    private record TestEvent(
        UUID eventId,
        Instant occurredAt,
        String eventType,
        String message
    ) implements DomainEvent {

        static TestEvent create(String eventType, String message) {
            return new TestEvent(UUID.randomUUID(), Instant.now(), eventType, message);
        }
    }
}
