package com.umc.product.global.event.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.umc.product.global.event.domain.EventOutbox;
import com.umc.product.support.PersistenceAdapterTest;

import jakarta.persistence.EntityManager;

@PersistenceAdapterTest
@DisplayName("EventOutboxJpaRepository")
class EventOutboxJpaRepositoryTest {

    private static final Instant NOW = Instant.parse("2026-07-13T00:00:00Z");

    @Autowired
    private EventOutboxJpaRepository repository;

    @Autowired
    private EntityManager entityManager;

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
}
