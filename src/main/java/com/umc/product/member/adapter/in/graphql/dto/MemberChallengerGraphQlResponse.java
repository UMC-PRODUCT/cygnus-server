package com.umc.product.member.adapter.in.graphql.dto;

import java.util.List;

import com.umc.product.challenger.application.port.in.query.dto.ChallengerBasicInfo;
import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.common.domain.enums.ChallengerStatus;
import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.global.graphql.relay.GlobalId;
import com.umc.product.global.graphql.relay.GlobalIdTypes;

/**
 * 스키마 {@code MemberChallenger} 타입 응답. Node가 아니며 challengerId는 Challenger 전역 ID로 인코딩한다.
 * rawChallengerId/gisuId는 배치 로딩용 raw ID로 스키마에 노출하지 않는다.
 */
public record MemberChallengerGraphQlResponse(
    Long rawChallengerId,
    Long gisuId,
    ChallengerPart part,
    List<ChallengerTrack> tracks,
    ChallengerStatus status
) {

    public static MemberChallengerGraphQlResponse from(ChallengerBasicInfo info) {
        return new MemberChallengerGraphQlResponse(
            info.challengerId(),
            info.gisuId(),
            info.part(),
            info.tracks(),
            info.challengerStatus()
        );
    }

    public String challengerId() {
        return GlobalId.encode(GlobalIdTypes.CHALLENGER, rawChallengerId);
    }
}
