package com.umc.product.global.event.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.global.event.domain.DomainEvent;
import com.umc.product.global.event.domain.EventOutbox;
import com.umc.product.support.PersistenceAdapterTest;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.OptimisticLockException;

@PersistenceAdapterTest
@DisplayName("EventOutboxJpaRepository")
class EventOutboxJpaRepositoryTest {

    private static final Instant NOW = Instant.parse("2026-07-13T00:00:00Z");
    private static final String VALID_FINGERPRINT = "a".repeat(64);

    @Autowired
    private EventOutboxJpaRepository repository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Test
    @DisplayName("발행 가능한 이벤트를 다음 시도 시각 순서로 조회한다")
    void findPublishableForUpdateOrdersByNextAttemptAt() {
        UUID laterEventId = insertOutbox("PENDING", NOW.minusSeconds(1));
        UUID earlierEventId = insertOutbox("PENDING", NOW.minusSeconds(10));
        insertOutbox("PUBLISHED", NOW.minusSeconds(20));
        entityManager.flush();
        entityManager.clear();

        List<EventOutbox> result = repository.findPublishableForUpdate(100, NOW);

        assertThat(result)
            .extracting(EventOutbox::getEventId)
            .containsExactly(earlierEventId, laterEventId);
    }

    @Test
    @DisplayName("발행 대기 상태만 포함하는 partial index를 사용한다")
    void publishablePartialIndexExists() {
        Object indexDefinition = entityManager.createNativeQuery("""
                SELECT indexdef
                FROM pg_indexes
                WHERE schemaname = 'public'
                  AND tablename = 'event_outbox'
                  AND indexname = 'idx_event_outbox_publishable'
                """)
            .getSingleResult();

        assertThat(indexDefinition.toString())
            .contains("next_attempt_at", "id", "PENDING", "PROCESSING");

        Number legacyIndexCount = (Number) entityManager.createNativeQuery("""
                SELECT COUNT(*)
                FROM pg_indexes
                WHERE schemaname = 'public'
                  AND tablename = 'event_outbox'
                  AND indexname = 'idx_event_outbox_pending'
                """)
            .getSingleResult();
        assertThat(legacyIndexCount.longValue()).isZero();
    }

    @Test
    @DisplayName("availableAt을 PostgreSQL microsecond 정밀도로 round-trip한다")
    void availableAtRoundTripsAtMicrosecondPrecision() {
        Instant input = Instant.parse("2026-07-18T00:00:00.123456789Z");
        EventOutbox outbox = EventOutbox.record(TestEvent.create(), "{}", VALID_FINGERPRINT, input);

        repository.saveAndFlush(outbox);
        entityManager.clear();

        EventOutbox loaded = repository.findById(outbox.getId()).orElseThrow();
        Instant expected = Instant.parse("2026-07-18T00:00:00.123456Z");
        assertThat(loaded.getAvailableAt()).isEqualTo(expected);
        assertThat(loaded.getNextAttemptAt()).isEqualTo(expected);
        assertThat(loaded.getPayloadFingerprint()).isEqualTo(VALID_FINGERPRINT);
    }

    @Test
    @DisplayName("nextAttemptAt을 lease 시각으로 변경해도 availableAt은 불변이다")
    void availableAtRemainsAfterNextAttemptAtChanges() {
        Instant availableAt = Instant.parse("2026-07-18T00:00:00.123456Z");
        EventOutbox outbox = EventOutbox.record(TestEvent.create(), "{}", VALID_FINGERPRINT, availableAt);
        repository.saveAndFlush(outbox);
        entityManager.clear();
        EventOutbox loaded = repository.findById(outbox.getId()).orElseThrow();
        Instant leaseUntil = Instant.parse("2026-07-18T00:05:00Z");

        loaded.markProcessing(leaseUntil);
        repository.saveAndFlush(loaded);
        entityManager.clear();

        EventOutbox reloaded = repository.findById(outbox.getId()).orElseThrow();
        assertThat(reloaded.getAvailableAt()).isEqualTo(availableAt);
        assertThat(reloaded.getNextAttemptAt()).isEqualTo(leaseUntil);
    }

    @Test
    @DisplayName("신규 column이 null인 legacy row를 JPA로 round-trip한다")
    void legacyNullColumnsRoundTrip() {
        UUID eventId = insertOutbox("PENDING", NOW);
        entityManager.flush();
        entityManager.clear();
        EventOutbox legacy = findByEventId(eventId);

        legacy.markProcessing(NOW.plusSeconds(300));
        entityManager.flush();
        entityManager.clear();

        EventOutbox reloaded = findByEventId(eventId);
        assertThat(reloaded.getPayloadFingerprint()).isNull();
        assertThat(reloaded.getAvailableAt()).isNull();
    }

    @Test
    @DisplayName("미래 availableAt의 outbox는 due 조회에서 제외한다")
    void futureAvailableAtIsNotPublishable() {
        EventOutbox future = EventOutbox.record(
            TestEvent.create(),
            "{}",
            VALID_FINGERPRINT,
            NOW.plusSeconds(60)
        );
        repository.saveAndFlush(future);
        entityManager.clear();

        List<EventOutbox> result = repository.findPublishableForUpdate(100, NOW);

        assertThat(result).extracting(EventOutbox::getEventId).doesNotContain(future.getEventId());
    }

