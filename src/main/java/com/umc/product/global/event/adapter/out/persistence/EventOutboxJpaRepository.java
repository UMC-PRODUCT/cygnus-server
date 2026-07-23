package com.umc.product.global.event.adapter.out.persistence;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.umc.product.global.event.domain.EventOutbox;

public interface EventOutboxJpaRepository extends JpaRepository<EventOutbox, Long> {

    Optional<EventOutbox> findByEventId(UUID eventId);

    @Modifying
    @Query(value = """
        INSERT INTO event_outbox (
            event_id,
            event_type,
            event_class,
            payload,
            payload_fingerprint,
            available_at,
            next_attempt_at,
            traceparent,
            status,
            attempts,
            version,
            created_at,
            updated_at
        ) VALUES (
            :#{#outbox.eventId},
            :#{#outbox.eventType},
            :#{#outbox.eventClass},
            CAST(:#{#outbox.payload} AS jsonb),
            :#{#outbox.payloadFingerprint},
            :#{#outbox.availableAt},
            :#{#outbox.nextAttemptAt},
            :#{#outbox.traceparent},
            'PENDING',
            0,
            0,
            CURRENT_TIMESTAMP,
            CURRENT_TIMESTAMP
        )
        ON CONFLICT (event_id) DO NOTHING
        """, nativeQuery = true)
    int insertIfAbsent(@Param("outbox") EventOutbox outbox);

    @Query(value = """
        SELECT *
        FROM event_outbox
        WHERE status IN ('PENDING', 'PROCESSING')
          AND next_attempt_at <= :now
        ORDER BY next_attempt_at, id
        FOR UPDATE SKIP LOCKED
        LIMIT :limit
        """, nativeQuery = true)
    List<EventOutbox> findPublishableForUpdate(@Param("limit") int limit, @Param("now") Instant now);

    @Modifying
    @Query(value = """
        WITH candidates AS (
            SELECT id
            FROM event_outbox
            WHERE status = 'PUBLISHED'
              AND payload_redacted_at IS NULL
              AND published_at <= :threshold
            ORDER BY published_at, id
            FOR UPDATE SKIP LOCKED
            LIMIT :limit
        )
        UPDATE event_outbox outbox
        SET payload = CAST('{}' AS jsonb),
            traceparent = NULL,
            last_error = CASE
                WHEN last_error = sanitized_last_error THEN sanitized_last_error
                ELSE NULL
            END,
            sanitized_last_error = CASE
                WHEN last_error = sanitized_last_error THEN sanitized_last_error
                ELSE NULL
            END,
            payload_redacted_at = :redactedAt,
            updated_at = :redactedAt
        FROM candidates
        WHERE outbox.id = candidates.id
        """, nativeQuery = true)
    int redactPublishedBefore(
        @Param("threshold") Instant threshold,
        @Param("redactedAt") Instant redactedAt,
        @Param("limit") int limit
    );

    @Modifying
    @Query(value = """
        WITH candidates AS (
            SELECT id
            FROM event_outbox
            WHERE status = 'FAILED'
              AND payload_redacted_at IS NULL
              AND updated_at <= :threshold
            ORDER BY updated_at, id
            FOR UPDATE SKIP LOCKED
            LIMIT :limit
        )
        UPDATE event_outbox outbox
        SET payload = CAST('{}' AS jsonb),
            traceparent = NULL,
            last_error = CASE
                WHEN last_error = sanitized_last_error THEN sanitized_last_error
                ELSE NULL
            END,
            sanitized_last_error = CASE
                WHEN last_error = sanitized_last_error THEN sanitized_last_error
                ELSE NULL
            END,
            payload_redacted_at = :redactedAt,
            updated_at = :redactedAt
        FROM candidates
        WHERE outbox.id = candidates.id
        """, nativeQuery = true)
    int redactFailedBefore(
        @Param("threshold") Instant threshold,
        @Param("redactedAt") Instant redactedAt,
        @Param("limit") int limit
    );
}
