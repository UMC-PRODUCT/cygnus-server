package com.umc.product.storage.application.service;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.umc.product.storage.application.port.in.command.ManageFileUsageUseCase;
import com.umc.product.storage.application.port.in.command.ManageFileUseCase;
import com.umc.product.storage.application.port.in.command.dto.BulkRemoveFileUsagesCommand;
import com.umc.product.storage.application.port.in.command.dto.DeleteFileCommand;
import com.umc.product.storage.application.port.in.command.dto.ReplaceFileUsagesCommand;
import com.umc.product.storage.application.port.out.FileUsageRegistryReadinessPort;
import com.umc.product.storage.application.port.out.LoadFileMetadataPort;
import com.umc.product.storage.application.port.out.SaveFileMetadataPort;
import com.umc.product.storage.application.port.out.dto.StorageObjectInfo;
import com.umc.product.storage.domain.FileMetadata;
import com.umc.product.storage.domain.FileUsageCoordinate;
import com.umc.product.storage.domain.enums.FileCategory;
import com.umc.product.storage.domain.enums.FileUsageRegistryStatus;
import com.umc.product.storage.domain.enums.StorageProvider;
import com.umc.product.storage.domain.exception.StorageErrorCode;
import com.umc.product.storage.domain.exception.StorageException;
import com.umc.product.support.IntegrationTestSupport;

@Import(FileUsageConcurrencyIntegrationTest.ReadyRegistryConfig.class)
class FileUsageConcurrencyIntegrationTest extends IntegrationTestSupport {

    private static final Instant CONFIRMED_AT = Instant.parse("2026-07-16T00:00:00Z");

    @Autowired
    private ManageFileUsageUseCase manageFileUsageUseCase;

    @Autowired
    private ManageFileUseCase manageFileUseCase;

    @Autowired
    private FileDeletionService fileDeletionService;

    @Autowired
    private SaveFileMetadataPort saveFileMetadataPort;

    @Autowired
    private LoadFileMetadataPort loadFileMetadataPort;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    void 동일_owner_concurrent_replace는_exact_snapshot으로_직렬화된다() throws Exception {
        // given
        saveConfirmedFile("file-a", 7L);
        saveConfirmedFile("file-b", 7L);
        FileUsageCoordinate coordinate = FileUsageCoordinate.of("project", "10", "attachments");

        // when
        runConcurrently(
            () -> manageFileUsageUseCase.replaceUsages(replace(coordinate, Set.of("file-a"), 7L)),
            () -> manageFileUsageUseCase.replaceUsages(replace(coordinate, Set.of("file-b"), 7L))
        );

        // then
        Set<String> finalSnapshot = usageSnapshot(coordinate);
        assertThat(finalSnapshot).isIn(Set.of("file-a"), Set.of("file-b"));
        String referencedFile = finalSnapshot.iterator().next();
        String detachedFile = referencedFile.equals("file-a") ? "file-b" : "file-a";
        assertThat(unreferencedAt(referencedFile)).isNull();
        assertThat(unreferencedAt(detachedFile)).isNotNull();
    }

    @Test
    void 역순_bulk_remove는_deadlock_없이_완료된다() throws Exception {
        // given
        saveConfirmedFile("file-a", 7L);
        saveConfirmedFile("file-b", 7L);
        FileUsageCoordinate first = FileUsageCoordinate.of("notice", "10", "images");
        FileUsageCoordinate second = FileUsageCoordinate.of("project", "20", "attachments");
        manageFileUsageUseCase.replaceUsages(replace(first, Set.of("file-a"), 7L));
        manageFileUsageUseCase.replaceUsages(replace(second, Set.of("file-b"), 7L));

        // when
        runConcurrently(
            () -> manageFileUsageUseCase.removeAll(new BulkRemoveFileUsagesCommand(List.of(first, second))),
            () -> manageFileUsageUseCase.removeAll(new BulkRemoveFileUsagesCommand(List.of(second, first)))
        );

        // then
        assertThat(totalUsageCount()).isZero();
        assertThat(ownerAnchorCount()).isEqualTo(2L);
        assertThat(unreferencedAt("file-a")).isNotNull();
        assertThat(unreferencedAt("file-b")).isNotNull();
    }

