package com.umc.product.global.event.adapter.out;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.umc.product.global.event.application.port.out.LoadEventOutboxPort;
import com.umc.product.global.event.application.port.out.SaveEventOutboxPort;
import com.umc.product.global.event.application.port.out.dto.OutboxPublishResult;
import com.umc.product.global.event.domain.DomainEvent;
import com.umc.product.global.event.domain.EventOutbox;
import com.umc.product.global.event.domain.EventOutboxStatus;
import com.umc.product.global.event.domain.OutboxIdempotencyConflictException;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.test.simple.SimpleTracer;

@DisplayName("OutboxDomainEventPublisher publishOnce")
class OutboxDomainEventPublisherPublishOnceTest {

    private static final UUID EVENT_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final Instant OCCURRED_AT = Instant.parse("2026-07-18T00:00:00Z");
    private static final Instant AVAILABLE_AT = Instant.parse("2026-07-18T01:00:00.123456789Z");

    @Test
    @DisplayName("새 요청은 microsecond 예약 시각과 fingerprint를 가진 pending row를 한 번 저장한다")
    void publishOnce_newRequestStoresOnePendingRow() {
        InMemoryEventOutboxStore store = new InMemoryEventOutboxStore();
        OutboxDomainEventPublisher publisher = publisher(store, Tracer.NOOP);
        TestEvent event = event(OCCURRED_AT, "test.created", "original");

        OutboxPublishResult result = publisher.publishOnce(event, AVAILABLE_AT);

        assertThat(result.eventId()).isEqualTo(EVENT_ID);
        assertThat(result.status()).isEqualTo(EventOutboxStatus.PENDING);
        assertThat(result.deduplicated()).isFalse();
        assertThat(result.nextAttemptAt()).isEqualTo(AVAILABLE_AT.truncatedTo(ChronoUnit.MICROS));
        assertThat(store.saved).hasSize(1);
        assertThat(store.stored().getPayloadFingerprint()).matches("[0-9a-f]{64}");
        assertThat(store.stored().getAvailableAt()).isEqualTo(AVAILABLE_AT.truncatedTo(ChronoUnit.MICROS));
        assertThat(store.stored().getPayload()).contains(
            "\"eventId\":\"" + EVENT_ID + "\"",
            "\"eventType\":\"test.created\"",
            "\"value\":\"original\""
        );
    }

