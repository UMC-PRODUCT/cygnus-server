package com.umc.product.recruiting.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Connection;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@DisplayName("Recruiting registration database invariants")
class RecruitingRegistrationDatabaseInvariantTest extends RecruitingFormApplicationMigrationTestSupport {

    @Test
    @DisplayName("FINAL_PASSED는 acceptedTrack이 필수이고 READY는 최종 합격에만 허용한다")
    void enforceAcceptedTrackAndRegistrationLifecycle() throws Exception {
        executeMigration();
        insertRoundFixtures();

        try (Connection connection = POSTGRES.createConnection(""); var statement = connection.createStatement()) {
            statement.executeUpdate("""
                INSERT INTO recruiting_application_form (
                    created_at, updated_at, recruiting_round_id, form_id, status
                ) VALUES (CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 200, 'DRAFT')
                """);
            statement.executeUpdate(applicationInsert("""
                1, 1, 1000, 100, '지원자', 'applicant@example.com',
                'PLAN', 'DESIGN', NULL, NULL, 'A1B2C3', NULL
                """));

            assertThatThrownBy(() -> statement.executeUpdate("""
                UPDATE recruiting_application SET status = 'FINAL_PASSED' WHERE id = 1
                """)).isInstanceOf(Exception.class);
            assertThatThrownBy(() -> statement.executeUpdate("""
                UPDATE recruiting_application SET registration_status = 'READY' WHERE id = 1
                """)).isInstanceOf(Exception.class);

            statement.executeUpdate("""
                UPDATE recruiting_application
                SET accepted_track = 'PLAN', status = 'FINAL_PASSED', registration_status = 'READY'
                WHERE id = 1
                """);
        }
    }

}
