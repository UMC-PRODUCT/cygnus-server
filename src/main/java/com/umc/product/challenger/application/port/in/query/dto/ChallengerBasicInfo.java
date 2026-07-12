package com.umc.product.challenger.application.port.in.query.dto;

import java.util.List;

import com.umc.product.challenger.domain.Challenger;
import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.common.domain.enums.ChallengerStatus;
import com.umc.product.common.domain.enums.ChallengerTrack;

/**
 * 상벌점 없이 챌린저 식별/소속 정보만 담는 경량 Info DTO입니다.
 */
public record ChallengerBasicInfo(
    Long challengerId,
    Long memberId,
    Long gisuId,
    ChallengerPart part,
    List<ChallengerTrack> tracks,
    ChallengerStatus challengerStatus
) {

    public static ChallengerBasicInfo from(Challenger challenger) {
        return new ChallengerBasicInfo(
            challenger.getId(),
            challenger.getMemberId(),
            challenger.getGisuId(),
            challenger.getPart(),
            challenger.getEffectiveTracks(),
            challenger.getStatus()
        );
    }
}
