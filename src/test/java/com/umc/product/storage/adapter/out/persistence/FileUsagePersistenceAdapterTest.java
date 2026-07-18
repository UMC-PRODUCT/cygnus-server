package com.umc.product.storage.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import com.umc.product.storage.domain.FileMetadata;
import com.umc.product.storage.domain.FileUsageCoordinate;
import com.umc.product.storage.domain.FileUsageOwner;
import com.umc.product.storage.domain.enums.FileCategory;
import com.umc.product.storage.domain.enums.StorageProvider;
import com.umc.product.support.PersistenceAdapterTest;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceException;

@PersistenceAdapterTest
@Import({FileUsagePersistenceAdapter.class, FileMetadataPersistenceAdapter.class})
class FileUsagePersistenceAdapterTest {

    private static final String LIFECYCLE_CONSTRAINT = "ck_file_metadata_upload_lifecycle";

    @Autowired
    private FileUsagePersistenceAdapter fileUsagePersistenceAdapter;

    @Autowired
    private FileMetadataPersistenceAdapter fileMetadataPersistenceAdapter;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("두 owner가 같은 파일을 참조하면 exact snapshot과 전체 count를 조회한다")
    void 두_owner가_같은_파일을_참조하면_exact_snapshot과_전체_count를_조회한다() {
        // given
        saveFile("file-a");
        saveFile("file-b");
        FileUsageOwner firstOwner = fileUsagePersistenceAdapter.lockOrCreateOwner(
            FileUsageCoordinate.of("member", "10", "profile-image")
        );
        FileUsageOwner secondOwner = fileUsagePersistenceAdapter.lockOrCreateOwner(
            FileUsageCoordinate.of("project", "20", "logo")
        );

        // when
        fileUsagePersistenceAdapter.addUsages(firstOwner.getId(), Set.of("file-b", "file-a"));
        fileUsagePersistenceAdapter.addUsages(secondOwner.getId(), Set.of("file-a"));
        entityManager.flush();
        entityManager.clear();

        // then
        assertThat(fileUsagePersistenceAdapter.findFileIdsByOwnerId(firstOwner.getId()))
            .containsExactlyInAnyOrder("file-a", "file-b");
        assertThat(fileUsagePersistenceAdapter.findFileIdsByOwnerId(secondOwner.getId()))
            .containsExactly("file-a");
        assertThat(fileUsagePersistenceAdapter.countByFileId("file-a")).isEqualTo(2L);
        assertThat(fileUsagePersistenceAdapter.countByFileId("file-b")).isEqualTo(1L);
    }

    @Test
    @DisplayName("owner native upsert 후 같은 row를 비관적 쓰기 lock으로 조회한다")
    void owner_native_upsert_후_같은_row를_비관적_쓰기_lock으로_조회한다() {
        // given
        FileUsageCoordinate coordinate = FileUsageCoordinate.of("notice", "30", "images");

        // when
        FileUsageOwner first = fileUsagePersistenceAdapter.lockOrCreateOwner(coordinate);
        FileUsageOwner second = fileUsagePersistenceAdapter.lockOrCreateOwner(coordinate);

        // then
        assertThat(second.getId()).isEqualTo(first.getId());
        assertThat(entityManager.getLockMode(second)).isEqualTo(LockModeType.PESSIMISTIC_WRITE);
    }

    @Test
    @DisplayName("파일 metadata는 중복을 제거한 ID 오름차순으로 비관적 lock한다")
    void 파일_metadata는_중복을_제거한_ID_오름차순으로_비관적_lock한다() {
        // given
        saveFile("file-b");
        saveFile("file-a");
        entityManager.flush();
        entityManager.clear();

        // when
        List<FileMetadata> locked = fileMetadataPersistenceAdapter.lockAllByFileIds(
            List.of("file-b", "file-a", "file-b")
        );

        // then
        assertThat(locked).extracting(FileMetadata::getId).containsExactly("file-a", "file-b");
        assertThat(locked).allSatisfy(metadata ->
            assertThat(entityManager.getLockMode(metadata)).isEqualTo(LockModeType.PESSIMISTIC_WRITE)
        );
    }

    @Test
    @DisplayName("중복 owner tuple은 PostgreSQL unique constraint가 거부한다")
    void 중복_owner_tuple은_PostgreSQL_unique_constraint가_거부한다() {
        // given
        fileUsagePersistenceAdapter.lockOrCreateOwner(FileUsageCoordinate.of("member", "10", "profile-image"));
        entityManager.flush();

        // when & then
        assertThatThrownBy(() -> insertOwner("member", "10", "profile-image"))
            .isInstanceOf(PersistenceException.class);
    }

