package com.umc.product.community.adapter.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.umc.product.community.domain.CommunityThreadMember;
import com.umc.product.community.domain.enums.CommunityThreadMemberState;

public interface CommunityThreadMemberRepository extends JpaRepository<CommunityThreadMember, Long> {

    Optional<CommunityThreadMember> findByThreadIdAndMemberId(Long threadId, Long memberId);

    List<CommunityThreadMember> findAllByThreadIdAndMemberIdIn(Long threadId, Set<Long> memberIds);

    List<CommunityThreadMember> findAllByThreadIdOrderByIdAsc(Long threadId);

    long countByThreadIdAndState(Long threadId, CommunityThreadMemberState state);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
        UPDATE community_thread_member
        SET role = 'ADMIN', updated_at = CURRENT_TIMESTAMP
        WHERE thread_id = :threadId
          AND member_id = :previousOwnerMemberId
          AND state = 'ACTIVE'
          AND role = 'OWNER'
        """, nativeQuery = true)
    int demoteOwner(
        @Param("threadId") Long threadId,
        @Param("previousOwnerMemberId") Long previousOwnerMemberId
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
        UPDATE community_thread_member
        SET role = 'OWNER', updated_at = CURRENT_TIMESTAMP
        WHERE thread_id = :threadId
          AND member_id = :newOwnerMemberId
          AND state = 'ACTIVE'
          AND role <> 'OWNER'
        """, nativeQuery = true)
    int promoteOwner(
        @Param("threadId") Long threadId,
        @Param("newOwnerMemberId") Long newOwnerMemberId
    );
}