    @Test
    @DisplayName("같은 요청은 occurredAt과 현재 trace가 달라도 기존 row를 반환하고 최초 trace만 보존한다")
    void publishOnce_sameIdentityReturnsExistingRow() {
        InMemoryEventOutboxStore store = new InMemoryEventOutboxStore();
        SimpleTracer tracer = new SimpleTracer();
        OutboxDomainEventPublisher publisher = publisher(store, tracer);
        Span firstSpan = tracer.nextSpan().name("first").start();
        OutboxPublishResult first;
        try (Tracer.SpanInScope ignored = tracer.withSpan(firstSpan)) {
            first = publisher.publishOnce(event(OCCURRED_AT, "test.created", "original"), AVAILABLE_AT);
        } finally {
            firstSpan.end();
        }
        Span duplicateSpan = tracer.nextSpan().name("duplicate").start();

        OutboxPublishResult duplicate;
        try (Tracer.SpanInScope ignored = tracer.withSpan(duplicateSpan)) {
            duplicate = publisher.publishOnce(
                event(OCCURRED_AT.plusSeconds(30), "test.created", "original"),
                AVAILABLE_AT
            );
        } finally {
            duplicateSpan.end();
        }

        assertThat(first.deduplicated()).isFalse();
        assertThat(duplicate.deduplicated()).isTrue();
        assertThat(duplicate.status()).isEqualTo(EventOutboxStatus.PENDING);
        assertThat(duplicate.nextAttemptAt()).isEqualTo(first.nextAttemptAt());
        assertThat(store.saved).hasSize(1);
        assertThat(store.stored().getTraceparent())
            .contains(firstSpan.context().traceId())
            .doesNotContain(duplicateSpan.context().traceId());
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("identityMismatches")
    @DisplayName("같은 eventId의 동일성 필드가 다르면 기존 row를 바꾸지 않고 conflict를 반환한다")
    void publishOnce_identityMismatchConflicts(
        String mismatch,
        DomainEvent conflictingEvent,
        Instant conflictingAvailableAt
    ) {
        InMemoryEventOutboxStore store = new InMemoryEventOutboxStore();
        OutboxDomainEventPublisher publisher = publisher(store, Tracer.NOOP);
        publisher.publishOnce(event(OCCURRED_AT, "test.created", "original"), AVAILABLE_AT);
        EventOutbox original = store.stored();

        assertThatThrownBy(() -> publisher.publishOnce(conflictingEvent, conflictingAvailableAt))
            .as(mismatch)
            .isInstanceOf(OutboxIdempotencyConflictException.class);

        assertThat(store.saved).containsExactly(original);
        assertThat(store.stored()).isSameAs(original);
    }

    @Test
    @DisplayName("legacy null fingerprint와 availableAt row는 동일 요청으로 간주하지 않는다")
    void publishOnce_legacyNullIdentityConflicts() {
        InMemoryEventOutboxStore store = new InMemoryEventOutboxStore();
        TestEvent event = event(OCCURRED_AT, "test.created", "original");
        EventOutbox legacy = EventOutbox.record(event, "{\"payload\":\"legacy\"}");
        store.save(legacy);
        OutboxDomainEventPublisher publisher = publisher(store, Tracer.NOOP);

        assertThatThrownBy(() -> publisher.publishOnce(event, AVAILABLE_AT))
            .isInstanceOf(OutboxIdempotencyConflictException.class);

        assertThat(store.stored()).isSameAs(legacy);
        assertThat(store.stored().getPayloadFingerprint()).isNull();
        assertThat(store.stored().getAvailableAt()).isNull();
    }

    @Test
    @DisplayName("null event는 row를 저장하지 않고 거부한다")
    void publishOnce_nullEventRejected() {
        InMemoryEventOutboxStore store = new InMemoryEventOutboxStore();
        OutboxDomainEventPublisher publisher = publisher(store, Tracer.NOOP);

        assertThatThrownBy(() -> publisher.publishOnce(null, AVAILABLE_AT))
            .isInstanceOf(IllegalArgumentException.class);
        assertThat(store.saved).isEmpty();
    }

    @Test
    @DisplayName("null availableAt은 row를 저장하지 않고 거부한다")
    void publishOnce_nullAvailableAtRejected() {
        InMemoryEventOutboxStore store = new InMemoryEventOutboxStore();
        OutboxDomainEventPublisher publisher = publisher(store, Tracer.NOOP);

        assertThatThrownBy(() -> publisher.publishOnce(event(OCCURRED_AT, "test.created", "original"), null))
            .isInstanceOf(IllegalArgumentException.class);
        assertThat(store.saved).isEmpty();
    }

    private static Stream<Arguments> identityMismatches() {
        return Stream.of(
            Arguments.of(
                "eventClass",
                new AlternateTestEvent(EVENT_ID, OCCURRED_AT, "test.created", new BusinessPayload("original")),
                AVAILABLE_AT
            ),
            Arguments.of("eventType", event(OCCURRED_AT, "test.changed", "original"), AVAILABLE_AT),
            Arguments.of("nested business value", event(OCCURRED_AT, "test.created", "changed"), AVAILABLE_AT),
            Arguments.of(
                "microsecond availableAt",
                event(OCCURRED_AT, "test.created", "original"),
                AVAILABLE_AT.plus(1, ChronoUnit.MICROS)
            )
        );
    }

    private static TestEvent event(Instant occurredAt, String eventType, String value) {
        return new TestEvent(EVENT_ID, occurredAt, eventType, new BusinessPayload(value));
    }

    private OutboxDomainEventPublisher publisher(InMemoryEventOutboxStore store, Tracer tracer) {
        return new OutboxDomainEventPublisher(
            store,
            store,
            new EventPayloadSerializer(new ObjectMapper().findAndRegisterModules()),
            tracer
        );
    }

    private static class InMemoryEventOutboxStore implements SaveEventOutboxPort, LoadEventOutboxPort {

        private final Map<UUID, EventOutbox> byEventId = new LinkedHashMap<>();
        private final List<EventOutbox> saved = new ArrayList<>();

        @Override
        public void save(EventOutbox eventOutbox) {
            byEventId.put(eventOutbox.getEventId(), eventOutbox);
            saved.add(eventOutbox);
        }

        @Override
        public void saveAll(Collection<EventOutbox> eventOutboxes) {
            eventOutboxes.forEach(this::save);
        }

        @Override
        public boolean saveIfAbsent(EventOutbox eventOutbox) {
            if (byEventId.containsKey(eventOutbox.getEventId())) {
                return false;
            }
            save(eventOutbox);
            return true;
        }

        @Override
        public Optional<EventOutbox> findByEventId(UUID eventId) {
            return Optional.ofNullable(byEventId.get(eventId));
        }

        @Override
        public List<EventOutbox> listPublishable(int limit, Instant now) {
            return List.copyOf(saved);
        }

        private EventOutbox stored() {
            return byEventId.get(EVENT_ID);
        }
    }

    private record BusinessPayload(String value) {
    }

    private record TestEvent(
        UUID eventId,
        Instant occurredAt,
        String eventType,
        BusinessPayload business
    ) implements DomainEvent {
    }

    private record AlternateTestEvent(
        UUID eventId,
        Instant occurredAt,
        String eventType,
        BusinessPayload business
    ) implements DomainEvent {
    }
}
