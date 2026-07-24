package com.umc.product.project.migration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;

import com.umc.product.support.PostgisTestImageResolver;

class ProjectMatchingRoundGisuMigrationTest {

    private static final String PREVIOUS_VERSION = "2026.07.03.00.00";
    private static final String NEW_VERSION = "2026.07.13.21.20";
    private static final AtomicInteger DATABASE_SEQUENCE = new AtomicInteger();

    private static PostgreSQLContainer<?> postgres;

    @BeforeAll
    static void startPostgres() {
        postgres = new PostgreSQLContainer<>(PostgisTestImageResolver.resolve());
        postgres.start();
    }

    @AfterAll
    static void stopPostgres() {
        postgres.stop();
    }

    @Test
    @DisplayName("직전 버전의 정상 legacy row를 새 migration이 gisuId로 backfill한다")
    void legacy_row를_gisuId로_backfill한다() throws SQLException {
        Database database = previousDatabase("valid");
        seedValidLegacyRound(database);

        assertThat(columnExists(database, "project_matching_round", "gisu_id")).isFalse();

        Flyway latest = flyway(database, null);
        latest.migrate();

        assertThat(java.util.Arrays.stream(latest.info().applied())
            .map(info -> info.getVersion().getVersion()))
            .contains(NEW_VERSION);
        assertThat(singleLong(database, "SELECT gisu_id FROM project_matching_round WHERE id = 92001"))
            .isEqualTo(91001L);
        assertThat(singleBoolean(database, """
            SELECT attnotnull
            FROM pg_attribute
            WHERE attrelid = 'project_matching_round'::regclass
              AND attname = 'gisu_id'
            """)).isTrue();
        assertThat(singleBoolean(database, """
            SELECT convalidated
            FROM pg_constraint
            WHERE conrelid = 'project_matching_round'::regclass
              AND conname = 'fk_project_matching_round_on_gisu'
              AND contype = 'f'
            """)).isTrue();
        assertThat(singleString(database, """
            SELECT string_agg(attribute.attname, ',' ORDER BY key.ordinality)
            FROM pg_index index_info
                     JOIN LATERAL unnest(index_info.indkey) WITH ORDINALITY key(attnum, ordinality) ON true
                     JOIN pg_attribute attribute
                          ON attribute.attrelid = index_info.indrelid AND attribute.attnum = key.attnum
            WHERE index_info.indexrelid = 'idx_project_matching_round_gisu_chapter_starts'::regclass
            """)).isEqualTo("gisu_id,chapter_id,starts_at");
        assertThat(singleString(database, """
            SELECT array_to_string(function_info.proconfig, ',')
            FROM pg_proc function_info
                     JOIN pg_namespace namespace_info
                          ON namespace_info.oid = function_info.pronamespace
            WHERE namespace_info.nspname = 'public'
              AND function_info.proname = 'fill_project_matching_round_gisu_for_legacy_writer'
            """)).isEqualTo("search_path=pg_catalog");
        assertThat(singleString(database, """
            SELECT array_to_string(function_info.proconfig, ',')
            FROM pg_proc function_info
                     JOIN pg_namespace namespace_info
                          ON namespace_info.oid = function_info.pronamespace
            WHERE namespace_info.nspname = 'public'
              AND function_info.proname = 'enforce_project_application_matching_scope'
            """)).isEqualTo("search_path=pg_catalog");
        assertThat(singleString(database, """
            SELECT array_to_string(function_info.proconfig, ',')
            FROM pg_proc function_info
                     JOIN pg_namespace namespace_info
                          ON namespace_info.oid = function_info.pronamespace
            WHERE namespace_info.nspname = 'public'
              AND function_info.proname = 'enforce_project_matching_round_period'
            """)).isEqualTo("search_path=pg_catalog");
        assertThat(singleString(database, """
            SELECT array_to_string(function_info.proconfig, ',')
            FROM pg_proc function_info
                     JOIN pg_namespace namespace_info
                          ON namespace_info.oid = function_info.pronamespace
            WHERE namespace_info.nspname = 'public'
              AND function_info.proname = 'enforce_project_parent_application_scope'
            """)).isEqualTo("search_path=pg_catalog");
        assertThat(singleString(database, """
            SELECT array_to_string(function_info.proconfig, ',')
            FROM pg_proc function_info
                     JOIN pg_namespace namespace_info
                          ON namespace_info.oid = function_info.pronamespace
            WHERE namespace_info.nspname = 'public'
              AND function_info.proname = 'enforce_project_application_form_parent_scope'
            """)).isEqualTo("search_path=pg_catalog");
        assertThat(singleBoolean(database, """
            SELECT count(*) = 5
            FROM pg_trigger
            WHERE tgname IN (
                'trg_project_matching_round_legacy_gisu',
                'trg_project_matching_round_period',
                'trg_project_application_matching_scope',
                'trg_project_parent_application_scope',
                'trg_project_application_form_parent_scope'
            )
              AND NOT tgisinternal
            """)).isTrue();
        assertThat(successfulMigrationCount(database)).isEqualTo(1L);

        insertLegacyAndExplicitWriterRows(database);
        assertThat(singleLong(database, "SELECT gisu_id FROM project_matching_round WHERE id = 92002"))
            .isEqualTo(91001L);
        assertThat(singleLong(database, "SELECT gisu_id FROM project_matching_round WHERE id = 92003"))
            .isEqualTo(91001L);
    }

