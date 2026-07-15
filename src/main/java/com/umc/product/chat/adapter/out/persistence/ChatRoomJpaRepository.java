package com.umc.product.chat.adapter.out.persistence;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.umc.product.chat.domain.ChatRoom;

import jakarta.persistence.LockModeType;

public interface ChatRoomJpaRepository extends JpaRepository<ChatRoom, Long> {

    /**
     * 방 row 에 PESSIMISTIC_WRITE 락을 걸어 조회한다.
     * <p>
     * 같은 방에 대한 동시 메시지 전송을 직렬화하기 위한 락 획득용. 이 락을 message insert 이전에 잡으면,
     * 같은 방 안에서 message id 배정 순서가 commit 순서와 일치하게 되어 읽음 watermark 가 안전해진다.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from ChatRoom r where r.id = :roomId")
    Optional<ChatRoom> findByIdForUpdate(@Param("roomId") Long roomId);
}
