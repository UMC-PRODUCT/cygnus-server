package com.umc.product.chat.adapter.out.persistence;

import com.umc.product.chat.domain.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatMessageJpaRepository extends JpaRepository<ChatMessage, Long> {
}