    @Test
    void 공유_파일의_부분_detach는_unreferencedAt을_기록하지_않는다() {
        // given
        saveConfirmedFile("shared-file", 7L);
        FileUsageCoordinate first = FileUsageCoordinate.of("notice", "10", "images");
        FileUsageCoordinate second = FileUsageCoordinate.of("project", "20", "attachments");
        manageFileUsageUseCase.replaceUsages(replace(first, Set.of("shared-file"), 7L));
        manageFileUsageUseCase.replaceUsages(replace(second, Set.of("shared-file"), 7L));

        // when
        manageFileUsageUseCase.replaceUsages(replace(first, Set.of(), null));

        // then
        assertThat(totalUsageCount()).isEqualTo(1L);
        assertThat(unreferencedAt("shared-file")).isNull();
    }

    @Test
    void lifecycle_batch_flush는_outer_transaction_rollback에_참여한다() {
        // given
        saveConfirmedFile("rollback-file", 7L);
        FileUsageCoordinate coordinate = FileUsageCoordinate.of("notice", "10", "images");
        manageFileUsageUseCase.replaceUsages(replace(coordinate, Set.of("rollback-file"), 7L));
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);

        // when
        Boolean lifecycleVisibleBeforeRollback = transactionTemplate.execute(status -> {
            manageFileUsageUseCase.removeAll(new BulkRemoveFileUsagesCommand(List.of(coordinate)));
            boolean lifecycleVisible = unreferencedAt("rollback-file") != null;
            status.setRollbackOnly();
            return lifecycleVisible;
        });

