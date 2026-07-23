package com.umc.product.challenger.adapter.in.graphql.dto;

import java.util.List;

import com.umc.product.challenger.application.port.in.query.dto.ChallengerInfo;
import com.umc.product.challenger.application.port.in.query.dto.ChallengerPointInfo;
import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.common.domain.enums.ChallengerStatus;
import com.umc.product.common.domain.enums.ChallengerTrack;

public record ChallengerGraphQlResponse(
    Long challengerId,
    Long memberId,
    Long gisuId,
    ChallengerPart part,
    List<ChallengerTrack> tracks,
    List<ChallengerPointInfo> points,
    Double totalPoints,
    ChallengerStatus status
) {

    public static ChallengerGraphQlResponse from(ChallengerInfo info) {
        return new ChallengerGraphQlResponse(
            info.challengerId(),
            info.memberId(),
            info.gisuId(),
            info.part(),
            info.tracks(),
            info.challengerPoints() == null ? List.of() : info.challengerPoints(),
            info.totalPoints() == null ? 0.0 : info.totalPoints(),
            info.challengerStatus()
        );
    }
}
