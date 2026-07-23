package com.umc.product.global.event.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.umc.product.support.PersistenceAdapterTest;

import jakarta.persistence.EntityManager;

@PersistenceAdapterTest
@DisplayName("Event outbox payload retention persistence")
class EventOutboxRetentionPersistenceTest {

    private static final Instant NOW = Instant.parse("2026-07-23T00:00:00Z");

    @Autowired
    private EventOutboxJpaRepository repository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("fingerprint constraint와 retention schema가 최종 상태로 적용된다")
    void testCase001() {
        Object constraintValidated = entityManager.createNativeQuery("""
                SELECT convalidated
                FROM pg_constraint
                WHERE conname = 'chk_event_outbox_payload_fingerprint_sha256'
                """)
            .getSingleResult();
        Number redactedColumnCount = (Number) entityManager.createNativeQuery("""
                SELECT COUNT(*)
                FROM information_schema.columns
                WHERE table_schema = 'public'
                  AND table_name = 'event_outbox'
                  AND column_name = 'payload_redacted_at'
                """)
            .getSingleResult();
        Object[] sanitizedColumn = (Object[]) entityManager.createNativeQuery("""
                SELECT is_nullable, column_default
                FROM information_schema.columns
                WHERE table_schema = 'public'
                  AND table_name = 'event_outbox'
                  AND column_name = 'sanitized_last_error'
                """)
            .getSingleResult();

        assertThat(constraintValidated).isEqualTo(true);
        assertThat(redactedColumnCount.longValue()).isOne();
        assertThat(sanitizedColumn[0]).isEqualTo("YES");
        assertThat(sanitizedColumn[1]).isNull();
        assertIndex("idx_event_outbox_retention_published", "published_at", "PUBLISHED");
        assertIndex("idx_event_outbox_retention_failed", "updated_at", "FAILED");
    }

    @Test
    @DisplayName("보존기간이 지난 terminal payload만 비식별화한다")
    void testCase002() {
        UUID oldPublished = insert("PUBLISHED", NOW.minus(Duration.ofHours(24)), NOW.minus(Duration.ofHours(24)));
        UUID freshPublished = insert(
            "PUBLISHED",
            NOW.minus(Duration.ofHours(24)).plusNanos(1_000),
            NOW.minus(Duration.ofHours(24)).plusNanos(1_000)
        );
        UUID oldFailed = insert("FAILED", null, NOW.minus(Duration.ofDays(30)));
        UUID legacyUnsafeFailed = insert(
            "FAILED",
            null,
            NOW.minus(Duration.ofDays(31)),
            "ApplicantNameException",
            null
        );
        UUID mixedVersionFailed = insert(
            "FAILED",
            null,
            NOW.minus(Duration.ofDays(31)),
            "ApplicantNameException",
            "EMAIL-0005"
        );
        UUID pending = insert("PENDING", null, NOW.minus(Duration.ofDays(31)));
        entityManager.flush();

        int published = repository.redactPublishedBefore(NOW.minus(Duration.ofHours(24)), NOW, 500);
        int failed = repository.redactFailedBefore(NOW.minus(Duration.ofDays(30)), NOW, 500);
        entityManager.flush();
        entityManager.clear();

        assertThat(published).isOne();
        assertThat(failed).isEqualTo(3);
        assertRedacted(oldPublished, "PUBLISHED");
        assertRedacted(oldFailed, "FAILED");
        assertRedactedWithoutLastError(legacyUnsafeFailed, "FAILED");
        assertRedactedWithoutLastError(mixedVersionFailed, "FAILED");
        assertNotRedacted(freshPublished);
        assertNotRedacted(pending);
    }

    @Test
    @DisplayName("한 호출에서 지정한 batch 크기만 처리한다")
    void testCase003() {
        insert("PUBLISHED", NOW.minus(Duration.ofDays(2)), NOW.minus(Duration.ofDays(2)));
        insert("PUBLISHED", NOW.minus(Duration.ofDays(2)), NOW.minus(Duration.ofDays(2)));
        insert("PUBLISHED", NOW.minus(Duration.ofDays(2)), NOW.minus(Duration.ofDays(2)));
        entityManager.flush();

        int result = repository.redactPublishedBefore(NOW.minus(Duration.ofHours(24)), NOW, 2);
        entityManager.flush();

        Number remaining = (Number) entityManager.createNativeQuery("""
                SELECT COUNT(*)
                FROM event_outbox
                WHERE status = 'PUBLISHED' AND payload_redacted_at IS NULL
                """)
            .getSingleResult();
        assertThat(result).isEqualTo(2);
        assertThat(remaining.longValue()).isOne();
    }

