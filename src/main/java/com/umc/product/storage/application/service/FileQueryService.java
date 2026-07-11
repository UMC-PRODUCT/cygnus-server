package com.umc.product.storage.application.service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.storage.application.port.in.query.GetFileUseCase;
import com.umc.product.storage.application.port.in.query.dto.FileInfo;
import com.umc.product.storage.application.port.in.query.dto.FileMetadataInfo;
import com.umc.product.storage.application.port.out.LoadFileMetadataPort;
import com.umc.product.storage.application.port.out.StoragePort;
import com.umc.product.storage.domain.FileMetadata;
import com.umc.product.storage.domain.exception.StorageErrorCode;
import com.umc.product.storage.domain.exception.StorageException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FileQueryService implements GetFileUseCase {

    private static final long ACCESS_URL_DURATION_MINUTES = 60;

    private final StoragePort storagePort;
    private final LoadFileMetadataPort loadFileMetadataPort;

    @Override
    public FileInfo getById(String fileId) {
        FileMetadata metadata = loadFileMetadataPort.findByFileId(fileId)
            .orElseThrow(() -> new StorageException(StorageErrorCode.FILE_NOT_FOUND));

        // CDN을 통한 Signed URL 생성
        String fileLink = storagePort.generateAccessUrl(
            metadata.getStorageKey(),
            ACCESS_URL_DURATION_MINUTES
        );

        return FileInfo.of(metadata, fileLink);
    }

    @Override
    public Map<String, String> getFileLinks(List<String> fileIds) {
        List<FileMetadata> metadataList = loadFileMetadataPort.findByFileIds(fileIds);

        return metadataList.stream()
            .collect(Collectors.toMap(
                FileMetadata::getId,
                metadata -> storagePort.generateAccessUrl(
                    metadata.getStorageKey(),
                    ACCESS_URL_DURATION_MINUTES
                )
            ));
    }

    @Override
    public Map<String, FileInfo> findAllByIds(List<String> fileIds) {
        List<FileMetadata> metadataList = loadFileMetadataPort.findByFileIds(fileIds);

        return metadataList.stream()
            .collect(Collectors.toMap(
                FileMetadata::getId,
                metadata -> FileInfo.of(
                    metadata,
                    storagePort.generateAccessUrl(
                        metadata.getStorageKey(),
                        ACCESS_URL_DURATION_MINUTES
                    )
                )
            ));
    }

    @Override
    public List<FileMetadataInfo> batchGetUsableByIds(List<String> fileIds, Long memberId) {
        if (fileIds == null || fileIds.isEmpty()) {
            return List.of();
        }

        Set<String> uniqueFileIds = new LinkedHashSet<>(fileIds);
        if (uniqueFileIds.stream().anyMatch(fileId -> fileId == null || fileId.isBlank())) {
            throw new StorageException(StorageErrorCode.FILE_NOT_FOUND);
        }

        Map<String, FileMetadata> metadataById = loadFileMetadataPort.findByFileIds(List.copyOf(uniqueFileIds)).stream()
            .collect(Collectors.toMap(FileMetadata::getId, Function.identity()));

        if (metadataById.size() != uniqueFileIds.size()) {
            throw new StorageException(StorageErrorCode.FILE_NOT_FOUND);
        }

        return uniqueFileIds.stream()
            .map(metadataById::get)
            .map(metadata -> toUsableFileInfo(metadata, memberId))
            .toList();
    }

    @Override
    public boolean existsById(String fileId) {
        return loadFileMetadataPort.existsByFileId(fileId);
    }

    @Override
    public void throwIfNotExists(String fileId) {
        if (!existsById(fileId)) {
            throw new StorageException(StorageErrorCode.FILE_NOT_FOUND);
        }
    }

    private void validateUsable(FileMetadata metadata, Long memberId) {
        if (metadata.getUploadedMemberId() == null
            || memberId == null
            || !Objects.equals(metadata.getUploadedMemberId(), memberId)) {
            throw new StorageException(StorageErrorCode.FILE_USE_FORBIDDEN);
        }

        if (!metadata.isUploaded()) {
            throw new StorageException(StorageErrorCode.FILE_UPLOAD_NOT_COMPLETED);
        }
    }

    private FileMetadataInfo toUsableFileInfo(FileMetadata metadata, Long memberId) {
        validateUsable(metadata, memberId);
        return FileMetadataInfo.from(metadata);
    }
}
