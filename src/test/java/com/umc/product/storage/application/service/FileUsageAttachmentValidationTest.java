package com.umc.product.storage.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.verifyNoInteractions;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.storage.application.port.in.command.dto.ReplaceFileUsagesCommand;
import com.umc.product.storage.application.port.out.FileUsageRegistryReadinessPort;
import com.umc.product.storage.application.port.out.LoadFileUsagePort;
import com.umc.product.storage.application.port.out.LockFileMetadataPort;
import com.umc.product.storage.application.port.out.LockFileUsageOwnerPort;
import com.umc.product.storage.application.port.out.SaveFileMetadataPort;
import com.umc.product.storage.application.port.out.SaveFileUsagePort;
import com.umc.product.storage.domain.FileMetadata;
import com.umc.product.storage.domain.FileUsageCoordinate;
import com.umc.product.storage.domain.FileUsageOwner;
import com.umc.product.storage.domain.enums.FileCategory;
import com.umc.product.storage.domain.enums.FileUsageRegistryStatus;
import com.umc.product.storage.domain.enums.StorageProvider;
import com.umc.product.storage.domain.exception.StorageErrorCode;
import com.umc.product.storage.domain.exception.StorageException;

@ExtendWith(MockitoExtension.class)
class FileUsageAttachmentValidationTest {

    private static final FileUsageCoordinate COORDINATE =
        FileUsageCoordinate.of("project", "10", "attachments");
    private static final Instant CONFIRMED_AT = Instant.parse("2026-07-16T00:00:00Z");
    private static final Instant NOW = Instant.parse("2026-07-17T00:00:00Z");

    @Mock
    private LockFileUsageOwnerPort lockFileUsageOwnerPort;

    @Mock
    private LockFileMetadataPort lockFileMetadataPort;

    @Mock
    private LoadFileUsagePort loadFileUsagePort;

    @Mock
    private SaveFileUsagePort saveFileUsagePort;

    @Mock
    private SaveFileMetadataPort saveFileMetadataPort;

    @Mock
    private FileUsageRegistryReadinessPort readinessPort;

    private FileUsageCommandService sut;