    @Test
    @DisplayName("호출 세션의 선행 schema에 같은 이름의 chapter가 있어도 public chapter를 사용한다")
    void search_path_hijack을_차단한다() throws SQLException {
        Database database = migratedDatabase("search_path");
        insertGisu(database, 91002L);
        execute(database, "CREATE SCHEMA malicious");
        execute(database, "CREATE TABLE malicious.chapter (id BIGINT PRIMARY KEY, gisu_id BIGINT)");
        execute(database, "INSERT INTO malicious.chapter (id, gisu_id) VALUES (91001, 91002)");

        executeWithSearchPath(database, "malicious, public, pg_catalog",
            migratedRoundInsertSql(92002L, null, 91001L, "SECOND"));

        assertThat(singleLong(database, "SELECT gisu_id FROM public.project_matching_round WHERE id = 92002"))
            .isEqualTo(91001L);
    }

    @Test
    @DisplayName("명시한 gisuId가 지부의 기수와 다르면 insert를 stable error로 거부한다")
    void explicit_gisu_mismatch_insert를_거부한다() throws SQLException {
        Database database = migratedDatabase("mismatch_insert");
        insertGisu(database, 91002L);

        assertThatThrownBy(() -> execute(database,
            migratedRoundInsertSql(92002L, 91002L, 91001L, "SECOND")))
            .hasStackTraceContaining("PMR_GISU_TRIGGER_GISU_CHAPTER_MISMATCH");
    }

    @Test
    @DisplayName("기존 round의 gisuId를 지부와 다른 값으로 수정하면 stable error로 거부한다")
    void explicit_gisu_mismatch_update를_거부한다() throws SQLException {
        Database database = migratedDatabase("mismatch_update");
        insertGisu(database, 91002L);

        assertThatThrownBy(() -> execute(database, """
            UPDATE public.project_matching_round
            SET gisu_id = 91002
            WHERE id = 92001
            """))
            .hasStackTraceContaining("PMR_GISU_TRIGGER_GISU_CHAPTER_MISMATCH");
    }

    @Test
    @DisplayName("chapterId 변경 후 기존 gisuId가 stale하면 stable error로 거부한다")
    void stale_gisu_chapter_update를_거부한다() throws SQLException {
        Database database = migratedDatabase("stale_chapter_update");
        insertGisu(database, 91002L);
        insertChapter(database, 91002L, 91002L);

        assertThatThrownBy(() -> execute(database, """
            UPDATE public.project_matching_round
            SET chapter_id = 91002
            WHERE id = 92001
            """))
            .hasStackTraceContaining("PMR_GISU_TRIGGER_GISU_CHAPTER_MISMATCH");
    }

