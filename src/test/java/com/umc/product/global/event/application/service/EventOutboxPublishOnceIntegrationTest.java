package com.umc.product.global.event.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

import com.umc.product.global.event.adapter.out.persistence.EventOutboxJpaRepository;
import com.umc.product.global.event.application.port.out.DomainEventPublisher;
import com.umc.product.global.event.application.port.out.dto.OutboxPublishResult;
import com.umc.product.global.event.domain.DomainEvent;
import com.umc.product.global.event.domain.EventOutbox;
import com.umc.product.global.event.domain.EventOutboxStatus;
import com.umc.product.global.event.domain.OutboxIdempotencyConflictException;
import com.umc.product.support.IntegrationTestSupport;

@DisplayName("EventOutbox publishOnce PostgreSQL 통합")
@TestPropertySource(properties = "app.event-outbox.relay-enabled=false")
class EventOutboxPublishOnceIntegrationTest extends IntegrationTestSupport {

    private static final int CONCURRENT_REQUESTS = 20;
    private static final UUID EVENT_ID = UUID.fromString("30000000-0000-0000-0000-000000000001");
    private static final Instant OCCURRED_AT = Instant.parse("2026-07-18T00:00:00Z");
    private static final Instant AVAILABLE_AT = Instant.parse("2026-07-18T01:00:00.123456789Z");

    @Autowired
    private DomainEventPublisher publisher;

    @Autowired
    private EventOutboxJpaRepository repository;

