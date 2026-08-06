package com.umc.product.community.adapter.in.websocket;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.micrometer.core.instrument.MeterRegistry;

final class CommunityThreadOutboxProbe {

    private static final String INVITED_EVENT_TYPE = "community.thread.invited";

    private final JdbcTemplate jdbcTemplate;
    private final MeterRegistry meterRegistry;
    private final ObjectMapper objectMapper;

    CommunityThreadOutboxProbe(ApplicationContext context) {
        this(
            context.getBean(JdbcTemplate.class),
            context.getBean(MeterRegistry.class),
            context.getBean(ObjectMapper.class)
        );
    }

    CommunityThreadOutboxProbe(
        JdbcTemplate jdbcTemplate,
        MeterRegistry meterRegistry,
        ObjectMapper objectMapper
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.meterRegistry = meterRegistry;
        this.objectMapper = objectMapper;
    }

    OutboxRow awaitInvitation(Long threadId, Long invitedMemberId, Duration timeout) {
        long deadline = System.nanoTime() + timeout.toNanos();
        while (true) {
            List<InvitationOutboxRow> rows = jdbcTemplate.query(
                """
                    SELECT event_id, payload::text AS payload_text, status, attempts, last_error
                    FROM event_outbox
                    WHERE event_type = ?
                    ORDER BY id DESC
                    """,
                (resultSet, rowNumber) -> new InvitationOutboxRow(
                    new OutboxRow(
                        resultSet.getObject("event_id", UUID.class),
                        resultSet.getString("status"),
                        resultSet.getInt("attempts"),
                        resultSet.getString("last_error")
                    ),
                    resultSet.getString("payload_text")
                ),
                INVITED_EVENT_TYPE
            );
            OutboxRow invitation = rows.stream()
                .filter(row -> matches(row, threadId, invitedMemberId))
                .map(InvitationOutboxRow::outbox)
                .findFirst()
                .orElse(null);
            if (invitation != null) {
                return invitation;
            }
            if (System.nanoTime() >= deadline) {
                throw new AssertionError(
                    "commit된 community.thread.invited outbox를 찾지 못했습니다: threadId="
                        + threadId + ", memberId=" + invitedMemberId
                        + ", broadRows=" + describeBroadRows()
                );
            }
            Thread.onSpinWait();
        }
    }

    OutboxRow get(UUID eventId) {
        return jdbcTemplate.queryForObject(
            """
                SELECT event_id, status, attempts, last_error
                FROM event_outbox
                WHERE event_id = ?
                """,
            (resultSet, rowNumber) -> new OutboxRow(
                resultSet.getObject("event_id", UUID.class),
                resultSet.getString("status"),
                resultSet.getInt("attempts"),
                resultSet.getString("last_error")
            ),
            eventId
        );
    }

    void makePublishable(UUID eventId) {
        int updated = jdbcTemplate.update(
            """
                UPDATE event_outbox
                SET next_attempt_at = CURRENT_TIMESTAMP
                WHERE event_id = ? AND status = 'PENDING'
                """,
            eventId
        );
        if (updated != 1) {
            throw new AssertionError("PENDING outbox를 재시도 가능 상태로 만들지 못했습니다: " + eventId);
        }
    }

    double retryCount() {
        return meterRegistry.get("event.outbox.relay.retry").counter().count();
    }

    double failedCount() {
        return meterRegistry.get("event.outbox.relay.failed").counter().count();
    }

    private boolean matches(InvitationOutboxRow row, Long threadId, Long invitedMemberId) {
        JsonNode payload;
        try {
            payload = objectMapper.readTree(row.payload());
        } catch (JsonProcessingException exception) {
            throw new AssertionError("초대 outbox payload를 파싱하지 못했습니다: eventId="
                + row.outbox().eventId(), exception);
        }
        if (payload.path("threadId").asLong(-1L) != threadId.longValue()) {
            return false;
        }
        for (JsonNode memberId : payload.path("invitedMemberIds")) {
            if (memberId.asLong(-1L) == invitedMemberId.longValue()) {
                return true;
            }
        }
        return false;
    }

    private List<String> describeBroadRows() {
        return jdbcTemplate.query(
            """
                SELECT event_id, event_type, payload::text AS payload_text, status, attempts
                FROM event_outbox
                ORDER BY id DESC
                LIMIT 50
                """,
            (resultSet, rowNumber) -> describe(new BroadOutboxRow(
                resultSet.getObject("event_id", UUID.class),
                resultSet.getString("event_type"),
                resultSet.getString("payload_text"),
                resultSet.getString("status"),
                resultSet.getInt("attempts")
            ))
        );
    }

    private String describe(BroadOutboxRow row) {
        try {
            JsonNode storedPayload = objectMapper.readTree(row.payload());
            JsonNode eventPayload = storedPayload.isTextual()
                ? objectMapper.readTree(storedPayload.textValue())
                : storedPayload;
            List<String> keys = new ArrayList<>();
            eventPayload.fieldNames().forEachRemaining(keys::add);
            return "{eventId=" + row.eventId()
                + ", eventType=" + row.eventType()
                + ", status=" + row.status()
                + ", attempts=" + row.attempts()
                + ", storedJsonType=" + storedPayload.getNodeType()
                + ", keys=" + keys
                + ", threadId=" + eventPayload.path("threadId").asText()
                + ", invitedMemberIds=" + eventPayload.path("invitedMemberIds")
                + "}";
        } catch (JsonProcessingException exception) {
            return "{eventId=" + row.eventId()
                + ", eventType=" + row.eventType()
                + ", status=" + row.status()
                + ", attempts=" + row.attempts()
                + ", payloadParseError=" + exception.getClass().getSimpleName()
                + "}";
        }
    }

    record OutboxRow(UUID eventId, String status, int attempts, String lastError) {
    }

    private record InvitationOutboxRow(OutboxRow outbox, String payload) {
    }

    private record BroadOutboxRow(
        UUID eventId,
        String eventType,
        String payload,
        String status,
        int attempts
    ) {
    }
}
