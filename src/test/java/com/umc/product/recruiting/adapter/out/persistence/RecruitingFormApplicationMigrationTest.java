package com.umc.product.recruiting.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Connection;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@DisplayName("Recruiting Form Application v2 schema migration")
class RecruitingFormApplicationMigrationTest extends RecruitingFormApplicationMigrationTestSupport {

    @Test
    @DisplayName("legacy Recruiting 소스가 비어 있으면 v2 지원서 컬럼을 구성한다")
    void migrateLegacyRecruitingTables() throws Exception {
        clearLegacyRecruitingData();
        executeMigration();

        try (Connection connection = POSTGRES.createConnection("");
             var statement = connection.createStatement();
             ResultSet result = statement.executeQuery("""
                 SELECT column_name
                 FROM information_schema.columns
                 WHERE table_schema = 'public' AND table_name = 'recruiting_application'
                 """)) {
            List<String> columns = new ArrayList<>();
            while (result.next()) {
                columns.add(result.getString(1));
            }
            assertThat(columns).contains(
                "applicant_name",
                "applicant_email",
                "first_choice",
                "second_choice",
                "privacy_term_id",
                "privacy_agreed_at",
                "application_key",
                "accepted_track"
            );
            assertThat(columns).doesNotContain("applicant_identity_key", "application_no", "masked_email");
        }
    }

    @Test
    @DisplayName("Form은 Round당 하나이고 section policy type과 track 조합을 강제한다")
    void enforceFormAndSectionPolicyConstraints() throws Exception {
        clearLegacyRecruitingData();
        executeMigration();

        try (Connection connection = POSTGRES.createConnection(""); var statement = connection.createStatement()) {
            statement.executeUpdate("""
                INSERT INTO recruiting_application_form (
                    created_at, updated_at, recruiting_round_id, form_id, status
                ) VALUES (CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 200, 'DRAFT')
                """);
            assertThatThrownBy(() -> statement.executeUpdate("""
                INSERT INTO recruiting_application_form (
                    created_at, updated_at, recruiting_round_id, form_id, status
                ) VALUES (CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 201, 'DRAFT')
                """)).isInstanceOf(Exception.class);

            statement.executeUpdate("""
                INSERT INTO recruiting_form_section_policy (
                    created_at, updated_at, recruiting_application_form_id, form_section_id, type, track
                ) VALUES (CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 10, 'COMMON', NULL)
                """);
            assertThatThrownBy(() -> statement.executeUpdate("""
                INSERT INTO recruiting_form_section_policy (
                    created_at, updated_at, recruiting_application_form_id, form_section_id, type, track
                ) VALUES (CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 11, 'TRACK', NULL)
                """)).isInstanceOf(Exception.class);
        }
    }

    @Test
    @DisplayName("legacy Recruiting 소스가 비어 있지 않으면 데이터 삭제 전에 중단한다")
    void abortWhenLegacyRecruitingDataExists() {
        assertThatThrownBy(this::executeMigration)
            .hasMessageContaining("V2026.07.12.17.00 aborted")
            .hasMessageContaining("must be empty");
    }
}