    @RepeatedTest(3)
    @DisplayName("동일 요청 20개를 동시에 발행하면 한 row와 한 first-writer 결과만 남는다")
    void concurrentIdenticalRequestsInsertExactlyOnce() throws Exception {
        TestEvent event = event(OCCURRED_AT, "test.created", "original");
        CountDownLatch ready = new CountDownLatch(CONCURRENT_REQUESTS);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_REQUESTS);
        List<Future<OutboxPublishResult>> futures = new ArrayList<>();
        List<OutboxPublishResult> results = new ArrayList<>();
        try {
            for (int index = 0; index < CONCURRENT_REQUESTS; index++) {
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    start.await(10, TimeUnit.SECONDS);
                    return publisher.publishOnce(event, AVAILABLE_AT);
                }));
            }
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();

            start.countDown();
            for (Future<OutboxPublishResult> future : futures) {
                results.add(future.get(20, TimeUnit.SECONDS));
            }
        } finally {
            start.countDown();
            executor.shutdownNow();
            assertThat(executor.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
        }

        long persistedRows = rowCount(EVENT_ID);
        long firstWriters = results.stream().filter(result -> !result.deduplicated()).count();
        long deduplicated = results.stream().filter(OutboxPublishResult::deduplicated).count();
        assertThat(persistedRows).isOne();
        assertThat(results).hasSize(CONCURRENT_REQUESTS);
        assertThat(firstWriters).isOne();
        assertThat(deduplicated).isEqualTo(CONCURRENT_REQUESTS - 1);
        assertThat(results).allSatisfy(result -> {
            assertThat(result.eventId()).isEqualTo(EVENT_ID);
            assertThat(result.status()).isEqualTo(EventOutboxStatus.PENDING);
            assertThat(result.nextAttemptAt()).isEqualTo(AVAILABLE_AT.truncatedTo(ChronoUnit.MICROS));
        });
        EventOutbox stored = repository.findByEventId(EVENT_ID).orElseThrow();
        assertThat(stored.getPayloadFingerprint()).matches("[0-9a-f]{64}");
        assertThat(stored.getAvailableAt()).isEqualTo(AVAILABLE_AT.truncatedTo(ChronoUnit.MICROS));
        assertThat(stored.getPayload()).contains("original");
    }

    @Test
    @DisplayName("occurredAt만 다른 재요청은 기존 row의 상태를 반환한다")
    void differentOccurredAtIsDeduplicated() {
        OutboxPublishResult first = publisher.publishOnce(
            event(OCCURRED_AT, "test.created", "original"),
            AVAILABLE_AT
        );

        OutboxPublishResult duplicate = publisher.publishOnce(
            event(OCCURRED_AT.plusSeconds(1), "test.created", "original"),
            AVAILABLE_AT
        );

        assertThat(first.deduplicated()).isFalse();
        assertThat(duplicate.deduplicated()).isTrue();
        assertThat(duplicate.status()).isEqualTo(first.status());
        assertThat(duplicate.nextAttemptAt()).isEqualTo(first.nextAttemptAt());
        assertThat(rowCount(EVENT_ID)).isOne();
    }

    @Test
    @DisplayName("같은 eventId의 eventClass 불일치는 conflict이고 기존 row를 보존한다")
    void eventClassMismatchConflicts() {
        TestEvent original = event(OCCURRED_AT, "test.created", "original");
        DomainEvent conflicting = new AlternateTestEvent(
            EVENT_ID,
            OCCURRED_AT,
            "test.created",
            new BusinessPayload(new NestedValue("original"))
        );

        assertConflictPreservesOriginal(original, conflicting, AVAILABLE_AT);
    }

    @Test
    @DisplayName("같은 eventId의 eventType 불일치는 conflict이고 기존 row를 보존한다")
    void eventTypeMismatchConflicts() {
        TestEvent original = event(OCCURRED_AT, "test.created", "original");
        TestEvent conflicting = event(OCCURRED_AT, "test.changed", "original");

        assertConflictPreservesOriginal(original, conflicting, AVAILABLE_AT);
    }

    @Test
    @DisplayName("같은 eventId의 nested business value 불일치는 conflict이고 기존 row를 보존한다")
    void nestedBusinessValueMismatchConflicts() {
        TestEvent original = event(OCCURRED_AT, "test.created", "original");
        TestEvent conflicting = event(OCCURRED_AT, "test.created", "changed");

        assertConflictPreservesOriginal(original, conflicting, AVAILABLE_AT);
    }

    @Test
    @DisplayName("같은 eventId의 microsecond availableAt 불일치는 conflict이고 기존 row를 보존한다")
    void availableAtMismatchConflicts() {
        TestEvent original = event(OCCURRED_AT, "test.created", "original");

        assertConflictPreservesOriginal(
            original,
            original,
            AVAILABLE_AT.plus(1, ChronoUnit.MICROS)
        );
    }

    @Test
    @DisplayName("legacy null 동일성 row는 conflict이고 기존 row를 보존한다")
    void legacyNullIdentityConflicts() {
        TestEvent event = event(OCCURRED_AT, "test.created", "original");
        repository.saveAndFlush(EventOutbox.record(event, "{\"legacy\":true}"));
        PersistedSnapshot before = PersistedSnapshot.from(repository.findByEventId(EVENT_ID).orElseThrow());

        assertThatThrownBy(() -> publisher.publishOnce(event, AVAILABLE_AT))
            .isInstanceOf(OutboxIdempotencyConflictException.class);

        assertThat(rowCount(EVENT_ID)).isOne();
        assertThat(PersistedSnapshot.from(repository.findByEventId(EVENT_ID).orElseThrow())).isEqualTo(before);
    }

    private void assertConflictPreservesOriginal(
        TestEvent original,
        DomainEvent conflicting,
        Instant conflictingAvailableAt
    ) {
        publisher.publishOnce(original, AVAILABLE_AT);
        PersistedSnapshot before = PersistedSnapshot.from(repository.findByEventId(EVENT_ID).orElseThrow());

        assertThatThrownBy(() -> publisher.publishOnce(conflicting, conflictingAvailableAt))
            .isInstanceOf(OutboxIdempotencyConflictException.class);

        assertThat(rowCount(EVENT_ID)).isOne();
        assertThat(PersistedSnapshot.from(repository.findByEventId(EVENT_ID).orElseThrow())).isEqualTo(before);
    }

    private long rowCount(UUID eventId) {
        Number count = (Number) entityManager.createNativeQuery(
                "SELECT COUNT(*) FROM event_outbox WHERE event_id = :eventId"
            )
            .setParameter("eventId", eventId)
            .getSingleResult();
        return count.longValue();
    }

    private static TestEvent event(Instant occurredAt, String eventType, String value) {
        return new TestEvent(EVENT_ID, occurredAt, eventType, new BusinessPayload(new NestedValue(value)));
    }

    private record PersistedSnapshot(
        String eventClass,
        String eventType,
        String payload,
        String fingerprint,
        Instant availableAt,
        Instant nextAttemptAt,
        String traceparent,
        EventOutboxStatus status,
        int attempts,
        long version,
        Instant createdAt,
        Instant updatedAt
    ) {

        static PersistedSnapshot from(EventOutbox outbox) {
            return new PersistedSnapshot(
                outbox.getEventClass(),
                outbox.getEventType(),
                outbox.getPayload(),
                outbox.getPayloadFingerprint(),
                outbox.getAvailableAt(),
                outbox.getNextAttemptAt(),
                outbox.getTraceparent(),
                outbox.getStatus(),
                outbox.getAttempts(),
                outbox.getVersion(),
                outbox.getCreatedAt(),
                outbox.getUpdatedAt()
            );
        }
    }

    private record NestedValue(String value) {
    }

    private record BusinessPayload(NestedValue nested) {
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
