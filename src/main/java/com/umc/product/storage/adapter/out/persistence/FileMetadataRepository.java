package com.umc.product.storage.adapter.out.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.umc.product.storage.domain.FileMetadata;

import jakarta.persistence.LockModeType;

public interface FileMetadataRepository extends JpaRepository<FileMetadata, String> {

    Optional<FileMetadata> findById(String fileId);

    List<FileMetadata> findByIdIn(List<String> fileIds);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT metadata
        FROM FileMetadata metadata
        WHERE metadata.id IN :fileIds
        ORDER BY metadata.id
        """)
    List<FileMetadata> findAllByIdInOrderByIdForUpdate(@Param("fileIds") List<String> fileIds);

    boolean existsById(String fileId);

    void deleteById(String fileId);
}
