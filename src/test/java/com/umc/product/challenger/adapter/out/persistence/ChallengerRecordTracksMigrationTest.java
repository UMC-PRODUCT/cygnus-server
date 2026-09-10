package com.umc.product.challenger.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;

import com.p6spy.engine.wrapper.P6Proxy;
import com.umc.product.support.PersistenceAdapterTest;

@PersistenceAdapterTest
@DisplayName("챌린저 코드의 복수 트랙 Flyway 마이그레이션")
class ChallengerRecordTracksMigrationTest {

    private static final String MIGRATION_PATH =
        "db/migration/V2026.09.10.04.00__support_challenger_record_tracks.sql";

    @Autowired private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void preparePreviousSchemaWithinTestTransaction() {
        // 실제 Flyway 스키마에서 이번 변경만 되돌려 이전 코드 데이터를 준비한다. 테스트 종료 시 함께 롤백된다.
        jdbcTemplate.update("DELETE FROM challenger_record");
        jdbcTemplate.execute("ALTER TABLE challenger_record DROP COLUMN tracks CASCADE");
        jdbcTemplate.execute("""
            ALTER TABLE challenger_record
                ALTER COLUMN chapter_id SET NOT NULL,
                ADD CONSTRAINT challenger_record_track_registration_check
                    CHECK (track IS NULL OR (part IS NULL AND challenger_role_type IS NULL AND organization_id IS NULL))
            """);
        jdbcTemplate.update("""
            INSERT INTO challenger_record
                (code, member_name, created_member_id, gisu_id, chapter_id, school_id, part, track,
                 challenger_role_type, organization_id, is_used, used_member_id, used_at, created_at, updated_at)
            VALUES
                ('PART01', '기존 파트', 1, 10, 2, 3, 'SPRINGBOOT', NULL, NULL, NULL, false, NULL, NULL, now(), now()),
                ('TRACK1', '사용한 코드', 1, 11, 2, 3, NULL, 'WEB_PRODUCT_ENGINEER', NULL, NULL,
                    true, 10, '2026-09-10T01:00:00Z', now(), now()),
                ('STAFF1', '담당 운영진', 1, 11, 2, 3, 'DESIGN', NULL, 'SCHOOL_PART_LEADER', 3,
                    false, NULL, NULL, now(), now())
            """);
    }

    @Test
    @DisplayName("기존 코드와 사용 이력은 보존하고 단일 트랙만 배열로 옮기며 담당 파트는 수강으로 바꾸지 않는다")
    void preservesExistingCodesAndBackfillsOnlyEnrollmentTrack() throws Exception {
        // Given
        List<String> previous = readLegacyRows();

        // When
        executeMigration();

        // Then
        assertThat(readLegacyRows()).isEqualTo(previous);
        assertThat(jdbcTemplate.queryForList(
            "SELECT code || ':' || tracks::TEXT FROM challenger_record ORDER BY code", String.class))
            .containsExactly("PART01:{}", "STAFF1:{}", "TRACK1:{WEB_PRODUCT_ENGINEER}");
    }

    @Test
    @DisplayName("복수 트랙과 담당 역할을 함께 저장하고 비수강 중앙 운영진은 지부 없이 저장한다")
    void storesMultipleTracksWithRoleAndUnassignedCentralStaff() throws Exception {
        // Given
        executeMigration();

        // When
        jdbcTemplate.update("""
            UPDATE challenger_record SET tracks = ARRAY['DESIGN', 'WEB_PRODUCT_ENGINEER']::TEXT[]
            WHERE code = 'STAFF1'
            """);
        jdbcTemplate.update("""
            UPDATE challenger_record SET challenger_role_type = 'CENTRAL_OPERATING_TEAM_MEMBER',
                chapter_id = NULL, organization_id = NULL WHERE code = 'PART01'
            """);

        // Then
        assertThat(jdbcTemplate.queryForObject(
            "SELECT tracks::TEXT FROM challenger_record WHERE code = 'STAFF1'", String.class))
            .isEqualTo("{DESIGN,WEB_PRODUCT_ENGINEER}");
        assertThat(jdbcTemplate.queryForObject(
            "SELECT chapter_id FROM challenger_record WHERE code = 'PART01'", Long.class)).isNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "ARRAY['INFRA_PLUS']::TEXT[]", "ARRAY['PLAN', NULL]::TEXT[]", "ARRAY['DESIGN', 'DESIGN']::TEXT[]", "NULL"
    })
    @DisplayName("PLUS와 null 및 중복 트랙을 데이터베이스에서도 거부한다")
    void rejectsInvalidEnrollmentTracks(String tracksSql) throws Exception {
        // Given
        executeMigration();

        // When / Then
        assertThatThrownBy(() -> executeSql(
            "UPDATE challenger_record SET tracks = " + tracksSql + " WHERE code = 'STAFF1'"))
            .isInstanceOfSatisfying(DataIntegrityViolationException.class, exception -> {
                SQLException sqlException = (SQLException)exception.getMostSpecificCause();
                assertThat(sqlException.getSQLState()).isEqualTo(tracksSql.equals("NULL") ? "23502" : "23514");
            });
        assertThat(jdbcTemplate.queryForObject(
            "SELECT tracks::TEXT FROM challenger_record WHERE code = 'STAFF1'", String.class)).isEqualTo("{}");
    }

    @ParameterizedTest
    @ValueSource(strings = {"NULL", "'SCHOOL_PRESIDENT'", "'CENTRAL_PRESIDENT'"})
    @DisplayName("일반 코드와 학교 운영진 및 수강 중인 중앙 운영진의 지부 생략을 거부한다")
    void rejectsMissingChapterOutsideNonLearningCentralStaff(String roleSql) throws Exception {
        // Given
        executeMigration();

        // When / Then
        assertThatThrownBy(() -> executeSql("""
            UPDATE challenger_record SET chapter_id = NULL, challenger_role_type = %s WHERE code = 'TRACK1'
            """.formatted(roleSql))).isInstanceOf(DataIntegrityViolationException.class);
        assertThat(jdbcTemplate.queryForObject(
            "SELECT chapter_id FROM challenger_record WHERE code = 'TRACK1'", Long.class)).isEqualTo(2L);
    }

    private List<String> readLegacyRows() {
        return jdbcTemplate.queryForList(
            "SELECT (to_jsonb(record) - 'tracks')::TEXT FROM challenger_record record ORDER BY code", String.class);
    }

    private void executeMigration() throws Exception {
        executeSql(new ClassPathResource(MIGRATION_PATH).getContentAsString(StandardCharsets.UTF_8));
    }

    private void executeSql(String sql) {
        jdbcTemplate.execute((ConnectionCallback<Void>) connection -> {
            var savepoint = connection.setSavepoint();
            try (var statement = connection.createStatement()) {
                // DB 제약 검증이 전역 P6Spy 포매터의 배열 SQL 처리에 영향받지 않도록 실제 JDBC로 실행한다.
                Statement jdbcStatement = statement instanceof P6Proxy proxy
                    ? (Statement)proxy.unwrapP6SpyProxy() : statement;
                jdbcStatement.execute(sql);
            } catch (SQLException | RuntimeException exception) {
                connection.rollback(savepoint);
                throw exception;
            } finally {
                connection.releaseSavepoint(savepoint);
            }
            return null;
        });
    }
}