    @Test
    @DisplayName("존재하지 않는 chapter의 old-writer insert를 stable error로 거부한다")
    void missing_chapter_insert를_거부한다() throws SQLException {
        Database database = migratedDatabase("missing_chapter");

        assertThatThrownBy(() -> execute(database,
            migratedRoundInsertSql(92002L, null, 99999L, "SECOND")))
            .hasStackTraceContaining("PMR_GISU_TRIGGER_CHAPTER_NOT_FOUND");
    }

    @Test
    @DisplayName("gisuId가 없는 chapter의 old-writer insert를 stable error로 거부한다")
    void null_chapter_gisu_insert를_거부한다() throws SQLException {
        Database database = migratedDatabase("null_chapter_gisu_insert");
        insertChapter(database, 91002L, null);

        assertThatThrownBy(() -> execute(database,
            migratedRoundInsertSql(92002L, null, 91002L, "SECOND")))
            .hasStackTraceContaining("PMR_GISU_TRIGGER_NULL_CHAPTER_GISU");
    }

    @Test
    @DisplayName("round 기간 불변식을 위반한 old-writer insert를 모든 경계에서 stable error로 거부한다")
    void invalid_round_period_insert를_거부한다() throws SQLException {
        Database database = migratedDatabase("invalid_period_insert");
        String[][] invalidPeriods = {
            {"2025-12-31T23:59:59Z", "2026-05-10T00:00:00Z", "2026-05-12T00:00:00Z"},
            {"2026-05-10T00:00:00Z", "2026-05-10T00:00:00Z", "2026-05-12T00:00:00Z"},
            {"2026-05-01T00:00:00Z", "2026-05-12T00:00:00Z", "2026-05-12T00:00:00Z"},
            {"2026-05-01T00:00:00Z", "2026-05-10T00:00:00Z", "2027-01-01T00:00:00Z"}
        };

        for (String[] period : invalidPeriods) {
            assertThatThrownBy(() -> execute(database,
                migratedRoundInsertSql(92002L, null, 91001L, "SECOND", period[0], period[1], period[2])))
                .hasStackTraceContaining("PMR_GISU_TRIGGER_ROUND_PERIOD");
        }
    }

    @Test
    @DisplayName("round 기간 불변식을 위반한 raw update를 모든 경계에서 stable error로 거부한다")
    void invalid_round_period_update를_거부한다() throws SQLException {
        Database database = migratedDatabase("invalid_period_update");
        String[][] invalidPeriods = {
            {"2025-12-31T23:59:59Z", "2026-05-10T00:00:00Z", "2026-05-12T00:00:00Z"},
            {"2026-05-10T00:00:00Z", "2026-05-10T00:00:00Z", "2026-05-12T00:00:00Z"},
            {"2026-05-01T00:00:00Z", "2026-05-12T00:00:00Z", "2026-05-12T00:00:00Z"},
            {"2026-05-01T00:00:00Z", "2026-05-10T00:00:00Z", "2027-01-01T00:00:00Z"}
        };

        for (String[] period : invalidPeriods) {
            String updateSql = """
                UPDATE public.project_matching_round
                SET starts_at = '%s', ends_at = '%s', decision_deadline = '%s'
                WHERE id = 92001
                """.formatted(period[0], period[1], period[2]);
            assertThatThrownBy(() -> execute(database, updateSql))
                .hasStackTraceContaining("PMR_GISU_TRIGGER_ROUND_PERIOD");
        }
    }

    @Test
    @DisplayName("application과 matching round의 기수·지부 scope를 raw insert와 update에서 강제한다")
    void application_matching_scope를_쓰기마다_강제한다() throws SQLException {
        Database database = migratedDatabase("application_scope_trigger");
        insertApplicationWithProjectScope(database, 91001L, 91001L);
        assertThat(singleLong(database, "SELECT count(*) FROM project_application WHERE id = 93001"))
            .isEqualTo(1L);

        insertGisu(database, 91002L);
        insertChapter(database, 91002L, 91002L);
        execute(database, migratedRoundInsertSql(92002L, 91002L, 91002L, "SECOND"));
        insertProjectAndForm(database, 93002L, 91002L, 91002L);

        assertThatThrownBy(() -> insertApplication(database, 93002L, 93002L, 92001L))
            .hasStackTraceContaining("PMR_GISU_TRIGGER_APPLICATION_SCOPE");
        assertThatThrownBy(() -> execute(database, """
            UPDATE public.project_application
            SET project_application_form_id = 93002
            WHERE id = 93001
            """))
            .hasStackTraceContaining("PMR_GISU_TRIGGER_APPLICATION_SCOPE");
        assertThatThrownBy(() -> execute(database, """
            UPDATE public.project_application
            SET applied_matching_round_id = 92002
            WHERE id = 93001
            """))
            .hasStackTraceContaining("PMR_GISU_TRIGGER_APPLICATION_SCOPE");
    }

