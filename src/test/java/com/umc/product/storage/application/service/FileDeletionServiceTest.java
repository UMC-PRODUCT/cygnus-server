package com.umc.product.storage.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import java.lang.reflect.Method;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.authorization.application.port.in.query.GetChallengerRoleUseCase;
import com.umc.product.storage.application.port.in.command.dto.DeleteFileCommand;
import com.umc.product.storage.application.port.out.FileUsageRegistryReadinessPort;
import com.umc.product.storage.application.port.out.LoadFileUsagePort;
import com.umc.product.storage.application.port.out.LockFileMetadataPort;
import com.umc.product.storage.application.port.out.SaveFileMetadataPort;
import com.umc.product.storage.domain.FileMetadata;
import com.umc.product.storage.domain.enums.FileCategory;
import com.umc.product.storage.domain.enums.FileUsageRegistryStatus;
import com.umc.product.storage.domain.enums.StorageProvider;
import com.umc.product.storage.domain.exception.StorageErrorCode;
import com.umc.product.storage.domain.exception.StorageException;

@ExtendWith(MockitoExtension.class)
class FileDeletionServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-17T00:00:00Z");
    private static final UUID CURRENT_TOKEN = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID STALE_TOKEN = UUID.fromString("00000000-0000-0000-0000-000000000002");

    @Mock
    private FileUsageRegistryReadinessPort readinessPort;

    @Mock
    private LockFileMetadataPort lockFileMetadataPort;

    @Mock
    private LoadFileUsagePort loadFileUsagePort;

    @Mock
    private SaveFileMetadataPort saveFileMetadataPort;

    @Mock
    private GetChallengerRoleUseCase getChallengerRoleUseCase;

    private FileDeletionService sut;

    @BeforeEach
    void setUp() {
        sut = new FileDeletionService(
            readinessPort,
            lockFileMetadataPort,
            loadFileUsagePort,
            saveFileMetadataPort,
            getChallengerRoleUseCase,
            Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    @Test
    @DisplayName("claim과 finalize는 각각 짧은 REQUIRES_NEW transaction이다")
    void claim과_finalize는_REQUIRES_NEW_transaction이다() throws NoSuchMethodException {
        // when
        Method claim = FileDeletionService.class.getMethod("claim", DeleteFileCommand.class);
        Method finalizeDeletion = FileDeletionService.class.getMethod(
            "finalizeDeletion",
            FileDeletionService.DeletionClaim.class
        );

        // then
        assertThat(claim.getAnnotation(Transactional.class).propagation()).isEqualTo(Propagation.REQUIRES_NEW);
        assertThat(finalizeDeletion.getAnnotation(Transactional.class).propagation())
            .isEqualTo(Propagation.REQUIRES_NEW);
    }

    @Test
    @DisplayName("registry가 READY가 아니면 lifecycle이나 metadata 조회 전에 삭제를 차단한다")
    void registry가_READY가_아니면_metadata_조회_전에_삭제를_차단한다() {
        // given
        given(readinessPort.getStatus()).willReturn(FileUsageRegistryStatus.DISABLED);

        // when & then
        assertThatThrownBy(() -> sut.claim(deleteCommand(1L)))
            .isInstanceOf(StorageException.class)
            .extracting("baseCode")
            .isEqualTo(StorageErrorCode.FILE_USAGE_REGISTRY_NOT_READY);
        then(lockFileMetadataPort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("READY에서 없는 파일을 claim하면 FILE_NOT_FOUND다")
    void READY에서_없는_파일_claim은_FILE_NOT_FOUND다() {
        // given
        given(readinessPort.getStatus()).willReturn(FileUsageRegistryStatus.READY);
        given(lockFileMetadataPort.lockAllByFileIds(List.of("file-id"))).willReturn(List.of());

        // when & then
        assertThatThrownBy(() -> sut.claim(deleteCommand(1L)))
            .isInstanceOf(StorageException.class)
            .extracting("baseCode")
            .isEqualTo(StorageErrorCode.FILE_NOT_FOUND);
    }

    @Test
    @DisplayName("READY여도 global usage가 남은 파일은 claim하지 않는다")
    void READY여도_usage가_남은_파일은_claim하지_않는다() {
        // given
        FileMetadata metadata = file(1L);
        given(readinessPort.getStatus()).willReturn(FileUsageRegistryStatus.READY);
        given(lockFileMetadataPort.lockAllByFileIds(List.of("file-id"))).willReturn(List.of(metadata));
        given(loadFileUsagePort.countByFileId("file-id")).willReturn(1L);

        // when & then
        assertThatThrownBy(() -> sut.claim(deleteCommand(1L)))
            .isInstanceOf(StorageException.class)
            .extracting("baseCode")
            .isEqualTo(StorageErrorCode.FILE_IN_USE);
        assertThat(metadata.getCleanupClaimToken()).isNull();
        then(saveFileMetadataPort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("uploader도 SUPER_ADMIN도 아닌 requester는 claim할 수 없다")
    void 권한이_없는_requester는_claim할_수_없다() {
        // given
        FileMetadata metadata = file(1L);
        given(readinessPort.getStatus()).willReturn(FileUsageRegistryStatus.READY);
        given(lockFileMetadataPort.lockAllByFileIds(List.of("file-id"))).willReturn(List.of(metadata));
        given(getChallengerRoleUseCase.isSuperAdmin(2L)).willReturn(false);

        // when & then
        assertThatThrownBy(() -> sut.claim(deleteCommand(2L)))
            .isInstanceOf(StorageException.class)
            .extracting("baseCode")
            .isEqualTo(StorageErrorCode.FILE_DELETE_FORBIDDEN);
        then(loadFileUsagePort).should(never()).countByFileId("file-id");
        assertThat(metadata.getCleanupClaimToken()).isNull();
    }

    @Test
    @DisplayName("requester가 null이면 uploader도 null이어도 fail-closed하고 admin을 조회하지 않는다")
    void requester가_null이면_admin_조회_없이_삭제를_거부한다() {
        // given
        FileMetadata metadata = file(null);
        DeleteFileCommand command = org.mockito.Mockito.mock(DeleteFileCommand.class);
        given(command.fileId()).willReturn("file-id");
        given(command.requesterMemberId()).willReturn(null);
        given(readinessPort.getStatus()).willReturn(FileUsageRegistryStatus.READY);
        given(lockFileMetadataPort.lockAllByFileIds(List.of("file-id"))).willReturn(List.of(metadata));

        // when & then
        assertThatThrownBy(() -> sut.claim(command))
            .isInstanceOf(StorageException.class)
            .extracting("baseCode")
            .isEqualTo(StorageErrorCode.FILE_DELETE_FORBIDDEN);
        then(getChallengerRoleUseCase).shouldHaveNoInteractions();
        assertThat(metadata.getCleanupClaimToken()).isNull();
    }

    @Test
    @DisplayName("uploader가 아니어도 SUPER_ADMIN은 usage 0 파일을 claim할 수 있다")
    void SUPER_ADMIN은_usage_0_파일을_claim할_수_있다() {
        // given
        FileMetadata metadata = file(1L);
        given(readinessPort.getStatus()).willReturn(FileUsageRegistryStatus.READY);
        given(lockFileMetadataPort.lockAllByFileIds(List.of("file-id"))).willReturn(List.of(metadata));
        given(getChallengerRoleUseCase.isSuperAdmin(2L)).willReturn(true);
        given(loadFileUsagePort.countByFileId("file-id")).willReturn(0L);

        // when
        FileDeletionService.DeletionClaim claim = sut.claim(deleteCommand(2L));

        // then
        assertThat(metadata.getCleanupClaimToken()).isEqualTo(claim.token());
    }

    @Test
    @DisplayName("READY이고 권한과 usage 0을 만족하면 random claim receipt를 저장한다")
    void READY이고_권한과_usage_0이면_claim_receipt를_저장한다() {
        // given
        FileMetadata metadata = file(1L);
        given(readinessPort.getStatus()).willReturn(FileUsageRegistryStatus.READY);
        given(lockFileMetadataPort.lockAllByFileIds(List.of("file-id"))).willReturn(List.of(metadata));
        given(loadFileUsagePort.countByFileId("file-id")).willReturn(0L);

        // when
        FileDeletionService.DeletionClaim claim = sut.claim(deleteCommand(1L));

        // then
        assertThat(claim.fileId()).isEqualTo("file-id");
        assertThat(claim.storageKey()).isEqualTo("test/file-id.pdf");
        assertThat(claim.token()).isNotNull();
        assertThat(metadata.getCleanupClaimToken()).isEqualTo(claim.token());
        assertThat(metadata.getCleanupClaimedAt()).isEqualTo(NOW);
        then(saveFileMetadataPort).should().save(metadata);
    }

    @Test
    @DisplayName("기존 cleanup claim이 있으면 수동 삭제가 token을 덮어쓰지 않는다")
    void 기존_cleanup_claim은_덮어쓰지_않는다() {
        // given
        FileMetadata metadata = file(1L);
        metadata.claimCleanup(CURRENT_TOKEN, NOW.minusSeconds(60));
        given(readinessPort.getStatus()).willReturn(FileUsageRegistryStatus.READY);
        given(lockFileMetadataPort.lockAllByFileIds(List.of("file-id"))).willReturn(List.of(metadata));

        // when & then
        assertThatThrownBy(() -> sut.claim(deleteCommand(1L)))
            .isInstanceOf(StorageException.class)
            .extracting("baseCode")
            .isEqualTo(StorageErrorCode.FILE_CLEANUP_IN_PROGRESS);
        assertThat(metadata.getCleanupClaimToken()).isEqualTo(CURRENT_TOKEN);
    }

    @Test
    @DisplayName("stale token finalize는 metadata를 삭제하지 않는 no-op이다")
    void stale_token_finalize는_no_op이다() {
        // given
        FileMetadata metadata = file(1L);
        metadata.claimCleanup(CURRENT_TOKEN, NOW);
        given(lockFileMetadataPort.lockAllByFileIds(List.of("file-id"))).willReturn(List.of(metadata));
        FileDeletionService.DeletionClaim stale =
            new FileDeletionService.DeletionClaim("file-id", metadata.getStorageKey(), STALE_TOKEN);

        // when
        boolean finalized = sut.finalizeDeletion(stale);

        // then
        assertThat(finalized).isFalse();
        then(loadFileUsagePort).should(never()).countByFileId("file-id");
        then(saveFileMetadataPort).should(never()).deleteByFileId("file-id");
    }

    @Test
    @DisplayName("matching token이어도 finalize 시 usage가 생겼으면 metadata를 보존한다")
    void matching_token이어도_usage가_생겼으면_metadata를_보존한다() {
        // given
        FileMetadata metadata = file(1L);
        metadata.claimCleanup(CURRENT_TOKEN, NOW);
        given(lockFileMetadataPort.lockAllByFileIds(List.of("file-id"))).willReturn(List.of(metadata));
        given(loadFileUsagePort.countByFileId("file-id")).willReturn(1L);
        FileDeletionService.DeletionClaim claim =
            new FileDeletionService.DeletionClaim("file-id", metadata.getStorageKey(), CURRENT_TOKEN);

        // when
        boolean finalized = sut.finalizeDeletion(claim);

        // then
        assertThat(finalized).isFalse();
        then(saveFileMetadataPort).should(never()).deleteByFileId("file-id");
    }

    @Test
    @DisplayName("matching token과 usage 0이면 finalize가 metadata를 삭제한다")
    void matching_token과_usage_0이면_metadata를_삭제한다() {
        // given
        FileMetadata metadata = file(1L);
        metadata.claimCleanup(CURRENT_TOKEN, NOW);
        given(lockFileMetadataPort.lockAllByFileIds(List.of("file-id"))).willReturn(List.of(metadata));
        given(loadFileUsagePort.countByFileId("file-id")).willReturn(0L);
        FileDeletionService.DeletionClaim claim =
            new FileDeletionService.DeletionClaim("file-id", metadata.getStorageKey(), CURRENT_TOKEN);

        // when
        boolean finalized = sut.finalizeDeletion(claim);

        // then
        assertThat(finalized).isTrue();
        then(saveFileMetadataPort).should().deleteByFileId("file-id");
    }

    private DeleteFileCommand deleteCommand(Long requesterMemberId) {
        return new DeleteFileCommand("file-id", requesterMemberId);
    }

    private FileMetadata file(Long uploadedMemberId) {
        return FileMetadata.builder()
            .fileId("file-id")
            .originalFileName("file-id.pdf")
            .category(FileCategory.ETC)
            .contentType("application/pdf")
            .fileSize(1024L)
            .storageProvider(StorageProvider.AWS_S3)
            .storageKey("test/file-id.pdf")
            .uploadedMemberId(uploadedMemberId)
            .build();
    }
}
