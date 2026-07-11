package com.umc.product.chat.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import com.umc.product.chat.domain.ChatMessage;

public interface ChatMessageJpaRepository extends JpaRepository<ChatMessage, Long> {

    boolean existsByIdAndRoomId(Long id, Long roomId);
}
