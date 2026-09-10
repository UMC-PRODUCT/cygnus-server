package com.umc.product.curriculum.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.umc.product.support.PersistenceAdapterTest;

@PersistenceAdapterTest
@DisplayName("11기 커리큘럼 목차 Flyway 마이그레이션")
class CurriculumOutlineMigrationTest {

    private static final String MIGRATION_PATH =
        "db/migration/V2026.09.10.02.00__seed_11th_weekly_curricula.sql";
    private static final LocalDateTime START_AT = LocalDateTime.parse("2026-08-31T15:00:00");
    private static final LocalDateTime END_AT = LocalDateTime.parse("2027-02-27T14:59:59.999999");

    @Autowired private JdbcTemplate jdbcTemplate;

    private Long gisuId;

    @BeforeEach
    void prepareEmptyEleventhCurricula() {
        List<Long> existingGisuIds = jdbcTemplate.queryForList(
            "SELECT id FROM gisu WHERE generation = 11", Long.class);
        if (existingGisuIds.isEmpty()) {
            gisuId = jdbcTemplate.queryForObject("""
                INSERT INTO gisu (generation, is_active, learning_type, start_at, end_at, created_at, updated_at)
                VALUES (11, false, 'TRACK', '2026-09-01T00:00:00+09:00',
                    '2027-02-27T23:59:59.999999+09:00', now(), now())
                RETURNING id
                """, Long.class);
        } else {
            assertThat(existingGisuIds).hasSize(1);
            gisuId = existingGisuIds.getFirst();
            jdbcTemplate.update("""
                UPDATE gisu SET learning_type = 'TRACK',
                    start_at = TIMESTAMPTZ '2026-09-01T00:00:00+09:00',
                    end_at = TIMESTAMPTZ '2027-02-27T23:59:59.999999+09:00'
                WHERE id = ?
                """, gisuId);
        }
        jdbcTemplate.update("""
            INSERT INTO curriculum (gisu_id, track, title, created_at, updated_at)
            SELECT ?, track, track || ' 커리큘럼', now(), now()
            FROM (VALUES ('PLAN'), ('DESIGN'), ('MOBILE_PRODUCT_ENGINEER'), ('WEB_PRODUCT_ENGINEER')) AS seed(track)
            ON CONFLICT (gisu_id, track) DO NOTHING
            """, gisuId);
        jdbcTemplate.update("""
            DELETE FROM weekly_curriculum
            WHERE curriculum_id IN (SELECT id FROM curriculum WHERE gisu_id = ?)
            """, gisuId);
    }

    @ParameterizedTest
    @ValueSource(strings = {"UTC", "Asia/Seoul"})
    @DisplayName("세션 시간대와 관계없이 승인된 44개 목차를 기수 전체 기간의 UTC 시각으로 등록한다")
    void importsApprovedOutlineWithUtcDates(String timeZone) throws Exception {
        // Given
        jdbcTemplate.execute("SET LOCAL TIME ZONE '" + timeZone + "'");
        Map<String, Long> previousCounts = nonWeeklyCounts();

        // When
        executeMigration();

        // Then
        var weeks = readWeeks(gisuId);
        assertThat(weeks).hasSize(44);
        assertThat(weeks.stream().collect(Collectors.groupingBy(WeeklyRow::track, Collectors.counting())))
            .isEqualTo(Map.of("PLAN", 11L, "DESIGN", 13L,
                "MOBILE_PRODUCT_ENGINEER", 10L, "WEB_PRODUCT_ENGINEER", 10L));
        assertThat(weeks).allSatisfy(week -> {
            assertThat(week.startsAt()).isEqualTo(START_AT);
            assertThat(week.endsAt()).isEqualTo(END_AT);
        });
        assertThat(weeks.stream().map(week -> new OutlineRow(
            week.track(), week.weekNo(), week.extra(), week.title())).toList())
            .containsExactlyInAnyOrderElementsOf(readApprovedOutline());
        assertThat(weeks).filteredOn(week -> week.weekNo() == 0)
            .extracting(WeeklyRow::track).containsExactlyInAnyOrder("PLAN", "DESIGN");
        assertThat(weeks).filteredOn(WeeklyRow::extra)
            .extracting(WeeklyRow::title).containsExactlyInAnyOrder("협업 가이드", "디자인 인사이트");
        assertThat(nonWeeklyCounts()).isEqualTo(previousCounts);
    }

