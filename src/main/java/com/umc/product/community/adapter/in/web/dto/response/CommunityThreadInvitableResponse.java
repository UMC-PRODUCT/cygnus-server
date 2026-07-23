package com.umc.product.community.adapter.in.web.dto.response;

import static com.umc.product.community.adapter.in.web.CommunityWebNumbers.text;

import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.community.application.port.in.query.thread.dto.ThreadInvitableInfo;

import io.swagger.v3.oas.annotations.media.Schema;

public record CommunityThreadInvitableResponse(
    String memberId,
    @Schema(description = "최신 Challenger ID. Challenger 이력이 없으면 null", nullable = true)
    String challengerId,
    String name,
    @Schema(description = "최신 Challenger 파트. Challenger 이력이 없으면 null", nullable = true)
    ChallengerPart part,
    @Schema(description = "최신 Challenger 기수. Challenger 이력이 없으면 null", nullable = true)
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
