package com.umc.product.chat.adapter.out.persistence;

import com.umc.product.chat.domain.ChatMember;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChatMemberJpaRepository extends JpaRepository<ChatMember, Long> {

    List<ChatMember> findAllByRoomId(Long roomId);

    List<ChatMember> findAllByMemberId(Long memberId);

    Optional<ChatMember> findByRoomIdAndMemberId(Long roomId, Long memberId);

    boolean existsByRoomIdAndMemberId(Long roomId, Long memberId);

    @Modifying
    @Query("DELETE FROM ChatMember cm WHERE cm.roomId = :roomId AND cm.memberId = :memberId")
    void deleteByRoomIdAndMemberId(@Param("roomId") Long roomId, @Param("memberId") Long memberId);
}
