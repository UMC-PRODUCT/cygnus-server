package com.umc.product.registry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.sql.DriverManager;
import java.util.List;
import java.util.UUID;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jdbc.JdbcConnectionDetails;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import com.umc.product.support.IntegrationTestSupport;

@DisplayName("Registry Flyway data migration")
@ResourceLock("registry-flyway-data-migration")
class RegistryFlywayDataMigrationIntegrationTest extends IntegrationTestSupport {

    private static final String PRE_REGISTRY_VERSION = "2026.07.14.01.00";
    private static final String STORAGE_VERSION = "2026.07.16.00.00";
    private static final String FORM_VERSION = "2026.07.16.00.10";
    private static final String CHAT_VERSION = "2026.07.16.00.20";
    private static final String LIFECYCLE_VERSION = "2026.07.16.00.40";

    @Autowired
    private JdbcConnectionDetails connectionDetails;

    private JdbcTemplate jdbcTemplate;
    private DriverManagerDataSource migrationDataSource;

    @Test
    void legacy_usage와_Form_ownership은_Flyway로_이관하고_Chat은_빈_상태로_초기화한다()
        throws Exception {
        try (TestDatabase ignored = createTestDatabase()) {
            // given
            insertStorageFixtures();
            insertFormFixtures();

            // when: 운영 Flyway와 같은 순서로 registry migration을 적용한다.
            migrateTo(STORAGE_VERSION);

            // then: Storage의 현재 9개 legacy source를 exact usage snapshot으로 이관한다.
            assertThat(usageRows()).containsExactly(
                "certificate/801/file=file-certificate",
                "chat.message/601/attachments=file-chat",
                "chat.message/601/attachments=file-shared",
                "form.answer/501/attachments=file-answer",
                "form.answer/501/attachments=file-shared",
                "member/101/profile-image=file-member",
                "notice/301/images=file-notice-a",
                "notice/301/images=file-notice-b",
                "organization.school/201/logo=file-school",
                "organization.umc-product-member/701/profile-image=file-umc-member",
                "project/401/logo=file-project-logo",
                "project/401/thumbnail=file-project-thumbnail"
            );
            assertThat(jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM file_metadata
                WHERE is_uploaded AND confirmed_at IS NULL
                """, Long.class)).isZero();
            assertThat(jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM file_metadata metadata
                WHERE EXISTS (SELECT 1 FROM file_usage usage WHERE usage.file_id = metadata.id)
                  AND metadata.unreferenced_at IS NOT NULL
                """, Long.class)).isZero();
            assertThat(jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM file_metadata
                WHERE id = 'unused-file' AND unreferenced_at IS NOT NULL
                """, Long.class)).isOne();
            assertThat(cutoverStatus()).isEqualTo("PENDING");

            // when & then: Form named owner와 standalone owner도 같은 Flyway 단계에서 확정한다.
            migrateTo(FORM_VERSION);
            assertThat(ownershipRows()).containsExactly(
                "10|project.application-form|100|default",
                "20|notice.vote|200|default",
                "30|feedback.template|300|default",
                "40|form.standalone|40|default"
            );

            // when & then: 아직 데이터가 없는 Chat은 legacy row와 파생 usage까지 제거한다.
            migrateTo(CHAT_VERSION);
            assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM chat_room", Long.class))
                .isZero();
            assertThat(usageRows()).noneMatch(row -> row.startsWith("chat.message/"));
            assertThat(jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM file_metadata
                WHERE id = 'file-chat' AND unreferenced_at IS NOT NULL
                """, Long.class)).isOne();

