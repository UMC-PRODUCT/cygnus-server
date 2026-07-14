package com.umc.product.challenger.application.port.in.query.dto;

import com.umc.product.challenger.domain.Challenger;
import com.umc.product.common.domain.enums.ChallengerPart;

public record ChallengerPolicyInfo(
    Long challengerId,
    Long memberId,
    Long gisuId,
    ChallengerPart part
) {

    public static ChallengerPolicyInfo from(Challenger challenger) {
        return new ChallengerPolicyInfo(
            challenger.getId(),
            challenger.getMemberId(),
            challenger.getGisuId(),
            challenger.getPart()
        );
    }
}
