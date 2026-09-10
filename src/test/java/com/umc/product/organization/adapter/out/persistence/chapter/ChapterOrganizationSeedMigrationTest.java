package com.umc.product.organization.adapter.out.persistence.chapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

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

import com.umc.product.support.PersistenceAdapterTest;

@PersistenceAdapterTest
@DisplayName("11기 학교·지부 배정 Flyway 마이그레이션")
class ChapterOrganizationSeedMigrationTest {

    private static final String MIGRATION_PATH =
        "db/migration/V2026.09.10.03.00__seed_11th_schools_chapters.sql";
    private static final Map<String, List<String>> EXPECTED_SCHOOLS = Map.of(
        "캥거루", List.of("가천대학교", "서울여자대학교", "한국항공대학교", "한성대학교"),
        "쿼카", List.of("동양미래대학교", "숭실대학교", "이화여자대학교", "인하대학교", "한양대학교 ERICA"),
        "수달", List.of("성신여자대학교", "숙명여자대학교", "중앙대학교", "한국외국어대학교"),
        "아홀로틀", List.of("가톨릭대학교", "덕성여자대학교", "세종대학교", "안양대학교", "홍익대학교 서울캠퍼스"),
        "티라노", List.of("단국대학교", "동국대학교", "동덕여자대학교", "서경대학교", "홍익대학교 세종캠퍼스"));

    @Autowired private JdbcTemplate jdbcTemplate;

    private Long gisuId;

    @BeforeEach
    void prepareEleventhGisuWithGeneratedId() {
        // 기본 데이터는 다른 기수로 보존하고, 고정 ID를 사용할 수 없는 새 11기를 준비한다.
        jdbcTemplate.update("UPDATE gisu SET generation = 111 WHERE generation = 11");
        gisuId = insertGisu(11);
    }

