package com.umc.product.global.event.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import javax.sql.DataSource;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import com.umc.product.global.event.application.port.out.RedactEventOutboxPayloadPort;
import com.umc.product.support.IntegrationTestSupport;

@DisplayName("Event outbox payload retention lock 통합")
class EventOutboxRetentionLockIntegrationTest extends IntegrationTestSupport {

    private static final Instant NOW = Instant.parse("2026-07-23T00:00:00Z");

    @Autowired
    private DataSource dataSource;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private RedactEventOutboxPayloadPort redactPort;

    @Test
    @DisplayName("다른 worker가 잠근 행은 기다리지 않고 건너뛴다")
    void testCase001() throws SQLException {
        UUID lockedEventId = insertPublished(NOW.minus(Duration.ofDays(2)));
        UUID availableEventId = insertPublished(NOW.minus(Duration.ofDays(2)));

        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            lock(connection, lockedEventId);

            int redacted = redactPort.redactPublishedBefore(NOW.minus(Duration.ofHours(24)), NOW, 500);

            assertThat(redacted).isOne();
            assertThat(payloadRedactedAt(lockedEventId)).isNull();
            assertThat(payloadRedactedAt(availableEventId)).isEqualTo(NOW);
            connection.rollback();
        }
    }

    private UUID insertPublished(Instant publishedAt) {
        UUID eventId = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO event_outbox (
                    event_id, event_type, event_class, payload, status, attempts,
                    next_attempt_at, published_at, traceparent, version,
                    created_at, updated_at, payload_fingerprint, available_at
                ) VALUES (
                    ?, 'retention.lock.test', 'example.RetentionLockEvent',
                    CAST('{"recipient":"secret@example.com"}' AS jsonb), 'PUBLISHED', 1,
                    ?, ?, '00-0af7651916cd43dd8448eb211c80319c-b7ad6b7169203331-01', 0,
                    ?, ?, ?, ?
                )
                """,
            eventId,
            Timestamp.from(publishedAt),
            Timestamp.from(publishedAt),
            Timestamp.from(publishedAt),
            Timestamp.from(publishedAt),
            "b".repeat(64),
            Timestamp.from(publishedAt)
        );
        return eventId;
    }

    private void lock(Connection connection, UUID eventId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
            "SELECT id FROM event_outbox WHERE event_id = ? FOR UPDATE"
        )) {
            statement.setObject(1, eventId);
            try (ResultSet resultSet = statement.executeQuery()) {
                assertThat(resultSet.next()).isTrue();
            }
        }
    }

    private Instant payloadRedactedAt(UUID eventId) {
        return jdbcTemplate.queryForObject(
            "SELECT payload_redacted_at FROM event_outbox WHERE event_id = ?",
            (resultSet, rowNumber) -> {
                Timestamp value = resultSet.getTimestamp("payload_redacted_at");
                return value == null ? null : value.toInstant();
            },
            eventId
        );
    }
}
