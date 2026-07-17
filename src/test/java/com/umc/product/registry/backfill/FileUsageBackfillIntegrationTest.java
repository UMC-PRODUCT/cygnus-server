package com.umc.product.registry.backfill;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import com.umc.product.registry.adapter.out.jdbc.PostgresRegistryAdvisoryLockAdapter;
import com.umc.product.registry.adapter.out.jdbc.PostgresRegistryControlAdapter;
import com.umc.product.registry.application.service.RegistryBackfillCoordinator;
import com.umc.product.registry.domain.RegistryDriftType;
import com.umc.product.registry.domain.RegistryReconciliationResult;
import com.umc.product.registry.domain.RegistryStatus;
import com.umc.product.storage.adapter.out.backfill.StorageFileUsageRolloutAdapter;
import com.umc.product.support.IntegrationTestSupport;

@DisplayName("File usage registry restartable backfill PostgreSQL 통합")
class FileUsageBackfillIntegrationTest extends IntegrationTestSupport {

    private static final Instant CUTOVER_AT = Instant.parse("2026-07-18T01:00:00Z");

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private DataSource dataSource;

    @BeforeEach
    void resetControlRows() {
        jdbcTemplate.update("DELETE FROM registry_backfill_checkpoint");
        jdbcTemplate.update("DELETE FROM registry_cutover_state");
    }

