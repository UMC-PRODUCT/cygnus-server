package com.umc.product.community.application.port.out.thread;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.umc.product.community.domain.CommunityThreadMember;

public interface LoadCommunityThreadMemberPort {

    Optional<CommunityThreadMember> findByThreadIdAndMemberId(Long threadId, Long memberId);

    Optional<CommunityThreadMember> findByThreadIdAndMemberIdForUpdate(Long threadId, Long memberId);

    List<CommunityThreadMember> listByThreadIdAndMemberIds(Long threadId, Set<Long> memberIds);

    List<CommunityThreadMember> listByThreadId(Long threadId);

    long countActiveByThreadId(Long threadId);
}
