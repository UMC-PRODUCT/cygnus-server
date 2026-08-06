package com.umc.product.member.adapter.in.graphql.dto;

import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.common.domain.enums.ChallengerStatus;
import com.umc.product.member.application.port.in.query.dto.SearchMemberItemV2Info.Participation;
import com.umc.product.member.application.port.in.query.dto.SearchMemberItemV2Info.PrimaryChallenger;

public record MemberSearchChallengerGraphQlResponse(
    Long challengerId,
    Long gisuId,
    Long generation,
    ChallengerPart part,
    ChallengerStatus challengerStatus
) {

    public static MemberSearchChallengerGraphQlResponse from(PrimaryChallenger info) {
        return new MemberSearchChallengerGraphQlResponse(
            info.challengerId(),
            info.gisuId(),
            info.generation(),
            info.part(),
            info.challengerStatus()
        );
    }

    public static MemberSearchChallengerGraphQlResponse from(Participation info) {
        return new MemberSearchChallengerGraphQlResponse(
            info.challengerId(),
            info.gisuId(),
            info.generation(),
            info.part(),
            info.challengerStatus()
        );
    }
}