    @Test
    @DisplayName("outbox 멱등 예약 column과 lowercase SHA-256 constraint를 추가한다")
    void idempotentScheduleSchemaExists() {
        Object[] fingerprintColumn = columnMetadata("payload_fingerprint");
        Object[] availableAtColumn = columnMetadata("available_at");
        Object constraintDefinition = entityManager.createNativeQuery("""
                SELECT pg_get_constraintdef(oid)
                FROM pg_constraint
                WHERE conrelid = 'public.event_outbox'::regclass
                  AND conname = 'chk_event_outbox_payload_fingerprint_sha256'
                """)
            .getSingleResult();

        assertThat(fingerprintColumn[0]).isEqualTo("character varying");
        assertThat(((Number) fingerprintColumn[1]).intValue()).isEqualTo(64);
        assertThat(fingerprintColumn[3]).isEqualTo("YES");
        assertThat(availableAtColumn[0]).isEqualTo("timestamp with time zone");
        assertThat(((Number) availableAtColumn[2]).intValue()).isEqualTo(6);
        assertThat(availableAtColumn[3]).isEqualTo("YES");
        assertThat(constraintDefinition.toString()).contains("payload_fingerprint", "[0-9a-f]{64}");
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @DisplayName("lease를 다시 획득한 뒤에는 이전 worker가 outbox 상태를 덮어쓸 수 없다")
    void staleWorkerCannotOverwriteReclaimedOutbox() {
        EventOutbox recorded = EventOutbox.record(TestEvent.create(), "{}");
        Long outboxId = persist(recorded);
        EventOutbox staleWorkerCopy = findDetached(outboxId);

        EntityManager reclaimingEntityManager = entityManagerFactory.createEntityManager();
        reclaimingEntityManager.getTransaction().begin();
        EventOutbox reclaimed = reclaimingEntityManager.find(EventOutbox.class, outboxId);
        reclaimed.markProcessing(Instant.now().plusSeconds(300));
        reclaimingEntityManager.getTransaction().commit();
        reclaimingEntityManager.close();

        EntityManager staleEntityManager = entityManagerFactory.createEntityManager();
        staleEntityManager.getTransaction().begin();
        staleWorkerCopy.markPublished();

        assertThatThrownBy(() -> {
            staleEntityManager.merge(staleWorkerCopy);
            staleEntityManager.flush();
        }).isInstanceOf(OptimisticLockException.class);

        staleEntityManager.getTransaction().rollback();
        staleEntityManager.close();
        delete(outboxId);
    }

    private Long persist(EventOutbox outbox) {
        EntityManager setupEntityManager = entityManagerFactory.createEntityManager();
        setupEntityManager.getTransaction().begin();
        setupEntityManager.persist(outbox);
        setupEntityManager.getTransaction().commit();
        Long outboxId = outbox.getId();
        setupEntityManager.close();
        return outboxId;
    }

    private EventOutbox findDetached(Long outboxId) {
        EntityManager workerEntityManager = entityManagerFactory.createEntityManager();
        EventOutbox outbox = workerEntityManager.find(EventOutbox.class, outboxId);
        workerEntityManager.close();
        return outbox;
    }

    private void delete(Long outboxId) {
        EntityManager cleanupEntityManager = entityManagerFactory.createEntityManager();
        cleanupEntityManager.getTransaction().begin();
        EventOutbox outbox = cleanupEntityManager.find(EventOutbox.class, outboxId);
        cleanupEntityManager.remove(outbox);
        cleanupEntityManager.getTransaction().commit();
        cleanupEntityManager.close();
    }

    private EventOutbox findByEventId(UUID eventId) {
        return entityManager.createQuery(
                "SELECT outbox FROM EventOutbox outbox WHERE outbox.eventId = :eventId",
                EventOutbox.class
            )
            .setParameter("eventId", eventId)
            .getSingleResult();
    }

    private Object[] columnMetadata(String columnName) {
        return (Object[]) entityManager.createNativeQuery("""
                SELECT data_type, character_maximum_length, datetime_precision, is_nullable
                FROM information_schema.columns
                WHERE table_schema = 'public'
                  AND table_name = 'event_outbox'
                  AND column_name = :columnName
                """)
            .setParameter("columnName", columnName)
            .getSingleResult();
    }

    private UUID insertOutbox(String status, Instant nextAttemptAt) {
        UUID eventId = UUID.randomUUID();
        entityManager.createNativeQuery("""
                INSERT INTO event_outbox (
                    event_id, event_type, event_class, payload, status, attempts,
                    next_attempt_at, created_at, updated_at
                ) VALUES (
                    :eventId, 'test.event', 'test.Event', CAST('{}' AS jsonb), :status, 0,
                    :nextAttemptAt, :createdAt, :updatedAt
                )
                """)
            .setParameter("eventId", eventId)
            .setParameter("status", status)
            .setParameter("nextAttemptAt", nextAttemptAt)
            .setParameter("createdAt", NOW)
            .setParameter("updatedAt", NOW)
            .executeUpdate();
        return eventId;
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
