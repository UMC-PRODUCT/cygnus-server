package com.umc.product.chat.adapter.out.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.umc.product.chat.domain.ChatMessage;

public interface ChatMessageJpaRepository extends JpaRepository<ChatMessage, Long> {

    Optional<ChatMessage> findByIdAndRoomId(Long id, Long roomId);

    boolean existsByIdAndRoomId(Long id, Long roomId);

    @Query("select message.id from ChatMessage message where message.roomId = :roomId order by message.id")
    List<Long> findIdsByRoomId(@Param("roomId") Long roomId);
}
