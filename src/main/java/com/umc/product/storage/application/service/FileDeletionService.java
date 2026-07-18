package com.umc.product.storage.application.service;

import java.time.Clock;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.authorization.application.port.in.query.GetChallengerRoleUseCase;
import com.umc.product.storage.application.port.in.command.dto.DeleteFileCommand;
import com.umc.product.storage.application.port.out.FileUsageRegistryReadinessPort;
import com.umc.product.storage.application.port.out.LoadFileUsagePort;
import com.umc.product.storage.application.port.out.LockFileMetadataPort;
import com.umc.product.storage.application.port.out.SaveFileMetadataPort;
import com.umc.product.storage.domain.FileMetadata;
import com.umc.product.storage.domain.enums.FileUsageRegistryStatus;
import com.umc.product.storage.domain.exception.StorageErrorCode;
import com.umc.product.storage.domain.exception.StorageException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FileDeletionService {

    private final FileUsageRegistryReadinessPort readinessPort;
    private final LockFileMetadataPort lockFileMetadataPort;
    private final LoadFileUsagePort loadFileUsagePort;
    private final SaveFileMetadataPort saveFileMetadataPort;
    private final GetChallengerRoleUseCase getChallengerRoleUseCase;
    private final Clock clock;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public DeletionClaim claim(DeleteFileCommand command) {
        if (readinessPort.getStatus() != FileUsageRegistryStatus.READY) {
            throw new StorageException(StorageErrorCode.FILE_USAGE_REGISTRY_NOT_READY);
        }

        FileMetadata metadata = lockFile(command.fileId());
        validateDeletePermission(metadata, command.requesterMemberId());
        validateCleanupState(metadata);
        if (loadFileUsagePort.countByFileId(command.fileId()) != 0L) {
            throw new StorageException(StorageErrorCode.FILE_IN_USE);
        }

        UUID token = UUID.randomUUID();
        metadata.claimCleanup(token, clock.instant());
        saveFileMetadataPort.save(metadata);
        return new DeletionClaim(metadata.getId(), metadata.getStorageKey(), token);
    }

    public boolean isReadyForPhysicalDelete() {
        return readinessPort.getStatus() == FileUsageRegistryStatus.READY;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean finalizeDeletion(DeletionClaim claim) {
        List<FileMetadata> locked = lockFileMetadataPort.lockAllByFileIds(List.of(claim.fileId()));
        if (locked.isEmpty()) {
            return false;
        }

        FileMetadata metadata = locked.getFirst();
        if (!metadata.matchesCleanupClaim(claim.token())) {
            return false;
        }
        if (loadFileUsagePort.countByFileId(claim.fileId()) != 0L) {
            return false;
        }

        saveFileMetadataPort.deleteByFileId(claim.fileId());
        return true;
    }

    private FileMetadata lockFile(String fileId) {
        return lockFileMetadataPort.lockAllByFileIds(List.of(fileId)).stream()
            .findFirst()
            .orElseThrow(() -> new StorageException(StorageErrorCode.FILE_NOT_FOUND));
    }

    private void validateDeletePermission(FileMetadata metadata, Long requesterMemberId) {
        if (requesterMemberId == null) {
            throw new StorageException(StorageErrorCode.FILE_DELETE_FORBIDDEN);
        }
        if (Objects.equals(metadata.getUploadedMemberId(), requesterMemberId) || isSuperAdmin(requesterMemberId)) {
            return;
        }
        throw new StorageException(StorageErrorCode.FILE_DELETE_FORBIDDEN);
    }

    private void validateCleanupState(FileMetadata metadata) {
        if (metadata.hasCleanupClaim()) {
            throw new StorageException(StorageErrorCode.FILE_CLEANUP_IN_PROGRESS);
        }
        if (metadata.hasCleanupFailure()) {
            throw new StorageException(StorageErrorCode.FILE_CLEANUP_FAILED);
        }
    }

    private boolean isSuperAdmin(Long memberId) {
        return getChallengerRoleUseCase.isSuperAdmin(memberId);
    }

    public record DeletionClaim(
        String fileId,
        String storageKey,
        UUID token
    ) {

        public DeletionClaim {
            Objects.requireNonNull(fileId, "삭제 claim file ID는 필수입니다.");
            Objects.requireNonNull(storageKey, "삭제 claim storage key는 필수입니다.");
            Objects.requireNonNull(token, "삭제 claim token은 필수입니다.");
        }
    }
}
