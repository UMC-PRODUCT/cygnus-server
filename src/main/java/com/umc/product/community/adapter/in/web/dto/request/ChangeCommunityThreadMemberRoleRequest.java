package com.umc.product.community.adapter.in.web.dto.request;

import com.umc.product.community.application.port.in.command.thread.dto.ChangeCommunityThreadMemberRoleCommand;
import com.umc.product.community.domain.enums.CommunityThreadMemberRole;

import jakarta.validation.constraints.NotNull;

public record ChangeCommunityThreadMemberRoleRequest(
    @NotNull CommunityThreadMemberRole role
) {

    public ChangeCommunityThreadMemberRoleCommand toCommand(
        Long threadId,
        Long actorMemberId,
        Long memberId
    ) {
        return new ChangeCommunityThreadMemberRoleCommand(threadId, actorMemberId, memberId, role);
    }
}
