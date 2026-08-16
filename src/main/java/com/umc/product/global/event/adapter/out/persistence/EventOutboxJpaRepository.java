package com.umc.product.global.event.adapter.out.persistence;

import java.time.Instant;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.umc.product.global.event.domain.EventOutbox;

public interface EventOutboxJpaRepository extends JpaRepository<EventOutbox, Long> {

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
}
