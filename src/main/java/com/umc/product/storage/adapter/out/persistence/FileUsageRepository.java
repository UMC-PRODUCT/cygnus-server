package com.umc.product.storage.adapter.out.persistence;

import java.util.List;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.umc.product.storage.domain.FileUsage;

public interface FileUsageRepository extends JpaRepository<FileUsage, Long> {

    List<FileUsage> findAllByOwnerIdOrderByFileIdAsc(Long ownerId);

    long countByFileId(String fileId);

    @Modifying(flushAutomatically = true)
    @Query("""
        DELETE FROM FileUsage usage
        WHERE usage.ownerId = :ownerId
          AND usage.fileId IN :fileIds
        """)
    int deleteAllByOwnerIdAndFileIdIn(
        @Param("ownerId") Long ownerId,
        @Param("fileIds") Set<String> fileIds
    );
}
