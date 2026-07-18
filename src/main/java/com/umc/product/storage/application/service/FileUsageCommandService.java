package com.umc.product.storage.application.service;

import java.time.Clock;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.storage.application.port.in.command.ManageFileUsageUseCase;
import com.umc.product.storage.application.port.in.command.dto.BulkRemoveFileUsagesCommand;
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
import com.umc.product.storage.domain.enums.FileUsageRegistryStatus;
import com.umc.product.storage.domain.exception.StorageErrorCode;
import com.umc.product.storage.domain.exception.StorageException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FileUsageCommandService implements ManageFileUsageUseCase {

    private static final Comparator<FileUsageCoordinate> OWNER_ORDER = Comparator
        .comparing(FileUsageCoordinate::usageNamespace)
        .thenComparing(FileUsageCoordinate::resourceKey)
        .thenComparing(FileUsageCoordinate::slot);

    private final LockFileUsageOwnerPort lockFileUsageOwnerPort;
    private final LockFileMetadataPort lockFileMetadataPort;
    private final LoadFileUsagePort loadFileUsagePort;
    private final SaveFileUsagePort saveFileUsagePort;
    private final SaveFileMetadataPort saveFileMetadataPort;
    private final FileUsageRegistryReadinessPort readinessPort;
    private final Clock clock;

    @Override
    @Transactional
    public void replaceUsages(ReplaceFileUsagesCommand command) {
        FileUsageOwner owner = lockFileUsageOwnerPort.lockOrCreateOwner(command.owner());
        Set<String> currentFileIds = loadFileUsagePort.findFileIdsByOwnerId(owner.getId());
        List<String> fileIdsToLock = sortedUnion(currentFileIds, command.fileIds());
        Map<String, FileMetadata> metadataById = lockMetadata(fileIdsToLock);

        Set<String> newlyAttachedFileIds = difference(command.fileIds(), currentFileIds);
        validateNewAttachments(newlyAttachedFileIds, metadataById, command.requesterMemberId());

        Set<String> removedFileIds = difference(currentFileIds, command.fileIds());
        if (!removedFileIds.isEmpty()) {
            saveFileUsagePort.removeUsages(owner.getId(), removedFileIds);
        }
        if (!newlyAttachedFileIds.isEmpty()) {
            saveFileUsagePort.addUsages(owner.getId(), newlyAttachedFileIds);
        }

        markAttached(newlyAttachedFileIds, metadataById);
        markDetached(removedFileIds, metadataById);
        if (!newlyAttachedFileIds.isEmpty() || !removedFileIds.isEmpty()) {
            saveFileMetadataPort.flush();
        }
    }

    @Override
    @Transactional
    public void removeAll(BulkRemoveFileUsagesCommand command) {
        List<FileUsageOwner> owners = command.owners().stream()
            .distinct()
            .sorted(OWNER_ORDER)
            .map(lockFileUsageOwnerPort::lockOrCreateOwner)
            .toList();

        Map<Long, Set<String>> snapshotsByOwnerId = new LinkedHashMap<>();
        owners.forEach(owner -> snapshotsByOwnerId.put(
            owner.getId(),
            loadFileUsagePort.findFileIdsByOwnerId(owner.getId())
        ));

        List<String> fileIdsToLock = snapshotsByOwnerId.values().stream()
            .flatMap(Set::stream)
            .distinct()
            .sorted()
            .toList();
        Map<String, FileMetadata> metadataById = lockMetadata(fileIdsToLock);
        requireAllMetadata(fileIdsToLock, metadataById);

        snapshotsByOwnerId.forEach((ownerId, fileIds) -> {
            if (!fileIds.isEmpty()) {
                saveFileUsagePort.removeUsages(ownerId, fileIds);
            }
        });
        markDetached(new LinkedHashSet<>(fileIdsToLock), metadataById);
        if (!fileIdsToLock.isEmpty()) {
            saveFileMetadataPort.flush();
        }
    }

    private Map<String, FileMetadata> lockMetadata(List<String> fileIds) {
        if (fileIds.isEmpty()) {
            return Map.of();
        }
        return lockFileMetadataPort.lockAllByFileIds(fileIds).stream()
            .collect(Collectors.toMap(FileMetadata::getId, Function.identity()));
    }

    private void validateNewAttachments(
        Set<String> newlyAttachedFileIds,
        Map<String, FileMetadata> metadataById,
        Long requesterMemberId
    ) {
        requireAllMetadata(newlyAttachedFileIds, metadataById);
        if (newlyAttachedFileIds.isEmpty()) {
            return;
        }
        if (requesterMemberId == null) {
            throw new StorageException(StorageErrorCode.FILE_USE_FORBIDDEN);
        }

        FileUsageRegistryStatus registryStatus = readinessPort.getStatus();
        for (String fileId : new TreeSet<>(newlyAttachedFileIds)) {
            FileMetadata metadata = metadataById.get(fileId);
            validateUploader(metadata, requesterMemberId);
            validateUploadLifecycle(metadata, registryStatus);
            validateCleanupState(metadata);
        }
    }

    private void validateUploader(FileMetadata metadata, Long requesterMemberId) {
        if (metadata.getUploadedMemberId() == null
            || !Objects.equals(metadata.getUploadedMemberId(), requesterMemberId)) {
            throw new StorageException(StorageErrorCode.FILE_USE_FORBIDDEN);
        }
    }

    private void validateUploadLifecycle(FileMetadata metadata, FileUsageRegistryStatus registryStatus) {
        boolean confirmed = registryStatus == FileUsageRegistryStatus.READY
            ? metadata.getConfirmedAt() != null
            : metadata.isConfirmedForAudit();
        if (!confirmed) {
            throw new StorageException(StorageErrorCode.FILE_UPLOAD_NOT_COMPLETED);
        }
    }

    private void validateCleanupState(FileMetadata metadata) {
        if (metadata.hasCleanupFailure()) {
            throw new StorageException(StorageErrorCode.FILE_CLEANUP_FAILED);
        }
        if (metadata.hasCleanupClaim()) {
            throw new StorageException(StorageErrorCode.FILE_CLEANUP_IN_PROGRESS);
        }
    }

    private void markAttached(Set<String> attachedFileIds, Map<String, FileMetadata> metadataById) {
        for (String fileId : new TreeSet<>(attachedFileIds)) {
            FileMetadata metadata = metadataById.get(fileId);
            metadata.markReferenced();
            saveFileMetadataPort.save(metadata);
        }
    }

    private void markDetached(Set<String> detachedFileIds, Map<String, FileMetadata> metadataById) {
        for (String fileId : new TreeSet<>(detachedFileIds)) {
            FileMetadata metadata = metadataById.get(fileId);
            if (loadFileUsagePort.countByFileId(fileId) == 0L) {
                metadata.markUnreferenced(clock.instant());
            } else {
                metadata.markReferenced();
            }
            saveFileMetadataPort.save(metadata);
        }
    }

    private void requireAllMetadata(Iterable<String> fileIds, Map<String, FileMetadata> metadataById) {
        for (String fileId : fileIds) {
            if (!metadataById.containsKey(fileId)) {
                throw new StorageException(StorageErrorCode.FILE_NOT_FOUND);
            }
        }
    }

    private Set<String> difference(Set<String> source, Set<String> excluded) {
        return source.stream()
            .filter(fileId -> !excluded.contains(fileId))
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private List<String> sortedUnion(Set<String> first, Set<String> second) {
        return Stream.concat(first.stream(), second.stream())
            .distinct()
            .sorted()
            .toList();
    }
}
