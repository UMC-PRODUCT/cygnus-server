package com.umc.product.curriculum.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@DisplayName("커리큘럼 V2 불변식 Flyway migration")
class CurriculumV2InvariantMigrationTest {

    private static final String MIGRATION_PATH =
        "db/migration/V2026.07.23.00.00__enforce_curriculum_v2_submission_and_best_workbook_invariants.sql";

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:18-alpine");

    @BeforeEach
    void setUpLegacySchema() throws Exception {
        try (Connection connection = POSTGRES.createConnection(""); var statement = connection.createStatement()) {
            statement.execute("DROP SCHEMA public CASCADE");
            statement.execute("CREATE SCHEMA public");
            statement.execute("""
                CREATE TABLE weekly_curriculum (
                    id BIGINT PRIMARY KEY
                );
                CREATE TABLE mission_submission (
                    id BIGINT PRIMARY KEY,
                    challenger_workbook_id BIGINT NOT NULL
                );
                CREATE TABLE mission_feedback (
                    id BIGINT PRIMARY KEY,
                    mission_submission_id BIGINT NOT NULL
                );
                CREATE TABLE original_workbook (
                    id BIGINT PRIMARY KEY,
                    weekly_curriculum_id BIGINT NOT NULL
                );
                CREATE TABLE weekly_best_workbook (
                    id BIGINT PRIMARY KEY,
                    member_id BIGINT NOT NULL,
                    study_group_id BIGINT,
                    weekly_curriculum_id BIGINT NOT NULL,
                    CONSTRAINT uk_weekly_best_workbook_member_week_study_group
                        UNIQUE (member_id, study_group_id, weekly_curriculum_id)
                );
                CREATE TABLE study_group_schedule (
                    id BIGINT PRIMARY KEY,
                    study_group_id BIGINT NOT NULL,
                    schedule_id BIGINT NOT NULL
                );
                INSERT INTO weekly_curriculum (id) VALUES (1);
                """);
        }
    }

    @Test
    @DisplayName("레거시 스키마에 철회·일정 매핑·유일성·FK·조회 인덱스를 모두 적용한다")
    void appliesAllInvariantsAndIndexes() throws Exception {
        executeMigration();

        try (Connection connection = POSTGRES.createConnection(""); var statement = connection.createStatement()) {
            assertThat(columnExists(statement, "mission_submission", "withdrawn_at")).isTrue();
            assertThat(columnExists(statement, "study_group_schedule", "weekly_curriculum_id")).isTrue();
            assertThat(constraintExists(statement, "uk_weekly_best_workbook_study_group_week")).isTrue();
            assertThat(constraintExists(statement, "uk_study_group_schedule_group_week")).isTrue();
            assertThat(constraintExists(statement, "fk_study_group_schedule_weekly_curriculum")).isTrue();
            assertThat(indexExists(statement, "idx_mission_submission_active_workbook")).isTrue();
            assertThat(indexExists(statement, "idx_mission_feedback_submission")).isTrue();
            assertThat(indexExists(statement, "idx_weekly_best_workbook_week_id")).isTrue();
            assertThat(indexExists(statement, "idx_original_workbook_weekly_curriculum")).isTrue();

            statement.executeUpdate("""
                INSERT INTO weekly_best_workbook (id, member_id, study_group_id, weekly_curriculum_id)
                VALUES (1, 10, 100, 1)
                """);
            assertThatThrownBy(() -> statement.executeUpdate("""
                INSERT INTO weekly_best_workbook (id, member_id, study_group_id, weekly_curriculum_id)
                VALUES (2, 11, 100, 1)
                """)).isInstanceOf(SQLException.class);

            statement.executeUpdate("""
                INSERT INTO study_group_schedule (id, study_group_id, schedule_id, weekly_curriculum_id)
                VALUES (1, 100, 1000, 1)
                """);
            assertThatThrownBy(() -> statement.executeUpdate("""
                INSERT INTO study_group_schedule (id, study_group_id, schedule_id, weekly_curriculum_id)
                VALUES (2, 100, 1001, 1)
                """)).isInstanceOf(SQLException.class);
        }
    }

    @Test
    @DisplayName("기존 베스트에 그룹 NULL이 있으면 임의 보정하지 않고 migration을 중단한다")
    void rejectsLegacyNullBestGroup() throws Exception {
        try (Connection connection = POSTGRES.createConnection(""); var statement = connection.createStatement()) {
            statement.executeUpdate("""
                INSERT INTO weekly_best_workbook (id, member_id, study_group_id, weekly_curriculum_id)
                VALUES (1, 10, NULL, 1)
                """);
        }

        assertThatThrownBy(this::executeMigration)
            .isInstanceOf(SQLException.class)
            .hasMessageContaining("study_group_id contains NULL");
    }

    @Test
    @DisplayName("존재하지 않는 주차를 가리키는 일정 매핑이 있으면 migration을 중단한다")
    void rejectsDanglingScheduleWeek() throws Exception {
        try (Connection connection = POSTGRES.createConnection(""); var statement = connection.createStatement()) {
            statement.execute("ALTER TABLE study_group_schedule ADD COLUMN weekly_curriculum_id BIGINT");
            statement.executeUpdate("""
                INSERT INTO study_group_schedule (id, study_group_id, schedule_id, weekly_curriculum_id)
                VALUES (1, 100, 1000, 999)
                """);
        }

        assertThatThrownBy(this::executeMigration)
            .isInstanceOf(SQLException.class)
            .hasMessageContaining("dangling references");
    }

    private void executeMigration() throws Exception {
        String sql = new ClassPathResource(MIGRATION_PATH).getContentAsString(StandardCharsets.UTF_8);
        try (Connection connection = POSTGRES.createConnection(""); var statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private boolean columnExists(java.sql.Statement statement, String table, String column) throws SQLException {
        try (ResultSet result = statement.executeQuery("""
            SELECT EXISTS (
                SELECT 1
                FROM information_schema.columns
                WHERE table_schema = 'public'
                  AND table_name = '%s'
                  AND column_name = '%s'
            )
            """.formatted(table, column))) {
            result.next();
            return result.getBoolean(1);
        }
    }

    private boolean constraintExists(java.sql.Statement statement, String constraint) throws SQLException {
        try (ResultSet result = statement.executeQuery("""
            SELECT EXISTS (
                SELECT 1 FROM pg_constraint WHERE conname = '%s'
            )
            """.formatted(constraint))) {
            result.next();
            return result.getBoolean(1);
        }
    }

    private boolean indexExists(java.sql.Statement statement, String index) throws SQLException {
        try (ResultSet result = statement.executeQuery("""
            SELECT EXISTS (
                SELECT 1
                FROM pg_indexes
                WHERE schemaname = 'public'
                  AND indexname = '%s'
            )
            """.formatted(index))) {
            result.next();
            return result.getBoolean(1);
        }
    }
}
