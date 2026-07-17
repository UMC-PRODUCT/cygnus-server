package com.umc.product.storage.adapter.out.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.storage.application.port.out.LoadFileMetadataPort;
import com.umc.product.storage.application.port.out.LockFileMetadataPort;
import com.umc.product.storage.application.port.out.SaveFileMetadataPort;
import com.umc.product.storage.domain.FileMetadata;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class FileMetadataPersistenceAdapter
    implements LoadFileMetadataPort, SaveFileMetadataPort, LockFileMetadataPort {

    private final FileMetadataRepository fileMetadataRepository;

    @Override
    public Optional<FileMetadata> findByFileId(String fileId) {
        return fileMetadataRepository.findById(fileId);
    }

    @Override
    public List<FileMetadata> findByFileIds(List<String> fileIds) {
        return fileMetadataRepository.findByIdIn(fileIds);
    }

    @Override
    public boolean existsByFileId(String fileId) {
        return fileMetadataRepository.existsById(fileId);
    }

    @Override
    public List<FileMetadata> lockAllByFileIds(List<String> fileIds) {
        if (fileIds == null) {
            throw new IllegalArgumentException("lock할 파일 ID 목록은 필수입니다.");
        }
        List<String> sortedFileIds = fileIds.stream()
            .distinct()
            .sorted()
            .toList();
        if (sortedFileIds.isEmpty()) {
            return List.of();
        }
        return fileMetadataRepository.findAllByIdInOrderByIdForUpdate(sortedFileIds);
    }

    @Override
    public FileMetadata save(FileMetadata fileMetadata) {
        return fileMetadataRepository.save(fileMetadata);
    }

    @Override
    @Transactional
    public void deleteByFileId(String fileId) {
        fileMetadataRepository.deleteById(fileId);
    }
}
