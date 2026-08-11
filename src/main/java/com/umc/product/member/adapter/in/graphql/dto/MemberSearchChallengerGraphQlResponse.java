package com.umc.product.member.adapter.in.graphql.dto;

import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.common.domain.enums.ChallengerStatus;
import com.umc.product.global.graphql.relay.GlobalId;
import com.umc.product.global.graphql.relay.GlobalIdTypes;
import com.umc.product.member.application.port.in.query.dto.SearchMemberItemV2Info.Participation;
import com.umc.product.member.application.port.in.query.dto.SearchMemberItemV2Info.PrimaryChallenger;

/**
 * 스키마 {@code MemberSearchChallenger} 타입 응답. Node가 아니며 challengerId는 Challenger 전역 ID로 인코딩한다.
 * rawChallengerId/gisuId는 배치 로딩용 raw ID로 스키마에 노출하지 않는다.
 */
public record MemberSearchChallengerGraphQlResponse(
    Long rawChallengerId,
    Long gisuId,
    Integer generation,
    ChallengerPart part,
    ChallengerStatus challengerStatus
) {

    public static MemberSearchChallengerGraphQlResponse from(PrimaryChallenger info) {
        return new MemberSearchChallengerGraphQlResponse(
            info.challengerId(),
            info.gisuId(),
            toGeneration(info.generation()),
            info.part(),
            info.challengerStatus()
        );
    }

    public static MemberSearchChallengerGraphQlResponse from(Participation info) {
        return new MemberSearchChallengerGraphQlResponse(
            info.challengerId(),
            info.gisuId(),
            toGeneration(info.generation()),
            info.part(),
            info.challengerStatus()
        );
    }

    public String challengerId() {
        return GlobalId.encode(GlobalIdTypes.CHALLENGER, rawChallengerId);
    }

    private static Integer toGeneration(Long generation) {
        return generation == null ? null : Math.toIntExact(generation);
    }
}
