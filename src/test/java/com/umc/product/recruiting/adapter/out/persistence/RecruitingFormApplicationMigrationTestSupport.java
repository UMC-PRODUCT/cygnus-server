package com.umc.product.recruiting.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.core.io.ClassPathResource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;

abstract class RecruitingFormApplicationMigrationTestSupport {

    private static final String MIGRATION_PATH =
        "db/migration/V2026.07.15.13.30__create_recruiting_domain.sql";

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:18-alpine");

    @BeforeEach
    void setUpEmptySchema() throws Exception {
        try (Connection connection = POSTGRES.createConnection(""); var statement = connection.createStatement()) {
            statement.execute("DROP SCHEMA public CASCADE");
            statement.execute("CREATE SCHEMA public");
            statement.execute("CREATE TABLE unrelated_domain_sentinel (id BIGINT PRIMARY KEY)");
            statement.execute("INSERT INTO unrelated_domain_sentinel VALUES (1)");
        }
    }

    void executeMigration() throws Exception {
        String sql = new ClassPathResource(MIGRATION_PATH).getContentAsString(StandardCharsets.UTF_8);
        try (Connection connection = POSTGRES.createConnection(""); var statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    void insertRoundFixtures() throws Exception {
        try (Connection connection = POSTGRES.createConnection(""); var statement = connection.createStatement()) {
            statement.execute("""
                INSERT INTO recruiting_season (
                    created_at, updated_at, gisu_id, school_id, status
                ) VALUES (CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 9, 90, 'DRAFT');
                INSERT INTO recruiting_round (
                    created_at, updated_at, recruiting_season_id, type, round_no, status
                ) VALUES
                    (CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 'REGULAR', 1, 'DRAFT'),
                    (CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 'ADDITIONAL', 2, 'DRAFT');
                """);
        }
    }

    String applicationInsert(String values) {
        return """
            INSERT INTO recruiting_application (
                created_at, updated_at, recruiting_round_id, recruiting_application_form_id,
                form_response_id, applicant_member_id, applicant_name, applicant_email,
                first_choice, second_choice, privacy_term_id, privacy_agreed_at,
                application_key, accepted_track, status, registration_status
            ) VALUES (CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, %s, 'DRAFT', 'NOT_READY')
            """.formatted(values.strip());
    }

    void assertApplicationRejected(java.sql.Statement statement, String values) {
        assertThatThrownBy(() -> statement.executeUpdate(applicationInsert(values)))
            .isInstanceOf(Exception.class);
    }
}
