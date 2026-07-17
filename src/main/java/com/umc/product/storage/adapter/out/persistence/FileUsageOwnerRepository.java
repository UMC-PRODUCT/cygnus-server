package com.umc.product.storage.adapter.out.persistence;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.umc.product.storage.domain.FileUsageOwner;

import jakarta.persistence.LockModeType;

public interface FileUsageOwnerRepository extends JpaRepository<FileUsageOwner, Long> {

    @Modifying(flushAutomatically = true)
    @Query(value = """
        INSERT INTO file_usage_owner (usage_namespace, resource_key, slot, created_at, updated_at)
        VALUES (:usageNamespace, :resourceKey, :slot, NOW(), NOW())
        ON CONFLICT ON CONSTRAINT uq_file_usage_owner_coordinate DO NOTHING
        """, nativeQuery = true)
    int insertIfAbsent(
        @Param("usageNamespace") String usageNamespace,
        @Param("resourceKey") String resourceKey,
        @Param("slot") String slot
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT owner
        FROM FileUsageOwner owner
        WHERE owner.usageNamespace = :usageNamespace
          AND owner.resourceKey = :resourceKey
          AND owner.slot = :slot
        """)
    Optional<FileUsageOwner> findByCoordinateForUpdate(
        @Param("usageNamespace") String usageNamespace,
        @Param("resourceKey") String resourceKey,
        @Param("slot") String slot
    );
}
