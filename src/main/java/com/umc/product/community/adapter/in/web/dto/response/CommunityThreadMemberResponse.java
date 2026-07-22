package com.umc.product.community.adapter.in.web.dto.response;

import static com.umc.product.community.adapter.in.web.CommunityWebNumbers.text;

import java.time.Instant;

import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.community.application.port.in.query.thread.dto.ThreadMemberInfo;
import com.umc.product.community.domain.enums.CommunityThreadMemberRole;
import com.umc.product.community.domain.enums.CommunityThreadMemberState;

public record CommunityThreadMemberResponse(
    String memberId,
    String name,
    ChallengerPart part,
    String generation,
    CommunityThreadMemberRole role,
    Instant joinedAt,
    CommunityThreadMemberState state
) {

    public static CommunityThreadMemberResponse from(ThreadMemberInfo info) {
        return new CommunityThreadMemberResponse(
            text(info.memberId()),
            info.name(),
            info.part(),
            text(info.generation()),
            info.role(),
            info.joinedAt(),
            info.state()
        );
    }
}
