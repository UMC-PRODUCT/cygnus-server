package com.umc.product.community.adapter.in.web.dto.response;

import static com.umc.product.community.adapter.in.web.CommunityWebNumbers.text;

import java.util.List;

import com.umc.product.community.application.port.in.query.thread.dto.ThreadMemberInfo;

public record CommunityThreadInvitationResponse(
    List<CommunityThreadMemberResponse> invitedMembers,
    String memberCount
) {

    public static CommunityThreadInvitationResponse of(
        List<ThreadMemberInfo> invitedMembers,
        long memberCount
    ) {
        return new CommunityThreadInvitationResponse(
            invitedMembers.stream().map(CommunityThreadMemberResponse::from).toList(),
            text(memberCount)
        );
    }
}
