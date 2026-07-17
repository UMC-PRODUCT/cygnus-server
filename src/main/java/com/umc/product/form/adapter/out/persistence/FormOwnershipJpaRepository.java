package com.umc.product.form.adapter.out.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.umc.product.form.domain.FormOwnership;

public interface FormOwnershipJpaRepository extends JpaRepository<FormOwnership, Long> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
        INSERT INTO form_ownership
            (form_id, namespace, owner_resource_key, slot, created_at, updated_at)
        VALUES
            (:formId, :namespace, :ownerResourceKey, :slot, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        ON CONFLICT DO NOTHING
        """, nativeQuery = true)
    int insertIfAbsent(
        @Param("formId") Long formId,
        @Param("namespace") String namespace,
        @Param("ownerResourceKey") String ownerResourceKey,
        @Param("slot") String slot
    );

    @Query(value = """
        SELECT form_id
        FROM form_ownership
        WHERE form_id = :formId
        FOR UPDATE
        """, nativeQuery = true)
    Optional<Long> lockFormIdForUpdate(@Param("formId") Long formId);

    @Query(value = """
        SELECT form_id
        FROM form_ownership
        WHERE namespace = :namespace
          AND owner_resource_key = :ownerResourceKey
          AND slot = :slot
        FOR UPDATE
        """, nativeQuery = true)
    Optional<Long> lockOwnerTupleForUpdate(
        @Param("namespace") String namespace,
        @Param("ownerResourceKey") String ownerResourceKey,
        @Param("slot") String slot
    );

    Optional<FormOwnership> findByNamespaceAndOwnerResourceKeyAndSlot(
        String namespace,
        String ownerResourceKey,
        String slot
    );

    @Query("select distinct ownership.namespace from FormOwnership ownership order by ownership.namespace")
    List<String> findDistinctNamespaces();
}
