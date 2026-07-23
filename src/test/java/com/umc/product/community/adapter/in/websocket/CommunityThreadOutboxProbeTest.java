package com.umc.product.community.adapter.in.websocket;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.umc.product.community.adapter.in.websocket.CommunityThreadOutboxProbe.OutboxRow;
import com.umc.product.support.PersistenceAdapterTest;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

@PersistenceAdapterTest
@DisplayName("Community thread outbox probe")
class CommunityThreadOutboxProbeTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("문자열 ID로 직렬화된 초대 payload row를 찾는다")
    void findsInvitationWhoseIdsAreSerializedAsStrings() {
        // given
        UUID eventId = UUID.randomUUID();
        Instant now = Instant.parse("2026-07-18T00:00:00Z");
        Timestamp timestamp = Timestamp.from(now);
        jdbcTemplate.update(
            """
                INSERT INTO event_outbox (
                    event_id, event_type, event_class, payload, status, attempts,
                    next_attempt_at, created_at, updated_at
                ) VALUES (?, 'community.thread.invited', 'test.InvitedEvent',
                    CAST(? AS jsonb), 'PENDING', 0, ?, ?, ?)
                """,
            eventId,
            """
                {"eventId":"%s","threadId":"1","occurredAt":"2026-07-18T00:00:00Z",\
                "inviterMemberId":"2","invitedMemberIds":["4"]}
                """.formatted(eventId),
            timestamp,
            timestamp,
            timestamp
        );
        CommunityThreadOutboxProbe probe = new CommunityThreadOutboxProbe(
            jdbcTemplate,
            new SimpleMeterRegistry(),
            new ObjectMapper()
        );

        // when
        OutboxRow row = probe.awaitInvitation(1L, 4L, Duration.ZERO);

        // then
        assertThat(row.eventId()).isEqualTo(eventId);
        assertThat(row.status()).isEqualTo("PENDING");
        assertThat(row.attempts()).isZero();
    }
}