    @Test
    @DisplayName("중복 owner file pair는 PostgreSQL unique constraint가 거부한다")
    void 중복_owner_file_pair는_PostgreSQL_unique_constraint가_거부한다() {
        // given
        saveFile("file-a");
        FileUsageOwner owner = fileUsagePersistenceAdapter.lockOrCreateOwner(
            FileUsageCoordinate.of("member", "10", "profile-image")
        );
        fileUsagePersistenceAdapter.addUsages(owner.getId(), Set.of("file-a"));
        entityManager.flush();

        // when & then
        assertThatThrownBy(() -> entityManager.createNativeQuery("""
            INSERT INTO file_usage (owner_id, file_id, created_at)
            VALUES (:ownerId, :fileId, NOW())
            """)
            .setParameter("ownerId", owner.getId())
            .setParameter("fileId", "file-a")
            .executeUpdate())
            .isInstanceOf(PersistenceException.class);
    }

    @Test
    @DisplayName("잘못된 namespace는 PostgreSQL check constraint가 거부한다")
    void 잘못된_namespace는_PostgreSQL_check_constraint가_거부한다() {
        assertThatThrownBy(() -> insertOwner("Member.Profile", "10", "profile-image"))
            .isInstanceOf(PersistenceException.class);
    }

    @Test
    @DisplayName("잘못된 resource key는 PostgreSQL check constraint가 거부한다")
    void 잘못된_resource_key는_PostgreSQL_check_constraint가_거부한다() {
        assertThatThrownBy(() -> insertOwner("member", "member 10", "profile-image"))
            .isInstanceOf(PersistenceException.class);
    }

    @Test
    @DisplayName("잘못된 slot은 PostgreSQL check constraint가 거부한다")
    void 잘못된_slot은_PostgreSQL_check_constraint가_거부한다() {
        assertThatThrownBy(() -> insertOwner("member", "10", "Profile_Image"))
            .isInstanceOf(PersistenceException.class);
    }

    @Test
    @DisplayName("usage가 남은 file metadata 삭제는 PostgreSQL restrict FK가 거부한다")
    void usage가_남은_file_metadata_삭제는_PostgreSQL_restrict_FK가_거부한다() {
        // given
        saveFile("file-a");
        FileUsageOwner owner = fileUsagePersistenceAdapter.lockOrCreateOwner(
            FileUsageCoordinate.of("member", "10", "profile-image")
        );
        fileUsagePersistenceAdapter.addUsages(owner.getId(), Set.of("file-a"));
        entityManager.flush();

        // when & then
        assertThatThrownBy(() -> entityManager.createNativeQuery("DELETE FROM file_metadata WHERE id = :fileId")
            .setParameter("fileId", "file-a")
            .executeUpdate())
            .isInstanceOf(PersistenceException.class);
    }

    @Test
    @DisplayName("owner 삭제는 해당 usage만 cascade하고 공유 파일의 다른 usage를 보존한다")
    void owner_삭제는_해당_usage만_cascade하고_공유_파일의_다른_usage를_보존한다() {
        // given
        saveFile("file-a");
        FileUsageOwner firstOwner = fileUsagePersistenceAdapter.lockOrCreateOwner(
            FileUsageCoordinate.of("member", "10", "profile-image")
        );
        FileUsageOwner secondOwner = fileUsagePersistenceAdapter.lockOrCreateOwner(
            FileUsageCoordinate.of("project", "20", "logo")
        );
        fileUsagePersistenceAdapter.addUsages(firstOwner.getId(), Set.of("file-a"));
        fileUsagePersistenceAdapter.addUsages(secondOwner.getId(), Set.of("file-a"));
        entityManager.flush();

        // when
        entityManager.createNativeQuery("DELETE FROM file_usage_owner WHERE id = :ownerId")
            .setParameter("ownerId", firstOwner.getId())
            .executeUpdate();
        entityManager.flush();
        entityManager.clear();

        // then
        assertThat(fileUsagePersistenceAdapter.countByFileId("file-a")).isEqualTo(1L);
        assertThat(fileUsagePersistenceAdapter.findFileIdsByOwnerId(secondOwner.getId()))
            .containsExactly("file-a");
    }

