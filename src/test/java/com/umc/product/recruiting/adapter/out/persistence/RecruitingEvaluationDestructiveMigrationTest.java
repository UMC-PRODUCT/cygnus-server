package com.umc.product.recruiting.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.core.io.ClassPathResource;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@DisplayName("Recruiting evaluation destructive migration safety")
class RecruitingEvaluationDestructiveMigrationTest extends RecruitingFormApplicationMigrationTestSupport {

    private static final String MIGRATION_PATH =
        "db/migration/V2026.07.13.10.00__replace_recruiting_evaluation_and_schedule.sql";

    @BeforeEach
    void setUpLegacyEvaluationTables() throws Exception {
        try (Connection connection = POSTGRES.createConnection(""); var statement = connection.createStatement()) {
            statement.execute("""
                CREATE TABLE recruiting_interview_evaluation (id BIGINT PRIMARY KEY);
                CREATE TABLE recruiting_interview_assignment (id BIGINT PRIMARY KEY);
                CREATE TABLE recruiting_evaluation_criterion (id BIGINT PRIMARY KEY);
                CREATE TABLE recruiting_evaluation_template (id BIGINT PRIMARY KEY);
                """);
        }
    }

    @Test
    @DisplayName("legacy 평가 소스 테이블이 모두 비어 있으면 신규 평가와 일정 테이블을 생성한다")
    void migrateWhenAllLegacyEvaluationTablesAreEmpty() throws Exception {
        executeEvaluationMigration();

        try (Connection connection = POSTGRES.createConnection(""); var statement = connection.createStatement()) {
            assertThat(tableExists(statement, "recruiting_application_evaluation")).isTrue();
            assertThat(tableExists(statement, "recruiting_interview_schedule")).isTrue();
            assertThat(tableExists(statement, "recruiting_interview_evaluation")).isFalse();
        }
    }

    @ParameterizedTest(name = "{0} 데이터가 있으면 중단")
    @ValueSource(strings = {
        "recruiting_interview_evaluation",
        "recruiting_interview_assignment",
        "recruiting_evaluation_criterion",
        "recruiting_evaluation_template"
    })
    @DisplayName("legacy 평가 소스가 비어 있지 않으면 DROP 전에 중단한다")
    void abortWhenLegacyEvaluationSourceContainsData(String sourceTable) throws Exception {
        try (Connection connection = POSTGRES.createConnection(""); var statement = connection.createStatement()) {
            statement.executeUpdate("INSERT INTO " + sourceTable + " VALUES (1)");
        }

        assertThatThrownBy(this::executeEvaluationMigration)
            .hasMessageContaining("V2026.07.13.10.00 aborted")
            .hasMessageContaining(sourceTable)
            .hasMessageContaining("must be empty");

        try (Connection connection = POSTGRES.createConnection(""); var statement = connection.createStatement()) {
            assertThat(tableExists(statement, sourceTable)).isTrue();
            assertThat(tableExists(statement, "recruiting_application_evaluation")).isFalse();
        }
    }

    private void executeEvaluationMigration() throws Exception {
        String sql = new ClassPathResource(MIGRATION_PATH).getContentAsString(StandardCharsets.UTF_8);
        try (Connection connection = POSTGRES.createConnection(""); var statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private boolean tableExists(java.sql.Statement statement, String tableName) throws Exception {
        try (var resultSet = statement.executeQuery(
            "SELECT TO_REGCLASS('public." + tableName + "') IS NOT NULL"
        )) {
            resultSet.next();
            return resultSet.getBoolean(1);
        }
    }
}
