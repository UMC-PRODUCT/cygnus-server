package com.umc.product.storage.adapter.out.persistence;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.umc.product.storage.application.port.out.LoadFileUsagePort;
import com.umc.product.storage.application.port.out.LockFileUsageOwnerPort;
import com.umc.product.storage.application.port.out.SaveFileUsagePort;
import com.umc.product.storage.domain.FileUsage;
import com.umc.product.storage.domain.FileUsageCoordinate;
import com.umc.product.storage.domain.FileUsageOwner;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class FileUsagePersistenceAdapter
    implements LockFileUsageOwnerPort, LoadFileUsagePort, SaveFileUsagePort {

    private final FileUsageOwnerRepository fileUsageOwnerRepository;
    private final FileUsageRepository fileUsageRepository;

    @Override
    public FileUsageOwner lockOrCreateOwner(FileUsageCoordinate coordinate) {
        if (coordinate == null) {
            throw new IllegalArgumentException("파일 usage owner 좌표는 필수입니다.");
        }
        fileUsageOwnerRepository.insertIfAbsent(
            coordinate.usageNamespace(),
            coordinate.resourceKey(),
            coordinate.slot()
        );
        return fileUsageOwnerRepository.findByCoordinateForUpdate(
            coordinate.usageNamespace(),
            coordinate.resourceKey(),
            coordinate.slot()
        ).orElseThrow(() -> new IllegalStateException("파일 usage owner lock을 획득하지 못했습니다."));
    }

    @Override
    public Set<String> findFileIdsByOwnerId(Long ownerId) {
        LinkedHashSet<String> snapshot = fileUsageRepository.findAllByOwnerIdOrderByFileIdAsc(ownerId).stream()
            .map(FileUsage::getFileId)
            .collect(Collectors.toCollection(LinkedHashSet::new));
        return Collections.unmodifiableSet(snapshot);
    }

    @Override
    public long countByFileId(String fileId) {
        return fileUsageRepository.countByFileId(fileId);
    }

    @Override
    public void addUsages(Long ownerId, Set<String> fileIds) {
        if (fileIds == null) {
            throw new IllegalArgumentException("추가할 파일 ID 집합은 필수입니다.");
        }
        List<FileUsage> usages = fileIds.stream()
            .sorted()
            .map(fileId -> FileUsage.of(ownerId, fileId))
            .toList();
        fileUsageRepository.saveAll(usages);
    }

    @Override
    public void removeUsages(Long ownerId, Set<String> fileIds) {
        if (fileIds == null) {
            throw new IllegalArgumentException("제거할 파일 ID 집합은 필수입니다.");
        }
        if (fileIds.isEmpty()) {
            return;
        }
        fileUsageRepository.deleteAllByOwnerIdAndFileIdIn(ownerId, fileIds);
    }
}
