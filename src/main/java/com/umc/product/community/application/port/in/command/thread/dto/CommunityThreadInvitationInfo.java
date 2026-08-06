package com.umc.product.community.application.port.in.command.thread.dto;

import java.util.List;

public record CommunityThreadInvitationInfo(
    Long threadId,
    List<CommunityThreadMemberLifecycleInfo> invitedMembers,
    long memberCount
) {

    public CommunityThreadInvitationInfo {
        invitedMembers = List.copyOf(invitedMembers);
    }
}