            migrateTo(LIFECYCLE_VERSION);
            assertThat(jdbcTemplate.queryForObject("""
                SELECT convalidated
                FROM pg_constraint
                WHERE conname = 'ck_file_metadata_upload_lifecycle'
                """, Boolean.class)).isTrue();
        }
    }

    @Test
    void 중복된_legacy_owner_file_reference는_Flyway를_실패시킨다() throws Exception {
        try (TestDatabase ignored = createTestDatabase()) {
            insertFile("file-duplicate");
            insertAnswer(501L, "{file-duplicate,file-duplicate}");

            assertThatThrownBy(() -> migrateTo(STORAGE_VERSION))
                .isInstanceOf(FlywayException.class);

            assertThat(tableExists("file_usage_owner")).isFalse();
        }
    }

    @Test
    void null_or_blank_legacy_file_reference는_Flyway를_실패시킨다() throws Exception {
        try (TestDatabase ignored = createTestDatabase()) {
            insertAnswer(501L, "{NULL}");
            insertAnswer(502L, "{\"\"}");

            assertThatThrownBy(() -> migrateTo(STORAGE_VERSION))
                .isInstanceOf(FlywayException.class);

            assertThat(tableExists("file_usage_owner")).isFalse();
        }
    }

    @Test
    void pending_legacy_file_reference는_Flyway를_실패시킨다() throws Exception {
        try (TestDatabase ignored = createTestDatabase()) {
            insertPendingFile("file-pending");
            insertAnswer(501L, "{file-pending}");

            assertThatThrownBy(() -> migrateTo(STORAGE_VERSION))
                .isInstanceOf(FlywayException.class);

            assertThat(tableExists("file_usage_owner")).isFalse();
        }
    }

    @Test
    void 후속_migration이_실패해도_Storage_cutover는_PENDING이다() throws Exception {
        try (TestDatabase ignored = createTestDatabase()) {
            insertStorageFixtures();
            insertFormFixtures();
            jdbcTemplate.update("""
                INSERT INTO notice_vote
                    (id, created_at, updated_at, notice_id, vote_id, starts_at, ends_at_exclusive)
                VALUES (2001, NOW(), NOW(), 200, 10, NOW(), NOW() + INTERVAL '1 day')
                """);

            migrateTo(STORAGE_VERSION);

            assertThatThrownBy(() -> migrateTo(FORM_VERSION))
                .isInstanceOf(FlywayException.class);
            assertThat(cutoverStatus()).isEqualTo("PENDING");
        }
    }

    @Test
    void migration_후_legacy_only_write가_있으면_certification은_실패한다() throws Exception {
        try (TestDatabase ignored = createTestDatabase()) {
            insertStorageFixtures();
            insertFormFixtures();
            migrateTo(LIFECYCLE_VERSION);
            insertConfirmedFileAfterMigration("file-late-writer");
            jdbcTemplate.update("""
                UPDATE member
                SET profile_image_id = 'file-late-writer', updated_at = NOW()
                WHERE id = 101
                """);

            assertThatThrownBy(this::certifyStorageCutover)
                .isInstanceOf(RuntimeException.class);
            assertThat(cutoverStatus()).isEqualTo("PENDING");

            Long ownerId = jdbcTemplate.queryForObject("""
                SELECT id FROM file_usage_owner
                WHERE usage_namespace = 'member'
                  AND resource_key = '101'
                  AND slot = 'profile-image'
                """, Long.class);
            jdbcTemplate.update("DELETE FROM file_usage WHERE owner_id = ?", ownerId);
            jdbcTemplate.update("""
                INSERT INTO file_usage (owner_id, file_id, created_at)
                VALUES (?, 'file-late-writer', NOW())
                """, ownerId);
            jdbcTemplate.update("""
                UPDATE file_metadata
                SET unreferenced_at = CASE
                    WHEN id = 'file-late-writer' THEN NULL
                    ELSE NOW()
                END
                WHERE id IN ('file-member', 'file-late-writer')
                """);

            certifyStorageCutover();

            assertThat(cutoverStatus()).isEqualTo("READY");
        }
    }

    private TestDatabase createTestDatabase() throws Exception {
        String databaseName = "registry_migration_" + UUID.randomUUID().toString().replace("-", "");
        try (
            var connection = DriverManager.getConnection(
                connectionDetails.getJdbcUrl(),
                connectionDetails.getUsername(),
                connectionDetails.getPassword()
            );
            var statement = connection.createStatement()
        ) {
            statement.execute("CREATE DATABASE " + databaseName);
        }

        migrationDataSource = new DriverManagerDataSource(
            databaseUrl(databaseName),
            connectionDetails.getUsername(),
            connectionDetails.getPassword()
        );
        jdbcTemplate = new JdbcTemplate(migrationDataSource);
        jdbcTemplate.execute("CREATE EXTENSION IF NOT EXISTS postgis");
        migrateTo(PRE_REGISTRY_VERSION);
        jdbcTemplate.execute("""
            TRUNCATE TABLE file_metadata, form, project, notice, user_feedback_template,
                member, school, chat_room, umc_product_member, certificate
            RESTART IDENTITY CASCADE
            """);
        return new TestDatabase(databaseName);
    }

    private void migrateTo(String version) {
        Flyway.configure()
            .dataSource(migrationDataSource)
            .locations("classpath:db/migration")
            .target(MigrationVersion.fromVersion(version))
            .load()
            .migrate();
    }

    private void certifyStorageCutover() throws Exception {
        String sql = new ClassPathResource(
            "db/operation/certify_file_usage_registry_cutover.sql"
        ).getContentAsString(StandardCharsets.UTF_8);
        jdbcTemplate.execute(sql);
    }

    private boolean tableExists(String tableName) {
        return Boolean.TRUE.equals(jdbcTemplate.queryForObject("""
            SELECT to_regclass('public.' || ?) IS NOT NULL
            """, Boolean.class, tableName));
    }

    private String cutoverStatus() {
        return jdbcTemplate.queryForObject("""
            SELECT status FROM file_usage_registry_cutover WHERE singleton = TRUE
            """, String.class);
    }

    private String databaseUrl(String databaseName) {
        String sourceUrl = connectionDetails.getJdbcUrl();
        int queryStart = sourceUrl.indexOf('?');
        String query = queryStart >= 0 ? sourceUrl.substring(queryStart) : "";
        String withoutQuery = queryStart >= 0 ? sourceUrl.substring(0, queryStart) : sourceUrl;
        return withoutQuery.substring(0, withoutQuery.lastIndexOf('/') + 1) + databaseName + query;
    }

    private final class TestDatabase implements AutoCloseable {

        private final String databaseName;

        private TestDatabase(String databaseName) {
            this.databaseName = databaseName;
        }

        @Override
        public void close() throws Exception {
            jdbcTemplate = null;
            migrationDataSource = null;
            try (
                var connection = DriverManager.getConnection(
                    connectionDetails.getJdbcUrl(),
                    connectionDetails.getUsername(),
                    connectionDetails.getPassword()
                );
                var statement = connection.createStatement()
            ) {
                statement.execute("DROP DATABASE " + databaseName + " WITH (FORCE)");
            }
        }
    }

    private List<String> usageRows() {
        return jdbcTemplate.queryForList("""
            SELECT owner.usage_namespace || '/' || owner.resource_key || '/' || owner.slot
                   || '=' || usage.file_id
            FROM file_usage usage
            JOIN file_usage_owner owner ON owner.id = usage.owner_id
            ORDER BY owner.usage_namespace, owner.resource_key, owner.slot, usage.file_id
            """, String.class);
    }

    private List<String> ownershipRows() {
        return jdbcTemplate.queryForList("""
            SELECT form_id::text || '|' || namespace || '|' || owner_resource_key || '|' || slot
            FROM form_ownership
            ORDER BY form_id
            """, String.class);
    }

    private void insertStorageFixtures() {
        List.of(
            "file-member", "file-school", "file-notice-a", "file-notice-b",
            "file-project-logo", "file-project-thumbnail", "file-answer",
            "file-shared", "file-chat", "file-umc-member", "file-certificate",
            "unused-file"
        ).forEach(this::insertFile);
        jdbcTemplate.update("""
            INSERT INTO school (id, created_at, updated_at, logo_image_id, name)
            VALUES (201, NOW(), NOW(), 'file-school', '테스트 학교')
            """);
        jdbcTemplate.update("""
            INSERT INTO member
                (id, created_at, updated_at, nickname, status, name, email, profile_image_id)
            VALUES (101, NOW(), NOW(), 'member101', 'ACTIVE', '멤버',
                    'member101@example.com', 'file-member')
            """);
        jdbcTemplate.update("""
            INSERT INTO notice
                (id, should_send_notification, author_member_id, created_at, updated_at, content, title)
            VALUES (301, FALSE, 101, NOW(), NOW(), '내용', '제목')
            """);
        jdbcTemplate.update("""
            INSERT INTO notice_image
                (id, display_order, created_at, updated_at, notice_id, image_id)
            VALUES
                (311, 1, NOW(), NOW(), 301, 'file-notice-a'),
                (312, 2, NOW(), NOW(), 301, 'file-notice-b')
            """);
        jdbcTemplate.update("""
            INSERT INTO project
                (id, created_at, updated_at, gisu_id, chapter_id, status,
                 logo_file_id, thumbnail_file_id, product_owner_member_id,
                 product_owner_school_id, created_by_member_id)
            VALUES
                (401, NOW(), NOW(), 1, 1, 'DRAFT', 'file-project-logo',
                 'file-project-thumbnail', 101, 201, 101)
            """);
        insertAnswer(501L, "{file-answer,file-shared}");
        jdbcTemplate.update("""
            INSERT INTO chat_room (id, created_at, updated_at)
            VALUES (61, NOW(), NOW())
            """);
        jdbcTemplate.update("""
            INSERT INTO chat_message
                (id, created_at, updated_at, room_id, sender_member_id,
                 content_type, content, file_metadata_ids)
            VALUES
                (601, NOW(), NOW(), 61, 101, 'FILE', NULL,
                 CAST('{file-chat,file-shared}' AS text[]))
            """);
        jdbcTemplate.update("""
            INSERT INTO umc_product_member
                (id, member_id, introduction, profile_image_id, created_at, updated_at)
            VALUES (701, 101, NULL, 'file-umc-member', NOW(), NOW())
            """);
        jdbcTemplate.update("""
            INSERT INTO certificate
                (id, created_at, updated_at, serial_number, template, status,
                 recipient_member_id, recipient_name, gisu_id, gisu_generation,
                 issued_by_member_id, issued_at, expires_at, file_id, file_sha256)
            VALUES
                (801, NOW(), NOW(), 'SERIAL-801', 'UMC_COURSE_COMPLETION', 'ISSUED',
                 101, '멤버', 1, 10, 101, NOW(), NOW() + INTERVAL '1 year',
                 'file-certificate', repeat('a', 64))
            """);
    }

    private void insertFormFixtures() {
        insertForm(10L);
        insertForm(20L);
        insertForm(30L);
        insertForm(40L);
        jdbcTemplate.update("""
            INSERT INTO project
                (id, created_at, updated_at, gisu_id, chapter_id, status,
                 product_owner_member_id, product_owner_school_id, created_by_member_id)
            VALUES (100, NOW(), NOW(), 2, 1, 'DRAFT', 101, 201, 101)
            """);
        jdbcTemplate.update("""
            INSERT INTO project_application_form
                (id, created_at, updated_at, project_id, form_id)
            VALUES (1000, NOW(), NOW(), 100, 10)
            """);
        jdbcTemplate.update("""
            INSERT INTO notice
                (id, should_send_notification, author_member_id, created_at, updated_at,
                 content, title)
            VALUES (200, FALSE, 101, NOW(), NOW(), '내용', '제목')
            """);
        jdbcTemplate.update("""
            INSERT INTO notice_vote
                (id, created_at, updated_at, notice_id, vote_id, starts_at, ends_at_exclusive)
            VALUES (2000, NOW(), NOW(), 200, 20, NOW(), NOW() + INTERVAL '1 day')
            """);
        jdbcTemplate.update("""
            INSERT INTO user_feedback_template
                (id, context, target_type, form_id, is_active, created_at, updated_at)
            VALUES (300, 'APPLICATION_SUBMITTED', 'NEW_CHALLENGER', 30, TRUE, NOW(), NOW())
            """);
    }

    private void insertForm(long formId) {
        jdbcTemplate.update("""
            INSERT INTO form
                (id, created_at, updated_at, created_member_id, title, status,
                 is_anonymous, allow_duplicate_responses)
            VALUES (?, NOW(), NOW(), 101, ?, 'DRAFT', FALSE, FALSE)
            """, formId, "Form " + formId);
    }

    private void insertAnswer(long answerId, String fileIds) {
        jdbcTemplate.execute("ALTER TABLE answer DISABLE TRIGGER ALL");
        try {
            jdbcTemplate.update("""
                INSERT INTO answer
                    (id, created_at, updated_at, form_response_id, question_id,
                     answered_as_type, file_ids)
                VALUES (?, NOW(), NOW(), 999, 999, 'FILE', CAST(? AS text[]))
                """, answerId, fileIds);
        } finally {
            jdbcTemplate.execute("ALTER TABLE answer ENABLE TRIGGER ALL");
        }
    }

    private void insertFile(String fileId) {
        jdbcTemplate.update("""
            INSERT INTO file_metadata
                (id, is_uploaded, created_at, updated_at, file_size, category,
                 storage_provider, content_type, storage_key, original_file_name)
            VALUES (?, TRUE, '2026-07-09T00:00:00Z', '2026-07-10T00:00:00Z', 1,
                    'ETC', 'AWS_S3', 'application/octet-stream', ?, ?)
            """, fileId, "migration/" + fileId, fileId);
    }

    private void insertPendingFile(String fileId) {
        jdbcTemplate.update("""
            INSERT INTO file_metadata
                (id, is_uploaded, created_at, updated_at, file_size, category,
                 storage_provider, content_type, storage_key, original_file_name)
            VALUES (?, FALSE, '2026-07-09T00:00:00Z', '2026-07-10T00:00:00Z', 1,
                    'ETC', 'AWS_S3', 'application/octet-stream', ?, ?)
            """, fileId, "migration/" + fileId, fileId);
    }

    private void insertConfirmedFileAfterMigration(String fileId) {
        jdbcTemplate.update("""
            INSERT INTO file_metadata
                (id, is_uploaded, confirmed_at, unreferenced_at, created_at, updated_at,
                 file_size, category, storage_provider, content_type, storage_key,
                 original_file_name)
            VALUES (?, TRUE, NOW(), NOW(), NOW(), NOW(), 1, 'ETC', 'AWS_S3',
                    'application/octet-stream', ?, ?)
            """, fileId, "migration/" + fileId, fileId);
    }
}
