package com.umc.product.global.event.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.umc.product.global.event.domain.DomainEvent;
import com.umc.product.global.event.domain.EventOutbox;
import com.umc.product.global.event.domain.EventOutboxStatus;
import com.umc.product.support.PersistenceAdapterTest;

import jakarta.persistence.EntityManager;

@PersistenceAdapterTest
@DisplayName("EventOutbox atomic insert persistence")
class EventOutboxAtomicInsertJpaRepositoryTest {

    private static final Instant NOW = Instant.parse("2026-07-13T00:00:00Z");
    private static final String VALID_FINGERPRINT = "a".repeat(64);

    @Autowired
    private EventOutboxJpaRepository repository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("기존 save와 ID 조회는 outbox 메타데이터와 전체 payload를 보존한다")
    void saveAndFindByIdPreservesMetadataAndFullPayload() {
        TestEvent event = TestEvent.create();
        String payload = "{\"eventId\":\"" + event.eventId() + "\",\"businessValue\":\"value\"}";
        EventOutbox outbox = EventOutbox.record(event, payload, "00-test-traceparent");

        repository.saveAndFlush(outbox);
        entityManager.clear();

        EventOutbox loaded = repository.findById(outbox.getId()).orElseThrow();
        assertThat(loaded.getEventId()).isEqualTo(event.eventId());
        assertThat(loaded.getEventType()).isEqualTo(event.eventType());
        assertThat(loaded.getEventClass()).isEqualTo(TestEvent.class.getName());
        assertThat(loaded.getPayload()).contains(
            "\"eventId\":",
            event.eventId().toString(),
            "\"businessValue\":",
            "\"value\""
        );
        assertThat(loaded.getTraceparent()).isEqualTo("00-test-traceparent");
        assertThat(loaded.getStatus().name()).isEqualTo("PENDING");
        assertThat(loaded.getAttempts()).isZero();
    }

    @Test
    @DisplayName("native insert-if-absent는 모든 초기 필드를 채우고 eventId conflict에서 기존 row를 보존한다")
    void insertIfAbsentAtomicallyPreservesOriginalRow() {
        UUID eventId = UUID.fromString("20000000-0000-0000-0000-000000000001");
        TestEvent event = new TestEvent(eventId, NOW);
        Instant availableAt = Instant.parse("2026-07-18T01:00:00.123456789Z");
        EventOutbox original = EventOutbox.record(
            event,
            "{\"business\":{\"value\":\"original\"}}",
            VALID_FINGERPRINT,
            availableAt,
            "00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01"
        );
        EventOutbox conflicting = EventOutbox.record(
            event,
            "{\"business\":{\"value\":\"changed\"}}",
            "b".repeat(64),
            availableAt.plus(1, ChronoUnit.MICROS),
            "00-aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa-bbbbbbbbbbbbbbbb-01"
        );

        int inserted = repository.insertIfAbsent(original);
        int duplicateInserted = repository.insertIfAbsent(conflicting);
        entityManager.clear();

        EventOutbox loaded = repository.findByEventId(eventId).orElseThrow();
        Instant normalizedAvailableAt = availableAt.truncatedTo(ChronoUnit.MICROS);
        assertThat(inserted).isOne();
        assertThat(duplicateInserted).isZero();
        assertThat(loaded.getEventId()).isEqualTo(eventId);
        assertThat(loaded.getEventType()).isEqualTo(event.eventType());
        assertThat(loaded.getEventClass()).isEqualTo(TestEvent.class.getName());
        assertThat(loaded.getPayload()).contains("original").doesNotContain("changed");
        assertThat(loaded.getPayloadFingerprint()).isEqualTo(VALID_FINGERPRINT);
        assertThat(loaded.getAvailableAt()).isEqualTo(normalizedAvailableAt);
        assertThat(loaded.getNextAttemptAt()).isEqualTo(normalizedAvailableAt);
        assertThat(loaded.getTraceparent()).contains("4bf92f3577b34da6a3ce929d0e0e4736");
        assertThat(loaded.getStatus()).isEqualTo(EventOutboxStatus.PENDING);
        assertThat(loaded.getAttempts()).isZero();
        assertThat(loaded.getVersion()).isZero();
        assertThat(loaded.getCreatedAt()).isNotNull().isEqualTo(loaded.getUpdatedAt());
        System.out.println(
            "TODO3_DB_ATOMIC_INSERT inserted=1 duplicateInserted=0 rowCount=1 status=PENDING attempts=0 version=0"
        );
    }

    private record TestEvent(UUID eventId, Instant occurredAt) implements DomainEvent {

        private static TestEvent create() {
            return new TestEvent(UUID.randomUUID(), Instant.now());
        }

        @Override
        public String eventType() {
            return "test.event";
        }
    }
}
