package com.umc.product.community.application.port.in.command.thread.dto;

import java.time.Instant;

import com.umc.product.community.domain.CommunityThreadMember;
import com.umc.product.community.domain.enums.CommunityThreadMemberRole;
import com.umc.product.community.domain.enums.CommunityThreadMemberState;

public record CommunityThreadMemberLifecycleInfo(
    Long threadId,
    Long memberId,
    CommunityThreadMemberRole role,
    CommunityThreadMemberState state,
    Instant joinedAt,
    Instant leftAt,
    long memberCount
) {

    public static CommunityThreadMemberLifecycleInfo from(
        CommunityThreadMember member,
        long memberCount
    ) {
        return new CommunityThreadMemberLifecycleInfo(
            member.getThreadId(),
            member.getMemberId(),
            member.getRole(),
            member.getState(),
            member.getJoinedAt(),
            member.getLeftAt(),
            memberCount
        );
    }
}
