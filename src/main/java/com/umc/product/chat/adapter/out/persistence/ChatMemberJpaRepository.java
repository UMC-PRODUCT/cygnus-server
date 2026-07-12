package com.umc.product.chat.adapter.out.persistence;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.umc.product.chat.domain.ChatMember;

public interface ChatMemberJpaRepository extends JpaRepository<ChatMember, Long> {

    List<ChatMember> findAllByRoomId(Long roomId);

    List<ChatMember> findAllByMemberId(Long memberId);

    List<ChatMember> findAllByMemberIdAndRoomIdIn(Long memberId, Collection<Long> roomIds);

    Optional<ChatMember> findByRoomIdAndMemberId(Long roomId, Long memberId);

    boolean existsByRoomIdAndMemberId(Long roomId, Long memberId);

    @Modifying
    @Query("DELETE FROM ChatMember cm WHERE cm.roomId = :roomId AND cm.memberId = :memberId")
    void deleteByRoomIdAndMemberId(@Param("roomId") Long roomId, @Param("memberId") Long memberId);

    /**
     * 읽음 위치를 candidate 로 단조 증가시킨다(원자적). 이미 더 큰 값이면 그대로 둔다.
     *
     * @return 갱신된 row 수(멤버가 아니면 0)
     */
    @Modifying
    @Query(value = """
        UPDATE chat_member
           SET last_read_message_id = GREATEST(COALESCE(last_read_message_id, 0), :candidate),
               updated_at = now()
         WHERE room_id = :roomId AND member_id = :memberId
        """, nativeQuery = true)
    int bumpLastReadMessageId(@Param("roomId") Long roomId,
                              @Param("memberId") Long memberId,
                              @Param("candidate") long candidate);
}