    @Test
    @DisplayName("legacy uploaded true confirmed null row를 expand schema에서 읽는다")
    @ResourceLock("file-upload-lifecycle-contract")
    void legacy_uploaded_true_confirmed_null_row를_expand_schema에서_읽는다() {
        // given
        jdbcTemplate.execute("""
            ALTER TABLE file_metadata
            DROP CONSTRAINT IF EXISTS ck_file_metadata_upload_lifecycle
            """);
        try {
            entityManager.createNativeQuery("""
                INSERT INTO file_metadata (
                    id, original_file_name, category, content_type, file_size,
                    storage_provider, storage_key, uploaded_member_id, is_uploaded,
                    created_at, updated_at
                ) VALUES (
                    'legacy-file', 'legacy.pdf', 'ETC', 'application/pdf', 1024,
                    'AWS_S3', 'test/legacy-file.pdf', 1, true, NOW(), NOW()
                )
                """).executeUpdate();
            entityManager.flush();
            entityManager.clear();

            // when
            FileMetadata legacy = fileMetadataPersistenceAdapter.findByFileId("legacy-file").orElseThrow();

            // then
            assertThat(legacy.isUploaded()).isTrue();
            assertThat(legacy.getConfirmedAt()).isNull();
            assertThat(legacy.isConfirmedForAudit()).isTrue();
        } finally {
            entityManager.createNativeQuery("DELETE FROM file_metadata WHERE id = 'legacy-file'")
                .executeUpdate();
            restoreContractConstraint();
        }
    }

    @Test
    @DisplayName("cleanup candidate partial index와 nullable lifecycle schema가 존재한다")
    void cleanup_candidate_partial_index와_nullable_lifecycle_schema가_존재한다() {
        // when
        Number lifecycleColumnCount = (Number) entityManager.createNativeQuery("""
            SELECT COUNT(*)
            FROM information_schema.columns
            WHERE table_schema = 'public'
              AND table_name = 'file_metadata'
              AND column_name IN (
                  'confirmed_at', 'unreferenced_at', 'cleanup_claim_token', 'cleanup_claimed_at',
                  'cleanup_attempts', 'cleanup_next_attempt_at', 'cleanup_failed_at'
              )
            """).getSingleResult();
        String indexDefinition = (String) entityManager.createNativeQuery("""
            SELECT indexdef
            FROM pg_indexes
            WHERE schemaname = 'public'
              AND indexname = 'idx_file_metadata_cleanup_candidate'
            """).getSingleResult();

        // then
        assertThat(lifecycleColumnCount.longValue()).isEqualTo(7L);
        assertThat(indexDefinition).contains("cleanup_failed_at IS NULL");
    }

    private FileMetadata saveFile(String fileId) {
        return fileMetadataPersistenceAdapter.save(FileMetadata.builder()
            .fileId(fileId)
            .originalFileName(fileId + ".pdf")
            .category(FileCategory.ETC)
            .contentType("application/pdf")
            .fileSize(1024L)
            .storageProvider(StorageProvider.AWS_S3)
            .storageKey("test/" + fileId + ".pdf")
            .uploadedMemberId(1L)
            .build());
    }

    private void insertOwner(String namespace, String resourceKey, String slot) {
        entityManager.createNativeQuery("""
            INSERT INTO file_usage_owner (usage_namespace, resource_key, slot, created_at, updated_at)
            VALUES (:namespace, :resourceKey, :slot, NOW(), NOW())
            """)
            .setParameter("namespace", namespace)
            .setParameter("resourceKey", resourceKey)
            .setParameter("slot", slot)
            .executeUpdate();
    }

    private void restoreContractConstraint() {
        jdbcTemplate.execute("""
            ALTER TABLE file_metadata
            ADD CONSTRAINT ck_file_metadata_upload_lifecycle
            CHECK (is_uploaded = (confirmed_at IS NOT NULL)) NOT VALID
            """);
        jdbcTemplate.execute("""
            ALTER TABLE file_metadata
            VALIDATE CONSTRAINT ck_file_metadata_upload_lifecycle
            """);
        assertThat(jdbcTemplate.queryForObject("""
            SELECT COUNT(*)
            FROM pg_constraint
            WHERE conrelid = 'file_metadata'::regclass
              AND conname = ?
            """, Integer.class, LIFECYCLE_CONSTRAINT)).isOne();
        assertThat(jdbcTemplate.queryForObject("""
            SELECT convalidated
            FROM pg_constraint
            WHERE conrelid = 'file_metadata'::regclass
              AND conname = ?
            """, Boolean.class, LIFECYCLE_CONSTRAINT)).isTrue();
    }
}
