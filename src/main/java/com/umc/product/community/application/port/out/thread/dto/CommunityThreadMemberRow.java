package com.umc.product.community.application.port.out.thread.dto;

import java.time.Instant;

import com.umc.product.community.domain.enums.CommunityThreadMemberRole;
import com.umc.product.community.domain.enums.CommunityThreadMemberState;

public record CommunityThreadMemberRow(
    Long memberId,
    CommunityThreadMemberRole role,
    CommunityThreadMemberState state,
    Instant joinedAt
) {
}
