package com.umc.product.chat.adapter.out.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.umc.product.chat.domain.ChatRoomOwnership;

public interface ChatRoomOwnershipJpaRepository extends JpaRepository<ChatRoomOwnership, Long> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
        INSERT INTO chat_room_ownership
            (room_id, namespace, owner_resource_key, slot, created_at, updated_at)
        VALUES
            (:roomId, :namespace, :ownerResourceKey, :slot, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        ON CONFLICT DO NOTHING
        """, nativeQuery = true)
    int insertIfAbsent(
        @Param("roomId") Long roomId,
        @Param("namespace") String namespace,
        @Param("ownerResourceKey") String ownerResourceKey,
        @Param("slot") String slot
    );

    @Query(value = """
        SELECT room_id
        FROM chat_room_ownership
        WHERE room_id = :roomId
        FOR UPDATE
        """, nativeQuery = true)
    Optional<Long> lockRoomIdForUpdate(@Param("roomId") Long roomId);

    @Query(value = """
        SELECT room_id
        FROM chat_room_ownership
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

    Optional<ChatRoomOwnership> findByNamespaceAndOwnerResourceKeyAndSlot(
        String namespace,
        String ownerResourceKey,
        String slot
    );

    @Query("select distinct ownership.namespace from ChatRoomOwnership ownership order by ownership.namespace")
    List<String> findDistinctNamespaces();
}
