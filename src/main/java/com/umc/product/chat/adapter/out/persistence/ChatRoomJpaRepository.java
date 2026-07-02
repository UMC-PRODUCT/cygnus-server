package com.umc.product.chat.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import com.umc.product.chat.domain.ChatRoom;

public interface ChatRoomJpaRepository extends JpaRepository<ChatRoom, Long> {
}
