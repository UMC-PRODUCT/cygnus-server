package com.umc.product.global.event.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jdbc.JdbcConnectionDetails;

import com.umc.product.support.IntegrationTestSupport;

@DisplayName("Event outbox retention Flyway migration 통합")
class EventOutboxRetentionFlywayMigrationIntegrationTest extends IntegrationTestSupport {

    private static final String PRE_IDEMPOTENCY_SCHEMA_VERSION = "2026.07.14.01.00";

    @Autowired
    private JdbcConnectionDetails connectionDetails;

    @Test
    @DisplayName("이전 schema의 legacy row를 보존하며 retention migration 전체를 적용한다")
    void testCase001() throws Exception {
        String databaseName = "outbox_retention_" + UUID.randomUUID().toString().replace("-", "");
        DatabaseUrls urls = databaseUrls(connectionDetails.getJdbcUrl(), databaseName);
        createDatabase(urls.adminUrl(), databaseName);

        try {
            createPostgisExtension(urls.databaseUrl());
            flyway(urls.databaseUrl(), PRE_IDEMPOTENCY_SCHEMA_VERSION).migrate();
            UUID legacyEventId = insertLegacyRow(urls.databaseUrl());

            flyway(urls.databaseUrl(), null).migrate();

            assertLatestSchemaAndLegacyRow(urls.databaseUrl(), legacyEventId);
        } finally {
            dropDatabase(urls.adminUrl(), databaseName);
        }
    }

    private Flyway flyway(String databaseUrl, String target) {
        var configuration = Flyway.configure()
            .dataSource(databaseUrl, connectionDetails.getUsername(), connectionDetails.getPassword())
            .locations("classpath:db/migration")
            .configuration(Map.of("flyway.postgresql.transactional.lock", "false"));
        if (target != null) {
            configuration.target(target);
        }
        return configuration.load();
    }

    private void createDatabase(String adminUrl, String databaseName) throws Exception {
        try (
            var connection = DriverManager.getConnection(
                adminUrl,
                connectionDetails.getUsername(),
                connectionDetails.getPassword()
            );
            var statement = connection.createStatement()
        ) {
            statement.execute("CREATE DATABASE " + databaseName);
        }
    }

    private void createPostgisExtension(String databaseUrl) throws Exception {
        try (
            var connection = DriverManager.getConnection(
                databaseUrl,
                connectionDetails.getUsername(),
                connectionDetails.getPassword()
            );
            var statement = connection.createStatement()
        ) {
            statement.execute("CREATE EXTENSION IF NOT EXISTS postgis");
        }
    }

    private UUID insertLegacyRow(String databaseUrl) throws Exception {
        UUID eventId = UUID.randomUUID();
        Instant now = Instant.parse("2026-07-23T00:00:00Z");
        try (
            var connection = DriverManager.getConnection(
                databaseUrl,
                connectionDetails.getUsername(),
                connectionDetails.getPassword()
            );
            var statement = connection.prepareStatement("""
                INSERT INTO event_outbox (
                    event_id, event_type, event_class, payload, status, attempts,
                    next_attempt_at, traceparent, version, created_at, updated_at
                ) VALUES (?, 'legacy.test', 'example.LegacyEvent', CAST(? AS jsonb),
                          'PENDING', 0, ?, NULL, 0, ?, ?)
                """)
        ) {
            statement.setObject(1, eventId);
            statement.setString(2, "{\"legacy\":true}");
            statement.setTimestamp(3, Timestamp.from(now));
            statement.setTimestamp(4, Timestamp.from(now));
            statement.setTimestamp(5, Timestamp.from(now));
            statement.executeUpdate();
        }
        return eventId;
    }

    private void assertLatestSchemaAndLegacyRow(String databaseUrl, UUID eventId) throws Exception {
        try (
            var connection = DriverManager.getConnection(
                databaseUrl,
                connectionDetails.getUsername(),
                connectionDetails.getPassword()
            );
            var statement = connection.createStatement()
        ) {
            try (var rowStatement = connection.prepareStatement("""
                SELECT payload::text, payload_fingerprint, available_at, payload_redacted_at,
                       sanitized_last_error
                FROM event_outbox
                WHERE event_id = ?
                """)) {
                rowStatement.setObject(1, eventId);
                try (ResultSet row = rowStatement.executeQuery()) {
                    assertThat(row.next()).isTrue();
                    assertThat(row.getString("payload")).contains("\"legacy\": true");
                    assertThat(row.getString("payload_fingerprint")).isNull();
                    assertThat(row.getObject("available_at")).isNull();
                    assertThat(row.getObject("payload_redacted_at")).isNull();
                    assertThat(row.getObject("sanitized_last_error")).isNull();
                }
            }
            try (ResultSet constraint = statement.executeQuery("""
                SELECT convalidated
                FROM pg_constraint
                WHERE conname = 'chk_event_outbox_payload_fingerprint_sha256'
                """)) {
                assertThat(constraint.next()).isTrue();
                assertThat(constraint.getBoolean("convalidated")).isTrue();
            }
            try (ResultSet indexes = statement.executeQuery("""
                SELECT COUNT(*), bool_and(index_metadata.indisvalid), bool_and(index_metadata.indisready)
                FROM pg_class index_class
                JOIN pg_namespace namespace ON namespace.oid = index_class.relnamespace
                JOIN pg_index index_metadata ON index_metadata.indexrelid = index_class.oid
                WHERE index_class.relname IN (
                    'idx_event_outbox_retention_published',
                    'idx_event_outbox_retention_failed'
                )
                  AND namespace.nspname = 'public'
                """)) {
                assertThat(indexes.next()).isTrue();
                assertThat(indexes.getInt(1)).isEqualTo(2);
                assertThat(indexes.getBoolean(2)).isTrue();
                assertThat(indexes.getBoolean(3)).isTrue();
            }
        }
    }

    private void dropDatabase(String adminUrl, String databaseName) throws Exception {
        try (
            var connection = DriverManager.getConnection(
                adminUrl,
                connectionDetails.getUsername(),
                connectionDetails.getPassword()
            );
            var statement = connection.createStatement()
        ) {
            statement.execute("DROP DATABASE IF EXISTS " + databaseName + " WITH (FORCE)");
        }
    }

    private DatabaseUrls databaseUrls(String currentUrl, String databaseName) {
        int queryIndex = currentUrl.indexOf('?');
        String base = queryIndex < 0 ? currentUrl : currentUrl.substring(0, queryIndex);
        String query = queryIndex < 0 ? "" : currentUrl.substring(queryIndex);
        int databaseSeparator = base.lastIndexOf('/');
        String server = base.substring(0, databaseSeparator + 1);
        return new DatabaseUrls(server + "postgres" + query, server + databaseName + query);
    }

    private record DatabaseUrls(String adminUrl, String databaseUrl) {
    }
}
