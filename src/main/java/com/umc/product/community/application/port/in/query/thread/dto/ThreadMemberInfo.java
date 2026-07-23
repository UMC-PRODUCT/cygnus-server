package com.umc.product.community.application.port.in.query.thread.dto;

import java.time.Instant;

import org.springframework.lang.Nullable;

import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.community.domain.enums.CommunityThreadMemberRole;
import com.umc.product.community.domain.enums.CommunityThreadMemberState;

public record ThreadMemberInfo(
    Long memberId,
    String name,
    @Nullable ChallengerPart part,
    @Nullable Long generation,
    CommunityThreadMemberRole role,
    Instant joinedAt,
    CommunityThreadMemberState state
) {
}
