package com.umc.product.storage.application.service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.storage.application.port.out.LoadFileUsagePort;
import com.umc.product.storage.application.port.out.LockFileMetadataPort;
import com.umc.product.storage.application.port.out.SaveFileMetadataPort;
import com.umc.product.storage.domain.FileMetadata;
import com.umc.product.storage.domain.exception.StorageErrorCode;
import com.umc.product.storage.domain.exception.StorageException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GeneratedFileMetadataService {

    private final LockFileMetadataPort lockFileMetadataPort;
    private final LoadFileUsagePort loadFileUsagePort;
    private final SaveFileMetadataPort saveFileMetadataPort;
    private final Clock clock;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void createPending(FileMetadata metadata) {
        saveFileMetadataPort.save(metadata);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void confirmGenerated(String fileId) {
        FileMetadata metadata = lockFileMetadataPort.lockAllByFileIds(List.of(fileId)).stream()
            .findFirst()
            .orElseThrow(() -> new StorageException(StorageErrorCode.FILE_NOT_FOUND));
        if (metadata.isUploaded()) {
            throw new StorageException(StorageErrorCode.FILE_ALREADY_UPLOADED);
        }

        Instant confirmedAt = clock.instant();
        metadata.confirmUploaded(metadata.getFileSize(), metadata.getContentType(), confirmedAt);
        if (loadFileUsagePort.countByFileId(fileId) == 0L) {
            metadata.markUnreferenced(confirmedAt);
        } else {
            metadata.markReferenced();
        }
        saveFileMetadataPort.save(metadata);
    }
}