    @Test
    @DisplayName("기존 주차의 ID·제목·기간과 연결 워크북 및 다른 기수를 보존하고 누락된 목차만 등록한다")
    void preservesExistingContentAndOtherGisuOnRerun() throws Exception {
        // Given
        Long curriculumId = jdbcTemplate.queryForObject(
            "SELECT id FROM curriculum WHERE gisu_id = ? AND track = 'PLAN'", Long.class, gisuId);
        Long existingWeeklyId = insertWeekly(curriculumId, "수정한 기획 1주차");
        Long workbookId = jdbcTemplate.queryForObject("""
            INSERT INTO original_workbook
                (weekly_curriculum_id, title, content, type, original_workbook_status, created_at, updated_at)
            VALUES (?, '이미 작성한 워크북', '보존할 학습 내용', 'MAIN', 'DRAFT', now(), now())
            RETURNING id
            """, Long.class, existingWeeklyId);
        WeeklyRow existingWeek = readWeeks(gisuId).getFirst();
        var existingWorkbook = jdbcTemplate.queryForMap("SELECT * FROM original_workbook WHERE id = ?", workbookId);
        Long otherGisuId = jdbcTemplate.queryForObject("""
            INSERT INTO gisu (generation, is_active, learning_type, start_at, end_at, created_at, updated_at)
            VALUES (77, false, 'PART', '2025-03-01T00:00:00Z', '2025-08-31T00:00:00Z', now(), now())
            RETURNING id
            """, Long.class);
        Long otherCurriculumId = jdbcTemplate.queryForObject("""
            INSERT INTO curriculum (gisu_id, part, title, created_at, updated_at)
            VALUES (?, 'WEB', '기존 기수 웹', now(), now()) RETURNING id
            """, Long.class, otherGisuId);
        insertWeekly(otherCurriculumId, "기존 기수 주차");
        var otherWeeks = readWeeks(otherGisuId);

        // When
        executeMigration();

        // Then
        var weeks = readWeeks(gisuId);
        assertThat(weeks).hasSize(44).contains(existingWeek);
        assertThat(readWeeks(otherGisuId)).isEqualTo(otherWeeks);
        assertThat(jdbcTemplate.queryForMap("SELECT * FROM original_workbook WHERE id = ?", workbookId))
            .isEqualTo(existingWorkbook);

        // When
        executeMigration();

        // Then
        assertThat(readWeeks(gisuId)).isEqualTo(weeks);
        assertThat(readWeeks(otherGisuId)).isEqualTo(otherWeeks);
        assertThat(jdbcTemplate.queryForMap("SELECT * FROM original_workbook WHERE id = ?", workbookId))
            .isEqualTo(existingWorkbook);
    }

    @Test
    @DisplayName("기본 Track 부모 커리큘럼이 하나라도 없으면 다른 Track의 목차도 생성하지 않는다")
    void rejectsMissingParentBeforeCreatingAnyWeek() {
        // Given
        jdbcTemplate.update("DELETE FROM curriculum WHERE gisu_id = ? AND track = 'DESIGN'", gisuId);

        // When / Then
        assertThatThrownBy(this::executeMigration).isInstanceOf(DataAccessException.class)
            .hasMessageContaining("기본 Track 커리큘럼 4개가 필요합니다");
        assertThat(readWeeks(gisuId)).isEmpty();
    }

    private Long insertWeekly(Long curriculumId, String title) {
        return jdbcTemplate.queryForObject("""
            INSERT INTO weekly_curriculum
                (curriculum_id, week_no, is_extra, title, starts_at, ends_at, created_at, updated_at)
            VALUES (?, 1, false, ?, TIMESTAMP '2026-10-01 03:00:00',
                TIMESTAMP '2026-10-08 03:00:00', now(), now())
            RETURNING id
            """, Long.class, curriculumId, title);
    }

    private void executeMigration() throws Exception {
        String sql = new ClassPathResource(MIGRATION_PATH).getContentAsString(StandardCharsets.UTF_8);
        jdbcTemplate.execute((ConnectionCallback<Void>) connection -> {
            var savepoint = connection.setSavepoint();
            try (var statement = connection.createStatement()) {
                statement.execute(sql);
            } catch (SQLException exception) {
                connection.rollback(savepoint);
                throw exception;
            } finally {
                connection.releaseSavepoint(savepoint);
            }
            return null;
        });
    }

    private List<WeeklyRow> readWeeks(Long targetGisuId) {
        return jdbcTemplate.query("""
            SELECT weekly.id, weekly.curriculum_id, curriculum.track, weekly.week_no,
                weekly.is_extra, weekly.title, weekly.starts_at, weekly.ends_at
            FROM weekly_curriculum weekly
            JOIN curriculum ON curriculum.id = weekly.curriculum_id
            WHERE curriculum.gisu_id = ?
            ORDER BY curriculum.track, weekly.is_extra, weekly.week_no, weekly.id
            """, (row, rowNum) -> new WeeklyRow(row.getLong("id"), row.getLong("curriculum_id"),
            row.getString("track"), row.getLong("week_no"), row.getBoolean("is_extra"), row.getString("title"),
            row.getObject("starts_at", LocalDateTime.class), row.getObject("ends_at", LocalDateTime.class)), targetGisuId);
    }

    private Map<String, Long> nonWeeklyCounts() {
        return Map.of(
            "curriculum", jdbcTemplate.queryForObject("SELECT count(*) FROM curriculum", Long.class),
            "workbook", jdbcTemplate.queryForObject("SELECT count(*) FROM original_workbook", Long.class),
            "mission", jdbcTemplate.queryForObject("SELECT count(*) FROM original_workbook_mission", Long.class),
            "challengerRecord", jdbcTemplate.queryForObject("SELECT count(*) FROM challenger_record", Long.class));
    }

    private List<OutlineRow> readApprovedOutline() throws Exception {
        JsonNode outline = new ObjectMapper().readTree(Path.of("scripts/data/11th-curriculum-outline.json").toFile());
        List<OutlineRow> result = new ArrayList<>();
        for (JsonNode curriculum : outline.path("curricula")) {
            for (JsonNode week : curriculum.path("weeks")) {
                result.add(new OutlineRow(curriculum.path("track").asText(), week.path("weekNo").asLong(),
                    week.path("isExtra").asBoolean(), week.path("title").asText()));
            }
        }
        return result;
    }

    private record OutlineRow(String track, long weekNo, boolean extra, String title) {
    }

    private record WeeklyRow(
        long id, long curriculumId, String track, long weekNo, boolean extra, String title,
        LocalDateTime startsAt, LocalDateTime endsAt
    ) {
    }
}