    private UUID insert(String status, Instant publishedAt, Instant updatedAt) {
        return insert(status, publishedAt, updatedAt, "EMAIL-0005", "EMAIL-0005");
    }

    private UUID insert(
        String status,
        Instant publishedAt,
        Instant updatedAt,
        String lastError,
        String sanitizedLastError
    ) {
        UUID eventId = UUID.randomUUID();
        entityManager.createNativeQuery("""
                INSERT INTO event_outbox (
                    event_id, event_type, event_class, payload, status, attempts,
                    next_attempt_at, last_error, published_at, traceparent, version,
                    created_at, updated_at, payload_fingerprint, available_at,
                    sanitized_last_error
                ) VALUES (
                    :eventId, 'retention.test', 'example.RetentionEvent',
                    CAST('{"recipient":"secret@example.com"}' AS jsonb), :status, 1,
                    :updatedAt, :lastError, :publishedAt,
                    '00-0af7651916cd43dd8448eb211c80319c-b7ad6b7169203331-01', 0,
                    :updatedAt, :updatedAt, :fingerprint, :updatedAt, :sanitizedLastError
                )
                """)
            .setParameter("eventId", eventId)
            .setParameter("status", status)
            .setParameter("updatedAt", updatedAt)
            .setParameter("lastError", lastError)
            .setParameter("sanitizedLastError", sanitizedLastError)
            .setParameter("publishedAt", publishedAt)
            .setParameter("fingerprint", "a".repeat(64))
            .executeUpdate();
        return eventId;
    }

    private void assertIndex(String indexName, String timeColumn, String status) {
        Object[] index = (Object[]) entityManager.createNativeQuery("""
                SELECT indexes.indexdef, index_metadata.indisvalid, index_metadata.indisready
                FROM pg_indexes indexes
                JOIN pg_class index_class ON index_class.relname = indexes.indexname
                JOIN pg_namespace namespace ON namespace.oid = index_class.relnamespace
                JOIN pg_index index_metadata ON index_metadata.indexrelid = index_class.oid
                WHERE indexes.schemaname = 'public'
                  AND indexes.tablename = 'event_outbox'
                  AND indexes.indexname = :indexName
                  AND namespace.nspname = indexes.schemaname
                """)
            .setParameter("indexName", indexName)
            .getSingleResult();
        assertThat(index[0].toString()).contains(timeColumn, status, "payload_redacted_at IS NULL");
        assertThat(index[1]).isEqualTo(true);
        assertThat(index[2]).isEqualTo(true);
    }

    private void assertRedacted(UUID eventId, String status) {
        Object[] row = row(eventId);
        assertThat(row[0].toString()).isEqualTo("{}");
        assertThat(row[1]).isNull();
        assertThat(row[2]).isNotNull();
        assertThat(row[3]).isEqualTo(status);
        assertThat(row[4]).isEqualTo("a".repeat(64));
        assertThat(row[5]).isEqualTo("EMAIL-0005");
        assertThat(row[6]).isEqualTo("EMAIL-0005");
    }

    private void assertNotRedacted(UUID eventId) {
        Object[] row = row(eventId);
        assertThat(row[0].toString()).contains("secret@example.com");
        assertThat(row[1]).isNotNull();
        assertThat(row[2]).isNull();
    }

    private void assertRedactedWithoutLastError(UUID eventId, String status) {
        Object[] row = row(eventId);
        assertThat(row[0].toString()).isEqualTo("{}");
        assertThat(row[1]).isNull();
        assertThat(row[2]).isNotNull();
        assertThat(row[3]).isEqualTo(status);
        assertThat(row[4]).isEqualTo("a".repeat(64));
        assertThat(row[5]).isNull();
        assertThat(row[6]).isNull();
    }

    private Object[] row(UUID eventId) {
        return (Object[]) entityManager.createNativeQuery("""
                SELECT payload, traceparent, payload_redacted_at, status, payload_fingerprint, last_error,
                       sanitized_last_error
                FROM event_outbox
                WHERE event_id = :eventId
                """)
            .setParameter("eventId", eventId)
            .getSingleResult();
    }
}