    @Test
    void 현재_9개_consumer_source를_exact_usage로_backfill하고_legacy_lifecycle을_정규화한다() {
        // given
        insertAllNineSourceFixtures();
        insertFile("unused-file", true);
        RegistryBackfillCoordinator coordinator = coordinator();

        // when
        RegistryReconciliationResult result = coordinator.backfill(
            StorageFileUsageRolloutAdapter.REGISTRY_NAME);

        // then
        assertThat(result.isClean()).isTrue();
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
            SELECT COUNT(*)
            FROM registry_backfill_checkpoint
            WHERE registry_name = ? AND completed
            """, Long.class, StorageFileUsageRolloutAdapter.REGISTRY_NAME)).isEqualTo(9L);
        assertThat(jdbcTemplate.queryForObject("""
            SELECT status FROM registry_cutover_state WHERE registry_name = ?
            """, String.class, StorageFileUsageRolloutAdapter.REGISTRY_NAME))
            .isEqualTo(RegistryStatus.VALIDATED.name());
        assertThat(jdbcTemplate.queryForObject("""
            SELECT confirmed_at FROM file_metadata WHERE id = 'file-member'
            """, Instant.class)).isEqualTo(Instant.parse("2026-07-10T00:00:00Z"));
        assertThat(jdbcTemplate.queryForObject("""
            SELECT COUNT(*) FROM file_metadata fm
            WHERE EXISTS (SELECT 1 FROM file_usage fu WHERE fu.file_id = fm.id)
              AND fm.unreferenced_at IS NOT NULL
            """, Long.class)).isZero();
        assertThat(jdbcTemplate.queryForObject("""
            SELECT unreferenced_at FROM file_metadata WHERE id = 'unused-file'
            """, Instant.class)).isEqualTo(CUTOVER_AT);
    }

    @Test
    void duplicate_array는_exact_set으로_정규화하고_pending과_broken_reference는_drift로_block한다() {
        // given
        insertFile("pending-file", false);
        insertAnswer(502L, "{pending-file,pending-file,missing-file}");
        StorageFileUsageRolloutAdapter rollout = new StorageFileUsageRolloutAdapter(jdbcTemplate);
        RegistryBackfillCoordinator coordinator = coordinator(rollout);

        // when & then
        assertThatThrownBy(() -> coordinator.backfill(StorageFileUsageRolloutAdapter.REGISTRY_NAME))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("drift");
        assertThat(usageRows()).containsExactly(
            "form.answer/502/attachments=pending-file");
        RegistryReconciliationResult summary = rollout.reconcile(20);
        assertThat(summary.sources())
            .filteredOn(source -> source.sourceName().equals("form-answer-attachments"))
            .singleElement()
            .satisfies(source -> {
                assertThat(source.counts()).doesNotContainKey(RegistryDriftType.DUPLICATE_REFERENCE);
                assertThat(source.counts().get(RegistryDriftType.BROKEN_REFERENCE)).isEqualTo(1L);
                assertThat(source.counts().get(RegistryDriftType.LIFECYCLE_CONFLICT)).isEqualTo(1L);
            });
        assertThat(jdbcTemplate.queryForObject("""
            SELECT status FROM registry_cutover_state WHERE registry_name = ?
            """, String.class, StorageFileUsageRolloutAdapter.REGISTRY_NAME))
            .isEqualTo(RegistryStatus.BLOCKED.name());
    }

    @Test
    void blocked_retry는_checkpoint를_reset해_low_PK_update를_rescan하고_stale_usage를_보존한다() {
        // given
        insertFile("old-profile", true);
        insertFile("new-profile", true);
        insertMember(101L, "old-profile");
        StorageFileUsageRolloutAdapter rollout = new StorageFileUsageRolloutAdapter(jdbcTemplate);
        RegistryBackfillCoordinator coordinator = coordinator(rollout);

        assertThat(rollout.reconcile(20).sources())
            .filteredOn(source -> source.sourceName().equals("member-profile-image"))
            .singleElement()
            .satisfies(source -> assertThat(source.counts())
                .containsEntry(RegistryDriftType.SOURCE_ONLY_MISSING, 1L));
        coordinator.backfill(StorageFileUsageRolloutAdapter.REGISTRY_NAME);
        new PostgresRegistryControlAdapter(jdbcTemplate).transition(
            StorageFileUsageRolloutAdapter.REGISTRY_NAME,
            RegistryStatus.VALIDATED,
            RegistryStatus.BLOCKED,
            null,
            "old writer drift"
        );
        jdbcTemplate.update(
            "UPDATE member SET profile_image_id = 'new-profile' WHERE id = 101");

        // when & then
        assertThatThrownBy(() -> coordinator.backfill(StorageFileUsageRolloutAdapter.REGISTRY_NAME))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("drift");
        assertThat(usageRows()).containsExactly(
            "member/101/profile-image=new-profile",
            "member/101/profile-image=old-profile"
        );
        assertThat(jdbcTemplate.queryForMap("""
            SELECT last_parent_id, processed_rows, completed
            FROM registry_backfill_checkpoint
            WHERE registry_name = ? AND source_name = 'member-profile-image'
            """, StorageFileUsageRolloutAdapter.REGISTRY_NAME))
            .containsEntry("last_parent_id", 101L)
            .containsEntry("processed_rows", 1L)
            .containsEntry("completed", true);
        assertThat(rollout.reconcile(20).sources())
            .filteredOn(source -> source.sourceName().equals("member-profile-image"))
            .singleElement()
            .satisfies(source -> assertThat(source.counts())
                .containsEntry(RegistryDriftType.REGISTRY_ONLY_STALE, 1L)
                .containsEntry(RegistryDriftType.OWNERSHIP_CONFLICT, 1L));
        assertThat(jdbcTemplate.queryForObject("""
            SELECT status FROM registry_cutover_state WHERE registry_name = ?
            """, String.class, StorageFileUsageRolloutAdapter.REGISTRY_NAME))
            .isEqualTo(RegistryStatus.BLOCKED.name());
    }

    private RegistryBackfillCoordinator coordinator() {
        return coordinator(new StorageFileUsageRolloutAdapter(jdbcTemplate));
    }

    private RegistryBackfillCoordinator coordinator(StorageFileUsageRolloutAdapter rollout) {
        return new RegistryBackfillCoordinator(
            List.of(rollout),
            new PostgresRegistryControlAdapter(jdbcTemplate),
            new PostgresRegistryAdvisoryLockAdapter(dataSource),
            Clock.fixed(CUTOVER_AT, ZoneOffset.UTC),
            500,
            20
        );
    }

    private List<String> usageRows() {
        return jdbcTemplate.queryForList("""
            SELECT fuo.usage_namespace || '/' || fuo.resource_key || '/' || fuo.slot
                   || '=' || fu.file_id
            FROM file_usage fu
            JOIN file_usage_owner fuo ON fuo.id = fu.owner_id
            ORDER BY fuo.usage_namespace, fuo.resource_key, fuo.slot, fu.file_id
            """, String.class);
    }

    private void insertAllNineSourceFixtures() {
        List.of(
            "file-member", "file-school", "file-notice-a", "file-notice-b",
            "file-project-logo", "file-project-thumbnail", "file-answer",
            "file-shared", "file-chat", "file-umc-member", "file-certificate"
        ).forEach(fileId -> insertFile(fileId, true));
        jdbcTemplate.update("""
            INSERT INTO school (id, created_at, updated_at, logo_image_id, name)
            VALUES (201, NOW(), NOW(), 'file-school', '테스트 학교')
            """);
        insertMember(101L, "file-member");
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

    private void insertMember(long memberId, String profileImageId) {
        jdbcTemplate.update("""
            INSERT INTO member
                (id, created_at, updated_at, nickname, status, name, email, profile_image_id)
            VALUES (?, NOW(), NOW(), ?, 'ACTIVE', '멤버', ?, ?)
            """, memberId, "member" + memberId,
            "member" + memberId + "@example.com", profileImageId);
    }

    private void insertFile(String fileId, boolean uploaded) {
        jdbcTemplate.update("""
            INSERT INTO file_metadata
                (id, is_uploaded, created_at, updated_at, file_size, category,
                 storage_provider, content_type, storage_key, original_file_name,
                 unreferenced_at)
            VALUES (?, ?, '2026-07-09T00:00:00Z', '2026-07-10T00:00:00Z', 1,
                    'ETC', 'AWS_S3', 'application/octet-stream', ?, ?,
                    '2026-07-11T00:00:00Z')
            """, fileId, uploaded, "backfill/" + fileId, fileId);
    }
}