    @BeforeEach
    void setUp() {
        sut = new FileUsageCommandService(
            lockFileUsageOwnerPort,
            lockFileMetadataPort,
            loadFileUsagePort,
            saveFileUsagePort,
            saveFileMetadataPort,
            readinessPort,
            Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    @Test
    @DisplayName("audit mode는 legacy uploaded true와 confirmedAt null 파일 attach를 허용한다")
    void audit_mode는_legacy_uploaded_true_confirmed_null_attach를_허용한다() {
        // given
        FileMetadata legacy = pendingFile("legacy-file", 7L);
        ReflectionTestUtils.setField(legacy, "isUploaded", true);
        stubNewAttachment(legacy);
        given(readinessPort.getStatus()).willReturn(FileUsageRegistryStatus.BACKFILLING);

        // when
        sut.replaceUsages(replace("legacy-file", 7L));

        // then
        then(saveFileUsagePort).should().addUsages(10L, Set.of("legacy-file"));
        assertThat(legacy.getUnreferencedAt()).isNull();
    }

    @Test
    @DisplayName("READY는 legacy uploaded true여도 confirmedAt null 파일 attach를 거부한다")
    void READY는_confirmedAt_null_attach를_거부한다() {
        // given
        FileMetadata legacy = pendingFile("legacy-file", 7L);
        ReflectionTestUtils.setField(legacy, "isUploaded", true);
        stubNewAttachment(legacy);
        given(readinessPort.getStatus()).willReturn(FileUsageRegistryStatus.READY);

        // when & then
        assertRejected(StorageErrorCode.FILE_UPLOAD_NOT_COMPLETED, replace("legacy-file", 7L));
    }

    @Test
    @DisplayName("신규 attach requester가 null이면 usage를 부분 변경하지 않는다")
    void 신규_attach_requester가_null이면_부분_변경하지_않는다() {
        assertAttachRejected(confirmedFile("file-a", 7L), null, StorageErrorCode.FILE_USE_FORBIDDEN);
    }

    @Test
    @DisplayName("uploader가 null이면 신규 attach를 거부한다")
    void uploader가_null이면_신규_attach를_거부한다() {
        assertAttachRejected(confirmedFile("file-a", null), 7L, StorageErrorCode.FILE_USE_FORBIDDEN);
    }

    @Test
    @DisplayName("uploader와 requester가 다르면 신규 attach를 거부한다")
    void uploader와_requester가_다르면_신규_attach를_거부한다() {
        assertAttachRejected(confirmedFile("file-a", 8L), 7L, StorageErrorCode.FILE_USE_FORBIDDEN);
    }

    @Test
    @DisplayName("pending 파일은 audit mode에서도 신규 attach를 거부한다")
    void pending_파일은_audit_mode에서도_신규_attach를_거부한다() {
        assertAttachRejected(pendingFile("file-a", 7L), 7L, StorageErrorCode.FILE_UPLOAD_NOT_COMPLETED);
    }

    @Test
    @DisplayName("active cleanup claim 파일은 신규 attach를 거부한다")
    void active_cleanup_claim_파일은_신규_attach를_거부한다() {
        FileMetadata claimed = confirmedFile("file-a", 7L);
        claimed.claimCleanup(UUID.fromString("00000000-0000-0000-0000-000000000001"), NOW);
        assertAttachRejected(claimed, 7L, StorageErrorCode.FILE_CLEANUP_IN_PROGRESS);
    }

    @Test
    @DisplayName("failed cleanup 파일은 신규 attach를 거부한다")
    void failed_cleanup_파일은_신규_attach를_거부한다() {
        FileMetadata failed = confirmedFile("file-a", 7L);
        UUID token = UUID.fromString("00000000-0000-0000-0000-000000000001");
        failed.claimCleanup(token, NOW.minusSeconds(60));
        failed.recordCleanupFailure(token, NOW, null, 1);
        assertAttachRejected(failed, 7L, StorageErrorCode.FILE_CLEANUP_FAILED);
    }

    @Test
    @DisplayName("backoff 중인 cleanup claim은 retry lifecycle을 유지하고 reattach를 거부한다")
    void backoff_중_cleanup_claim은_reattach를_거부한다() {
        // given
        UUID token = UUID.fromString("00000000-0000-0000-0000-000000000001");
        FileMetadata retrying = confirmedFile("file-a", 7L);
        retrying.claimCleanup(token, NOW.minusSeconds(60));
        retrying.recordCleanupFailure(token, NOW, NOW.plusSeconds(60), 10);

        // when & then
        assertAttachRejected(
            retrying,
            7L,
            StorageErrorCode.FILE_CLEANUP_IN_PROGRESS
        );

        assertThat(retrying.getCleanupClaimToken()).isEqualTo(token);
        assertThat(retrying.getCleanupAttempts()).isOne();
        assertThat(retrying.getCleanupNextAttemptAt()).isEqualTo(NOW.plusSeconds(60));
        assertThat(retrying.getUnreferencedAt()).isNull();
    }

    private void assertAttachRejected(
        FileMetadata metadata,
        Long requesterMemberId,
        StorageErrorCode expectedError
    ) {
        stubNewAttachment(metadata);
        if (requesterMemberId != null) {
            given(readinessPort.getStatus()).willReturn(FileUsageRegistryStatus.BACKFILLING);
        }
        assertRejected(expectedError, replace("file-a", requesterMemberId));
    }

    private void assertRejected(StorageErrorCode expectedError, ReplaceFileUsagesCommand command) {
        assertThatThrownBy(() -> sut.replaceUsages(command))
            .isInstanceOf(StorageException.class)
            .extracting("baseCode")
            .isEqualTo(expectedError);
        verifyNoInteractions(saveFileUsagePort, saveFileMetadataPort);
    }

    private void stubNewAttachment(FileMetadata metadata) {
        FileUsageOwner owner = org.mockito.Mockito.mock(FileUsageOwner.class);
        given(owner.getId()).willReturn(10L);
        given(lockFileUsageOwnerPort.lockOrCreateOwner(COORDINATE)).willReturn(owner);
        given(loadFileUsagePort.findFileIdsByOwnerId(10L)).willReturn(Set.of());
        given(lockFileMetadataPort.lockAllByFileIds(List.of(metadata.getId()))).willReturn(List.of(metadata));
    }

    private ReplaceFileUsagesCommand replace(String fileId, Long requesterMemberId) {
        return new ReplaceFileUsagesCommand(COORDINATE, Set.of(fileId), requesterMemberId);
    }

    private FileMetadata confirmedFile(String fileId, Long uploadedMemberId) {
        FileMetadata metadata = pendingFile(fileId, uploadedMemberId);
        metadata.markAsUploaded(CONFIRMED_AT);
        metadata.markReferenced();
        return metadata;
    }

    private FileMetadata pendingFile(String fileId, Long uploadedMemberId) {
        return FileMetadata.builder()
            .fileId(fileId)
            .originalFileName(fileId + ".pdf")
            .category(FileCategory.ETC)
            .contentType("application/pdf")
            .fileSize(1024L)
            .storageProvider(StorageProvider.AWS_S3)
            .storageKey("test/" + fileId + ".pdf")
            .uploadedMemberId(uploadedMemberId)
            .build();
    }
}
