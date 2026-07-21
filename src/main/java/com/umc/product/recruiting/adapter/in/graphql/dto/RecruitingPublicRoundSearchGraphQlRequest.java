package com.umc.product.recruiting.adapter.in.graphql.dto;

import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingPublicRoundSearchQuery;
import com.umc.product.recruiting.domain.enums.RecruitingRoundPhase;
import com.umc.product.recruiting.domain.enums.RecruitingRoundSort;

public record RecruitingPublicRoundSearchGraphQlRequest(
    Long gisuId,
    Long chapterId,
    Long schoolId,
    Long seasonId,
    ChallengerTrack track,
    RecruitingRoundPhase phase,
    RecruitingRoundSort sort
) {

    public RecruitingPublicRoundSearchQuery toQuery() {
        return RecruitingPublicRoundSearchQuery.builder()
            .gisuId(gisuId)
            .chapterId(chapterId)
            .schoolId(schoolId)
            .seasonId(seasonId)
            .track(track)
            .phase(phase)
            .sort(sort)
            .build();
    }
}