    @Test
    @DisplayName("지원서가 연결된 project의 기수·지부 parent update는 stable error로 거부하고 동일 scope는 허용한다")
    void project_parent_scope_update를_강제한다() throws SQLException {
        Database database = migratedDatabase("project_parent_scope_trigger");
        insertApplicationWithProjectScope(database, 91001L, 91001L);
        insertChapter(database, 91003L, 91001L);

        assertThatThrownBy(() -> execute(database, """
            UPDATE public.project
            SET chapter_id = 91003
            WHERE id = 93001
            """))
            .hasStackTraceContaining("PMR_GISU_TRIGGER_PROJECT_SCOPE");

        execute(database, """
            UPDATE public.project
            SET gisu_id = 91001, chapter_id = 91001
            WHERE id = 93001
            """);
        assertThat(singleLong(database, applicationScopeMismatchCountSql())).isZero();
    }

    @Test
    @DisplayName("지원서가 연결된 form의 project parent update는 stable error로 거부하고 동일 project는 허용한다")
    void application_form_parent_scope_update를_강제한다() throws SQLException {
        Database database = migratedDatabase("form_parent_scope_trigger");
        insertApplicationWithProjectScope(database, 91001L, 91001L);
        insertChapter(database, 91003L, 91001L);
        insertProjectAndForm(database, 93002L, 91001L, 91003L);

        assertThatThrownBy(() -> execute(database, """
            UPDATE public.project_application_form
            SET project_id = 93002
            WHERE id = 93001
            """))
            .hasStackTraceContaining("PMR_GISU_TRIGGER_APPLICATION_FORM_SCOPE");

        execute(database, """
            UPDATE public.project_application_form
            SET project_id = 93001
            WHERE id = 93001
            """);
        assertThat(singleLong(database, applicationScopeMismatchCountSql())).isZero();
    }

    @Test
    @DisplayName("project scope update와 동시 application insert는 parent commit 뒤 재검증해 TOCTOU를 차단한다")
    void concurrent_project_update와_application_insert를_직렬화한다() throws Exception {
        Database database = migratedDatabase("project_parent_scope_concurrency");
        insertProjectAndForm(database, 93001L, 91001L, 91001L);
        insertChapter(database, 91003L, 91001L);
        CountDownLatch insertStarted = new CountDownLatch(1);

        try (Connection projectUpdate = connection(database)) {
            projectUpdate.setAutoCommit(false);
            try (var statement = projectUpdate.createStatement()) {
                statement.executeUpdate("""
                    UPDATE public.project
                    SET chapter_id = 91003
                    WHERE id = 93001
                    """);
            }

            CompletableFuture<Throwable> insertion = CompletableFuture.supplyAsync(() -> {
                try (Connection childInsert = connection(database);
                     var statement = childInsert.createStatement()) {
                    statement.execute("SET lock_timeout = '5s'");
                    insertStarted.countDown();
                    statement.executeUpdate(applicationInsertSql(93001L, 93001L, 92001L));
                    return null;
                } catch (Throwable failure) {
                    return failure;
                }
            });

            try {
                assertThat(insertStarted.await(5, TimeUnit.SECONDS)).isTrue();
                assertThatThrownBy(() -> insertion.get(300, TimeUnit.MILLISECONDS))
                    .isInstanceOf(TimeoutException.class);

                projectUpdate.commit();
                Throwable insertionFailure = insertion.get(5, TimeUnit.SECONDS);
                assertThatThrownBy(() -> {
                    throw insertionFailure;
                }).hasStackTraceContaining("PMR_GISU_TRIGGER_APPLICATION_SCOPE");
            } finally {
                if (!projectUpdate.getAutoCommit()) {
                    projectUpdate.rollback();
                }
            }
        }
        assertThat(singleLong(database, applicationScopeMismatchCountSql())).isZero();
    }

