package com.umc.product.community.adapter.out.persistence;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.umc.product.community.domain.CommunityThread;

import jakarta.persistence.LockModeType;

public interface CommunityThreadRepository extends JpaRepository<CommunityThread, Long> {

    Optional<CommunityThread> findByChatRoomId(Long chatRoomId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select thread from CommunityThread thread where thread.id = :threadId")
    Optional<CommunityThread> findByIdForUpdate(@Param("threadId") Long threadId);
}
