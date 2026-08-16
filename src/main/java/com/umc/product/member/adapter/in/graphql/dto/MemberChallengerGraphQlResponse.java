package com.umc.product.member.adapter.in.graphql.dto;

import java.util.List;

import com.umc.product.challenger.application.port.in.query.dto.ChallengerBasicInfo;
import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.common.domain.enums.ChallengerStatus;
import com.umc.product.common.domain.enums.ChallengerTrack;

public record MemberChallengerGraphQlResponse(
    Long challengerId,
    Long memberId,
    Long gisuId,
    ChallengerPart part,
    List<ChallengerTrack> tracks,
    ChallengerStatus status
) {

    public static MemberChallengerGraphQlResponse from(ChallengerBasicInfo info) {
        return new MemberChallengerGraphQlResponse(
            info.challengerId(),
            info.memberId(),
            info.gisuId(),
            info.part(),
            info.tracks(),
            info.challengerStatus()
        );
    }
}