    @Test
    @DisplayName("존재하지 않는 지부를 참조한 legacy round는 migration을 원자적으로 중단한다")
    void orphan_chapter를_감사한다() throws SQLException {
        Database database = previousDatabase("orphan");
        insertRound(database, 92001L, 99999L, "FIRST", "2026-05-12T00:00:00Z");

        assertMigrationFailsWithoutPartialState(database, "PMR_GISU_AUDIT_ORPHAN_CHAPTER");
    }

    @Test
    @DisplayName("기수가 없는 지부를 참조한 legacy round는 migration을 원자적으로 중단한다")
    void null_chapter_gisu를_감사한다() throws SQLException {
        Database database = previousDatabase("null_gisu");
        insertChapter(database, 91001L, null);
        insertRound(database, 92001L, 91001L, "FIRST", "2026-05-12T00:00:00Z");

        assertMigrationFailsWithoutPartialState(database, "PMR_GISU_AUDIT_NULL_CHAPTER_GISU");
    }

    @Test
    @DisplayName("기수 종료 정각을 결정 마감으로 가진 legacy round는 migration을 원자적으로 중단한다")
    void round_period를_감사한다() throws SQLException {
        Database database = previousDatabase("period");
        insertGisu(database, 91001L);
        insertChapter(database, 91001L, 91001L);
        insertRound(database, 92001L, 91001L, "FIRST", "2027-01-01T00:00:00Z");

        assertMigrationFailsWithoutPartialState(database, "PMR_GISU_AUDIT_ROUND_PERIOD");
    }

    @Test
    @DisplayName("프로젝트와 round의 기수 또는 지부가 다른 지원서는 migration을 원자적으로 중단한다")
    void application_scope를_감사한다() throws SQLException {
        Database database = previousDatabase("application_scope");
        seedValidLegacyRound(database);
        insertGisu(database, 91002L);
        insertChapter(database, 91002L, 91002L);
        insertApplicationWithProjectScope(database, 91002L, 91002L);

        assertMigrationFailsWithoutPartialState(database, "PMR_GISU_AUDIT_APPLICATION_SCOPE");
    }

    private static Database previousDatabase(String label) throws SQLException {
        Database database = createDatabase(label);
        flyway(database, PREVIOUS_VERSION).migrate();
        return database;
    }

    private static Database migratedDatabase(String label) throws SQLException {
        Database database = previousDatabase(label);
        seedValidLegacyRound(database);
        flyway(database, null).migrate();
        return database;
    }

    private static Database createDatabase(String label) throws SQLException {
        String databaseName = "task5_" + label + "_" + DATABASE_SEQUENCE.incrementAndGet();
        try (Connection connection = connection(defaultDatabase()); var statement = connection.createStatement()) {
            statement.execute("CREATE DATABASE " + databaseName);
        }
        return new Database(jdbcUrl(databaseName));
    }

    private static Flyway flyway(Database database, String targetVersion) {
        var configuration = Flyway.configure()
            .dataSource(database.jdbcUrl(), postgres.getUsername(), postgres.getPassword())
            .locations("filesystem:src/main/resources/db/migration")
            .baselineVersion(MigrationVersion.fromVersion("2026.02.25.01.30"));
        if (targetVersion != null) {
            configuration.target(MigrationVersion.fromVersion(targetVersion));
        }
        return configuration.load();
    }

    private static void seedValidLegacyRound(Database database) throws SQLException {
        insertGisu(database, 91001L);
        insertChapter(database, 91001L, 91001L);
        insertRound(database, 92001L, 91001L, "FIRST", "2026-05-12T00:00:00Z");
    }

    private static void insertGisu(Database database, Long gisuId) throws SQLException {
        execute(database, """
            INSERT INTO gisu (id, generation, is_active, start_at, end_at, created_at, updated_at)
            VALUES (%d, %d, false, '2026-01-01T00:00:00Z', '2027-01-01T00:00:00Z', now(), now())
            """.formatted(gisuId, gisuId));
    }

