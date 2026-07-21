package com.umc.product.recruiting.application.port.in.query.dto;

import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.recruiting.domain.enums.RecruitingRoundPhase;
import com.umc.product.recruiting.domain.enums.RecruitingRoundSort;

import lombok.Builder;

@Builder
public record RecruitingPublicRoundSearchQuery(
    Long gisuId,
    Long chapterId,
    Long schoolId,
    Long seasonId,
    ChallengerTrack track,
    RecruitingRoundPhase phase,
    RecruitingRoundSort sort
) {

    public RecruitingRoundPhase effectivePhase() {
        return phase == null ? RecruitingRoundPhase.OPEN : phase;
    }

    public RecruitingRoundSort effectiveSort() {
        return sort == null ? RecruitingRoundSort.NEWEST : sort;
    }
}