        // then
        assertThat(lifecycleVisibleBeforeRollback).isTrue();
        assertThat(usageSnapshot(coordinate)).containsExactly("rollback-file");
        assertThat(unreferencedAt("rollback-file")).isNull();
    }

    @Test
    void upload_confirm은_confirmed와_unreferenced_lifecycle을_같이_기록한다() {
        // given
        FileMetadata pending = savePendingFile("pending-file", 7L);
        given(storagePort.findObjectInfoByStorageKey(pending.getStorageKey()))
            .willReturn(java.util.Optional.of(StorageObjectInfo.of(
                pending.getStorageKey(),
                pending.getFileSize(),
                pending.getContentType()
            )));

        // when
        manageFileUseCase.confirmUpload("pending-file");

        // then
        FileMetadata confirmed = loadFileMetadataPort.findByFileId("pending-file").orElseThrow();
        assertThat(confirmed.getConfirmedAt()).isNotNull();
        assertThat(confirmed.getUnreferencedAt()).isEqualTo(confirmed.getConfirmedAt());
    }

    @Test
    void READY_manual_delete는_in_use_파일을_거부한다() {
        // given
        saveConfirmedFile("in-use-file", 7L);
        FileUsageCoordinate coordinate = FileUsageCoordinate.of("project", "10", "attachments");
        manageFileUsageUseCase.replaceUsages(replace(coordinate, Set.of("in-use-file"), 7L));

        // when & then
        assertThatThrownBy(() -> manageFileUseCase.deleteFile(new DeleteFileCommand("in-use-file", 7L)))
            .isInstanceOf(StorageException.class)
            .extracting("baseCode")
            .isEqualTo(StorageErrorCode.FILE_IN_USE);
        verify(storagePort, never()).delete("test/in-use-file.pdf");
        assertThat(loadFileMetadataPort.findByFileId("in-use-file")).isPresent();
    }

    @Test
    void stale_cleanup_token_finalize는_metadata와_current_claim을_보존한다() {
        // given
        UUID currentToken = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID staleToken = UUID.fromString("00000000-0000-0000-0000-000000000002");
        FileMetadata metadata = saveConfirmedFile("claimed-file", 7L);
        metadata.claimCleanup(currentToken, CONFIRMED_AT.plusSeconds(60));
        saveFileMetadataPort.save(metadata);
        FileDeletionService.DeletionClaim stale =
            new FileDeletionService.DeletionClaim("claimed-file", metadata.getStorageKey(), staleToken);

        // when
        boolean finalized = fileDeletionService.finalizeDeletion(stale);

        // then
        FileMetadata preserved = loadFileMetadataPort.findByFileId("claimed-file").orElseThrow();
        assertThat(finalized).isFalse();
        assertThat(preserved.getCleanupClaimToken()).isEqualTo(currentToken);
    }

    @Test
    void S3_삭제_실패는_metadata와_claim_receipt를_보존한다() {
        // given
        FileMetadata metadata = saveConfirmedFile("delete-file", 7L);
        willThrow(new StorageException(StorageErrorCode.STORAGE_DELETE_FAILED))
            .given(storagePort)
            .delete(metadata.getStorageKey());

        // when & then
        assertThatThrownBy(() -> manageFileUseCase.deleteFile(new DeleteFileCommand("delete-file", 7L)))
            .isInstanceOf(StorageException.class)
            .extracting("baseCode")
            .isEqualTo(StorageErrorCode.STORAGE_DELETE_FAILED);
        FileMetadata preserved = loadFileMetadataPort.findByFileId("delete-file").orElseThrow();
        assertThat(preserved.getCleanupClaimToken()).isNotNull();
        assertThat(preserved.getCleanupClaimedAt()).isNotNull();
    }

    @Test
    void matching_cleanup_receipt는_metadata를_제거한다() {
        // given
        FileMetadata metadata = saveConfirmedFile("delete-file", 7L);

        // when
        manageFileUseCase.deleteFile(new DeleteFileCommand("delete-file", 7L));

        // then
        verify(storagePort).delete(metadata.getStorageKey());
        assertThat(loadFileMetadataPort.findByFileId("delete-file")).isEmpty();
    }

    private void runConcurrently(Runnable first, Runnable second) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Future<?> firstFuture = executor.submit(() -> runAfterSignal(first, ready, start));
            Future<?> secondFuture = executor.submit(() -> runAfterSignal(second, ready, start));
            assertThat(ready.await(5, SECONDS)).isTrue();
            start.countDown();
            firstFuture.get(10, SECONDS);
            secondFuture.get(10, SECONDS);
        } finally {
            executor.shutdownNow();
            assertThat(executor.awaitTermination(5, SECONDS)).isTrue();
        }
    }

    private void runAfterSignal(Runnable action, CountDownLatch ready, CountDownLatch start) {
        ready.countDown();
        try {
            if (!start.await(5, SECONDS)) {
                throw new IllegalStateException("concurrency start signal timeout");
            }
            action.run();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("concurrency test interrupted", e);
        }
    }

    private ReplaceFileUsagesCommand replace(
        FileUsageCoordinate coordinate,
        Set<String> fileIds,
        Long requesterMemberId
    ) {
        return new ReplaceFileUsagesCommand(coordinate, fileIds, requesterMemberId);
    }

    private Set<String> usageSnapshot(FileUsageCoordinate coordinate) {
        return new LinkedHashSet<>(jdbcTemplate.queryForList("""
            SELECT fu.file_id
            FROM file_usage fu
            JOIN file_usage_owner fuo ON fuo.id = fu.owner_id
            WHERE fuo.usage_namespace = ?
              AND fuo.resource_key = ?
              AND fuo.slot = ?
            ORDER BY fu.file_id
            """, String.class, coordinate.usageNamespace(), coordinate.resourceKey(), coordinate.slot()));
    }

    private Long totalUsageCount() {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM file_usage", Long.class);
    }

    private Long ownerAnchorCount() {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM file_usage_owner", Long.class);
    }

    private Instant unreferencedAt(String fileId) {
        OffsetDateTime value = jdbcTemplate.queryForObject(
            "SELECT unreferenced_at FROM file_metadata WHERE id = ?",
            OffsetDateTime.class,
            fileId
        );
        return value == null ? null : value.toInstant();
    }

    private FileMetadata saveConfirmedFile(String fileId, Long uploadedMemberId) {
        FileMetadata metadata = savePendingFile(fileId, uploadedMemberId);
        metadata.markAsUploaded(CONFIRMED_AT);
        metadata.markReferenced();
        return saveFileMetadataPort.save(metadata);
    }

    private FileMetadata savePendingFile(String fileId, Long uploadedMemberId) {
        return saveFileMetadataPort.save(FileMetadata.builder()
            .fileId(fileId)
            .originalFileName(fileId + ".pdf")
            .category(FileCategory.ETC)
            .contentType("application/pdf")
            .fileSize(1024L)
            .storageProvider(StorageProvider.AWS_S3)
            .storageKey("test/" + fileId + ".pdf")
            .uploadedMemberId(uploadedMemberId)
            .build());
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class ReadyRegistryConfig {

        @Bean
        @Primary
        FileUsageRegistryReadinessPort readyFileUsageRegistry() {
            return () -> FileUsageRegistryStatus.READY;
        }
    }
}