    private static void insertChapter(Database database, Long chapterId, Long gisuId) throws SQLException {
        String gisuValue = gisuId == null ? "NULL" : gisuId.toString();
        execute(database, """
            INSERT INTO chapter (id, gisu_id, name, created_at, updated_at)
            VALUES (%d, %s, 'migration-test-%d', now(), now())
            """.formatted(chapterId, gisuValue, chapterId));
    }

    private static void insertRound(
        Database database, Long roundId, Long chapterId, String phase, String deadline
    ) throws SQLException {
        execute(database, """
            INSERT INTO project_matching_round (
                id, name, type, phase, chapter_id, starts_at, ends_at, decision_deadline,
                created_at, updated_at
            ) VALUES (
                %d, 'legacy-round-%d', 'PLAN_DESIGN', '%s', %d,
                '2026-05-01T00:00:00Z', '2026-05-10T00:00:00Z', '%s',
                now(), now()
            )
            """.formatted(roundId, roundId, phase, chapterId, deadline));
    }

    private static void insertApplicationWithProjectScope(
        Database database, Long projectGisuId, Long projectChapterId
    ) throws SQLException {
        insertProjectAndForm(database, 93001L, projectGisuId, projectChapterId);
        insertApplication(database, 93001L, 93001L, 92001L);
    }

    private static void insertProjectAndForm(
        Database database, Long projectId, Long projectGisuId, Long projectChapterId
    ) throws SQLException {
        execute(database, """
            INSERT INTO project (
                id, gisu_id, chapter_id, status, product_owner_member_id, product_owner_school_id,
                created_by_member_id, created_at, updated_at
            ) VALUES (%d, %d, %d, 'IN_PROGRESS', 94001, 95001, 94001, now(), now())
            """.formatted(projectId, projectGisuId, projectChapterId));
        execute(database, """
            INSERT INTO project_application_form (id, project_id, form_id, created_at, updated_at)
            VALUES (%d, %d, 96001, now(), now())
            """.formatted(projectId, projectId));
    }

    private static void insertApplication(
        Database database, Long applicationId, Long formId, Long roundId
    ) throws SQLException {
        execute(database, applicationInsertSql(applicationId, formId, roundId));
    }

    private static String applicationInsertSql(Long applicationId, Long formId, Long roundId) {
        return """
            INSERT INTO project_application (
                id, project_application_form_id, form_response_id, applicant_member_id,
                applied_matching_round_id, status, created_at, updated_at
            ) VALUES (%d, %d, 97001, 98001, %d, 'DRAFT', now(), now())
            """.formatted(applicationId, formId, roundId);
    }

    private static void insertLegacyAndExplicitWriterRows(Database database) throws SQLException {
        execute(database, """
            INSERT INTO project_matching_round (
                id, name, type, phase, chapter_id, starts_at, ends_at, decision_deadline,
                created_at, updated_at
            ) VALUES (
                92002, 'old-writer', 'PLAN_DESIGN', 'SECOND', 91001,
                '2026-06-01T00:00:00Z', '2026-06-10T00:00:00Z', '2026-06-12T00:00:00Z',
                now(), now()
            )
            """);
        execute(database, """
            INSERT INTO project_matching_round (
                id, name, type, phase, gisu_id, chapter_id, starts_at, ends_at, decision_deadline,
                created_at, updated_at
            ) VALUES (
                92003, 'new-writer', 'PLAN_DESIGN', 'THIRD', 91001, 91001,
                '2026-07-01T00:00:00Z', '2026-07-10T00:00:00Z', '2026-07-12T00:00:00Z',
                now(), now()
            )
            """);
    }

    private static String applicationScopeMismatchCountSql() {
        return """
            SELECT count(*)
            FROM public.project_application application
                     JOIN public.project_application_form form
                          ON form.id = application.project_application_form_id
                     JOIN public.project ON project.id = form.project_id
                     JOIN public.project_matching_round round
                          ON round.id = application.applied_matching_round_id
            WHERE project.gisu_id IS DISTINCT FROM round.gisu_id
               OR project.chapter_id IS DISTINCT FROM round.chapter_id
            """;
    }

