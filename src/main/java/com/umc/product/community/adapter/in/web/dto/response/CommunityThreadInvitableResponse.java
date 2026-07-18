package com.umc.product.community.adapter.in.web.dto.response;

import static com.umc.product.community.adapter.in.web.CommunityWebNumbers.text;

import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.community.application.port.in.query.thread.dto.ThreadInvitableInfo;

public record CommunityThreadInvitableResponse(
    String memberId,
    String challengerId,
    String name,
    ChallengerPart part,
    String generation
) {

    public static CommunityThreadInvitableResponse from(ThreadInvitableInfo info) {
        return new CommunityThreadInvitableResponse(
            text(info.memberId()),
            text(info.challengerId()),
            info.name(),
            info.part(),
            text(info.generation())
        );
    }
}
