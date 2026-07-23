package com.umc.product.chat.adapter.out.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.umc.product.chat.domain.ChatMessage;

public interface ChatMessageJpaRepository extends JpaRepository<ChatMessage, Long> {

    Optional<ChatMessage> findByIdAndRoomId(Long id, Long roomId);

    boolean existsByIdAndRoomId(Long id, Long roomId);

    Optional<ChatMessage> findByRoomIdAndSenderMemberIdAndClientMessageId(
        Long roomId,
        Long senderMemberId,
        UUID clientMessageId
    );
}