    private static String migratedRoundInsertSql(
        Long roundId, Long gisuId, Long chapterId, String phase
    ) {
        return migratedRoundInsertSql(
            roundId,
            gisuId,
            chapterId,
            phase,
            "2026-06-01T00:00:00Z",
            "2026-06-10T00:00:00Z",
            "2026-06-12T00:00:00Z");
    }

    private static String migratedRoundInsertSql(
        Long roundId,
        Long gisuId,
        Long chapterId,
        String phase,
        String startsAt,
        String endsAt,
        String decisionDeadline
    ) {
        String gisuValue = gisuId == null ? "NULL" : gisuId.toString();
        return """
            INSERT INTO public.project_matching_round (
                id, name, type, phase, gisu_id, chapter_id, starts_at, ends_at, decision_deadline,
                created_at, updated_at
            ) VALUES (
                %d, 'trigger-round-%d', 'PLAN_DESIGN', '%s', %s, %d,
                '%s', '%s', '%s',
                now(), now()
            )
            """.formatted(
                roundId, roundId, phase, gisuValue, chapterId, startsAt, endsAt, decisionDeadline);
    }

    private static void assertMigrationFailsWithoutPartialState(Database database, String auditCode)
        throws SQLException {
        assertThatThrownBy(() -> flyway(database, null).migrate())
            .hasStackTraceContaining(auditCode);
        assertThat(columnExists(database, "project_matching_round", "gisu_id")).isFalse();
        assertThat(successfulMigrationCount(database)).isZero();
    }

    private static long successfulMigrationCount(Database database) throws SQLException {
        return singleLong(database, """
            SELECT count(*)
            FROM flyway_schema_history
            WHERE version = '2026.07.13.21.20'
              AND success = true
            """);
    }

    private static boolean columnExists(Database database, String tableName, String columnName)
        throws SQLException {
        try (
            Connection connection = connection(database);
            var statement = connection.prepareStatement("""
                SELECT EXISTS (
                    SELECT 1
                    FROM information_schema.columns
                    WHERE table_schema = 'public'
                      AND table_name = ?
                      AND column_name = ?
                )
                """)
        ) {
            statement.setString(1, tableName);
            statement.setString(2, columnName);
            try (var resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getBoolean(1);
            }
        }
    }

    private static long singleLong(Database database, String sql) throws SQLException {
        try (Connection connection = connection(database); var statement = connection.createStatement()) {
            try (var resultSet = statement.executeQuery(sql)) {
                assertThat(resultSet.next()).isTrue();
                return resultSet.getLong(1);
            }
        }
    }

    private static boolean singleBoolean(Database database, String sql) throws SQLException {
        try (Connection connection = connection(database); var statement = connection.createStatement()) {
            try (var resultSet = statement.executeQuery(sql)) {
                assertThat(resultSet.next()).isTrue();
                return resultSet.getBoolean(1);
            }
        }
    }

    private static String singleString(Database database, String sql) throws SQLException {
        try (Connection connection = connection(database); var statement = connection.createStatement()) {
            try (var resultSet = statement.executeQuery(sql)) {
                assertThat(resultSet.next()).isTrue();
                return resultSet.getString(1);
            }
        }
    }

    private static void execute(Database database, String sql) throws SQLException {
        try (Connection connection = connection(database); var statement = connection.createStatement()) {
            statement.executeUpdate(sql);
        }
    }

    private static void executeWithSearchPath(Database database, String searchPath, String sql)
        throws SQLException {
        try (Connection connection = connection(database); var statement = connection.createStatement()) {
            statement.execute("SET search_path TO " + searchPath);
            statement.executeUpdate(sql);
        }
    }

    private static Database defaultDatabase() {
        return new Database(postgres.getJdbcUrl());
    }

    private static String jdbcUrl(String databaseName) {
        return "jdbc:postgresql://%s:%d/%s".formatted(
            postgres.getHost(), postgres.getMappedPort(PostgreSQLContainer.POSTGRESQL_PORT), databaseName);
    }

    private static Connection connection(Database database) throws SQLException {
        return DriverManager.getConnection(
            database.jdbcUrl(), postgres.getUsername(), postgres.getPassword());
    }

    private record Database(String jdbcUrl) {
    }
}
