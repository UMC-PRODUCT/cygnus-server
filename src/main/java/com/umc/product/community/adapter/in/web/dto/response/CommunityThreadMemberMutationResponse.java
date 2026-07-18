package com.umc.product.community.adapter.in.web.dto.response;

import static com.umc.product.community.adapter.in.web.CommunityWebNumbers.text;

import com.umc.product.community.application.port.in.command.thread.dto.CommunityThreadMemberLifecycleInfo;
import com.umc.product.community.domain.enums.CommunityThreadMemberRole;
import com.umc.product.community.domain.enums.CommunityThreadMemberState;

public record CommunityThreadMemberMutationResponse(
    String threadId,
    String memberId,
    CommunityThreadMemberRole role,
    CommunityThreadMemberState state,
    String memberCount
) {

    public static CommunityThreadMemberMutationResponse from(
        CommunityThreadMemberLifecycleInfo info
    ) {
        return new CommunityThreadMemberMutationResponse(
            text(info.threadId()),
            text(info.memberId()),
            info.role(),
            info.state(),
            text(info.memberCount())
        );
    }
}
