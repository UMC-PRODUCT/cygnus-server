package com.umc.product.recruiting.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Connection;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@DisplayName("Recruiting Application v2 database invariants")
class RecruitingApplicationDatabaseInvariantTest extends RecruitingFormApplicationMigrationTestSupport {

    @Test
    @DisplayName("다른 도메인 데이터는 보존하고 지원서 세 unique constraint를 강제한다")
    void preserveOtherDomainAndEnforceUniqueConstraints() throws Exception {
        clearLegacyRecruitingData();
        executeMigration();

        try (Connection connection = POSTGRES.createConnection(""); var statement = connection.createStatement()) {
            assertThat(statement.executeQuery("SELECT id FROM unrelated_domain_sentinel").next()).isTrue();
            insertApplicationForms(statement);
            statement.executeUpdate(applicationInsert("""
                1, 1, 1000, 100, '지원자', 'applicant@example.com',
                'PLAN', 'DESIGN', NULL, NULL, 'A1B2C3', NULL
                """));

            assertApplicationRejected(statement, """
                1, 1, 1001, 100, '회원중복', 'member-duplicate@example.com',
                'PLAN', NULL, NULL, NULL, 'D4E5F6', NULL
                """);
            assertApplicationRejected(statement, """
                1, 1, 1002, 101, '이메일중복', 'applicant@example.com',
                'DESIGN', NULL, NULL, NULL, 'G7H8I9', NULL
                """);
            assertApplicationRejected(statement, """
                2, 2, 2000, 200, '키중복', 'applicant@example.com',
                'PLAN', NULL, NULL, NULL, 'A1B2C3', NULL
                """);
        }
    }

    @Test
    @DisplayName("지원서 key email name privacy choice accepted track CHECK를 강제한다")
    void enforceApplicationCheckConstraints() throws Exception {
        clearLegacyRecruitingData();
        executeMigration();

        try (Connection connection = POSTGRES.createConnection(""); var statement = connection.createStatement()) {
            insertApplicationForms(statement);
            statement.executeUpdate(applicationInsert("""
                1, 1, 1000, 100, '지원자', 'applicant@example.com',
                'PLAN', 'DESIGN', NULL, NULL, 'A1B2C3', NULL
                """));

            assertApplicationRejected(statement, """
                2, 2, 2001, 201, '키형식', 'key-format@example.com',
                'PLAN', NULL, NULL, NULL, 'abc123', NULL
                """);
            assertApplicationRejected(statement, """
                2, 2, 2002, 202, '이메일형식', 'a@@b.com',
                'PLAN', NULL, NULL, NULL, 'J1K2L3', NULL
                """);
            assertApplicationRejected(statement, """
                2, 2, 2007, 207, '이메일정규화', 'Upper@Example.com',
                'PLAN', NULL, NULL, NULL, 'Y7Z8A9', NULL
                """);
            assertApplicationRejected(statement, """
                2, 2, 2008, 208, '공백 이름', 'name@example.com',
                'PLAN', NULL, NULL, NULL, 'B1C2D3', NULL
                """);
            assertApplicationRejected(statement, """
                2, 2, 2003, 203, '1지망', 'first-choice@example.com',
                'INFRA_PLUS', NULL, NULL, NULL, 'M4N5O6', NULL
                """);
            assertApplicationRejected(statement, """
                2, 2, 2004, 204, '2지망', 'second-choice@example.com',
                'PLAN', 'INFRA_PLUS', NULL, NULL, 'P7Q8R9', NULL
                """);
            assertApplicationRejected(statement, """
                2, 2, 2005, 205, '개인정보', 'privacy@example.com',
                'PLAN', NULL, 10, NULL, 'S1T2U3', NULL
                """);
            assertApplicationRejected(statement, """
                2, 2, 2006, 206, '합격트랙', 'accepted@example.com',
                'PLAN', 'DESIGN', NULL, NULL, 'V4W5X6', 'WEB_PRODUCT_ENGINEER'
                """);
        }
    }

    @Test
    @DisplayName("익명 지원서는 회원 ID 없이 개인정보 증적과 Form access key를 모두 가져야 한다")
    void enforceAnonymousIdentityModeConstraint() throws Exception {
        clearLegacyRecruitingData();
        executeMigration();

        try (Connection connection = POSTGRES.createConnection(""); var statement = connection.createStatement()) {
            insertApplicationForms(statement);
            statement.executeUpdate("""
                INSERT INTO recruiting_application (
                    created_at, updated_at, recruiting_round_id, recruiting_application_form_id,
                    form_response_id, applicant_member_id, applicant_name, applicant_email,
                    first_choice, second_choice, privacy_term_id, privacy_agreed_at,
                    application_key, accepted_track, status, registration_status, form_response_access_key
                ) VALUES (
                    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 1,
                    3000, NULL, '익명지원자', 'anonymous@example.com',
                    'PLAN', NULL, 10, CURRENT_TIMESTAMP,
                    'N1M2O3', NULL, 'DRAFT', 'NOT_READY', 'raw-form-access-key'
                )
                """);

            assertThatThrownBy(() -> statement.executeUpdate("""
                INSERT INTO recruiting_application (
                    created_at, updated_at, recruiting_round_id, recruiting_application_form_id,
                    form_response_id, applicant_member_id, applicant_name, applicant_email,
                    first_choice, second_choice, privacy_term_id, privacy_agreed_at,
                    application_key, accepted_track, status, registration_status, form_response_access_key
                ) VALUES (
                    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 2, 2,
                    3001, NULL, '키누락', 'missing-key@example.com',
                    'PLAN', NULL, 10, CURRENT_TIMESTAMP,
                    'P4Q5R6', NULL, 'DRAFT', 'NOT_READY', NULL
                )
                """)).isInstanceOf(Exception.class);
        }
    }

    private void insertApplicationForms(java.sql.Statement statement) throws Exception {
        statement.executeUpdate("""
            INSERT INTO recruiting_application_form (
                created_at, updated_at, recruiting_round_id, form_id, status
            ) VALUES
                (CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 200, 'DRAFT'),
                (CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 2, 201, 'DRAFT')
            """);
    }
}
