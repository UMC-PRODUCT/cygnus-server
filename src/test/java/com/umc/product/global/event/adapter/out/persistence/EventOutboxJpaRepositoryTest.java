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