    @Test
    @DisplayName("세종대와 인천가톨릭대를 추가하고 11기 23개 지부 배정과 중앙 운영진 학교의 미배정을 확인한다")
    void createsMissingSchoolAndAllEleventhAssignments() throws Exception {
        // Given
        prepareMissingSejongSchool();
        jdbcTemplate.update("UPDATE school SET name = '기존 인천가톨릭대 명칭' WHERE name = '인천가톨릭대학교'");
        for (Assignment assignment : expectedAssignments()) {
            if (!assignment.schoolName().equals("세종대학교")) {
                ensureSchool(assignment.schoolName());
            }
        }
        OrganizationSnapshot previous = snapshot();

        // When
        executeMigration();

        // Then
        assertThat(readAssignments(gisuId)).containsExactlyInAnyOrderElementsOf(expectedAssignments());
        OrganizationSnapshot migrated = snapshot();
        assertThat(migrated.schools()).containsAll(previous.schools());
        assertThat(migrated.chapters()).hasSize(previous.chapters().size() + 5).containsAll(previous.chapters());
        assertThat(migrated.links()).hasSize(previous.links().size() + 23).containsAll(previous.links());
        assertThat(migrated.gisus()).isEqualTo(previous.gisus());
        assertThat(jdbcTemplate.queryForList("SELECT id FROM school WHERE name = '세종대학교'", Long.class)).hasSize(1);
        var headquartersSchoolIds = jdbcTemplate.queryForList(
            "SELECT id FROM school WHERE name = '인천가톨릭대학교'", Long.class);
        assertThat(headquartersSchoolIds).hasSize(1);
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM chapter_school WHERE school_id = ?",
            Long.class, headquartersSchoolIds.getFirst())).isZero();
    }

    @Test
    @DisplayName("기존 다섯 지부와 22개 배정 및 학교 정보를 보존하고 ERICA 배정만 추가하며 재실행해도 같다")
    void preservesExistingOrganizationAndOtherGisuOnRerun() throws Exception {
        // Given
        for (Map.Entry<String, List<String>> entry : EXPECTED_SCHOOLS.entrySet()) {
            Long chapterId = insertChapter(gisuId, entry.getKey());
            for (String schoolName : entry.getValue()) {
                Long schoolId = ensureSchool(schoolName);
                if (!schoolName.equals("한양대학교 ERICA")) {
                    insertLink(chapterId, schoolId);
                }
            }
        }
        Long existingSchoolId = ensureSchool("가천대학교");
        jdbcTemplate.update("""
            UPDATE school SET short_name = '학교 약칭', logo_image_id = '기존 로고', remark = '직접 등록한 메모'
            WHERE id = ?
            """, existingSchoolId);
        ensureSchool("남서울대학교");
        Long otherGisuId = insertGisu(77);
        insertLink(insertChapter(otherGisuId, "기존 기수 지부"), existingSchoolId);
        var otherAssignments = readAssignments(otherGisuId);
        OrganizationSnapshot previous = snapshot();

        // When
        executeMigration();

        // Then
        assertThat(readAssignments(gisuId)).containsExactlyInAnyOrderElementsOf(expectedAssignments());
        OrganizationSnapshot migrated = snapshot();
        assertThat(migrated.schools()).containsAll(previous.schools());
        assertThat(migrated.chapters()).isEqualTo(previous.chapters());
        assertThat(migrated.links()).hasSize(previous.links().size() + 1).containsAll(previous.links());
        assertThat(migrated.gisus()).isEqualTo(previous.gisus());
        assertThat(readAssignments(otherGisuId)).isEqualTo(otherAssignments);

        // When
        executeMigration();

        // Then
        assertThat(snapshot()).isEqualTo(migrated);
    }

    @ParameterizedTest(name = "{0} 충돌은 앞서 등록한 학교와 지부까지 롤백한다")
    @ValueSource(strings = {"학교명 중복", "다른 지부 배정", "동일 배정 중복"})
    @DisplayName("뒤쪽 학교에서 중복이나 배정 충돌이 발생하면 모든 조직 데이터를 변경하지 않는다")
    void rejectsLateConflictWithoutPartialCreation(String conflict) {
        // Given
        prepareMissingSejongSchool();
        Long schoolId = ensureSchool("홍익대학교 세종캠퍼스");
        String expectedMessage = switch (conflict) {
            case "학교명 중복" -> {
                insertSchool("홍익대학교 세종캠퍼스");
                yield "학교명이 중복되어 11기 지부를 배정할 수 없습니다";
            }
            case "다른 지부 배정" -> {
                insertLink(insertChapter(gisuId, "캥거루"), schoolId);
                yield "11기 학교가 다른 지부에 배정되어 있습니다";
            }
            case "동일 배정 중복" -> {
                Long chapterId = insertChapter(gisuId, "티라노");
                insertLink(chapterId, schoolId);
                insertLink(chapterId, schoolId);
                yield "11기 학교의 지부 배정이 중복되어 있습니다";
            }
            default -> throw new IllegalArgumentException(conflict);
        };
        OrganizationSnapshot previous = snapshot();

        // When / Then
        assertThatThrownBy(this::executeMigration).isInstanceOf(DataAccessException.class)
            .hasMessageContaining(expectedMessage);
        assertThat(snapshot()).isEqualTo(previous);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    @DisplayName("11기 기수가 없거나 중복이면 학교와 지부를 생성하지 않는다")
    void rejectsMissingOrDuplicateEleventhGisu(boolean duplicate) {
        // Given
        if (duplicate) {
            insertGisu(11);
        } else {
            jdbcTemplate.update("UPDATE gisu SET generation = 112 WHERE id = ?", gisuId);
        }
        OrganizationSnapshot previous = snapshot();

        // When / Then
        assertThatThrownBy(this::executeMigration).isInstanceOf(DataAccessException.class);
        assertThat(snapshot()).isEqualTo(previous);
    }

    private void prepareMissingSejongSchool() {
        jdbcTemplate.update("UPDATE school SET name = '기존 세종대 명칭' WHERE name = '세종대학교'");
    }

    private Long insertGisu(long generation) {
        return jdbcTemplate.queryForObject("""
            INSERT INTO gisu (generation, is_active, learning_type, start_at, end_at, created_at, updated_at)
            VALUES (?, false, 'TRACK', '2026-09-01T00:00:00+09:00',
                '2027-02-27T23:59:59.999999+09:00', now(), now()) RETURNING id
            """, Long.class, generation);
    }

    private Long ensureSchool(String name) {
        List<Long> schoolIds = jdbcTemplate.queryForList("SELECT id FROM school WHERE name = ?", Long.class, name);
        if (schoolIds.isEmpty()) {
            return insertSchool(name);
        }
        assertThat(schoolIds).hasSize(1);
        return schoolIds.getFirst();
    }

    private Long insertSchool(String name) {
        return jdbcTemplate.queryForObject("""
            INSERT INTO school (name, created_at, updated_at) VALUES (?, now(), now()) RETURNING id
            """, Long.class, name);
    }

    private Long insertChapter(Long targetGisuId, String name) {
        return jdbcTemplate.queryForObject("""
            INSERT INTO chapter (gisu_id, name, created_at, updated_at) VALUES (?, ?, now(), now()) RETURNING id
            """, Long.class, targetGisuId, name);
    }

    private void insertLink(Long chapterId, Long schoolId) {
        jdbcTemplate.update("""
            INSERT INTO chapter_school (chapter_id, school_id, created_at, updated_at) VALUES (?, ?, now(), now())
            """, chapterId, schoolId);
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

    private List<Assignment> readAssignments(Long targetGisuId) {
        return jdbcTemplate.query("""
            SELECT chapter.name AS chapter_name, school.name AS school_name
            FROM chapter_school
            JOIN chapter ON chapter.id = chapter_school.chapter_id
            JOIN school ON school.id = chapter_school.school_id
            WHERE chapter.gisu_id = ?
            ORDER BY chapter.name, school.name
            """, (row, rowNum) -> new Assignment(row.getString("chapter_name"), row.getString("school_name")),
            targetGisuId);
    }

    private List<Assignment> expectedAssignments() {
        return EXPECTED_SCHOOLS.entrySet().stream()
            .flatMap(entry -> entry.getValue().stream().map(school -> new Assignment(entry.getKey(), school)))
            .toList();
    }

    private OrganizationSnapshot snapshot() {
        return new OrganizationSnapshot(
            jdbcTemplate.queryForList("SELECT * FROM gisu ORDER BY id"),
            jdbcTemplate.queryForList("SELECT * FROM school ORDER BY id"),
            jdbcTemplate.queryForList("SELECT * FROM chapter ORDER BY id"),
            jdbcTemplate.queryForList("SELECT * FROM chapter_school ORDER BY id"));
    }

    private record Assignment(String chapterName, String schoolName) {
    }

    private record OrganizationSnapshot(
        List<Map<String, Object>> gisus, List<Map<String, Object>> schools,
        List<Map<String, Object>> chapters, List<Map<String, Object>> links
    ) {
    }
}
