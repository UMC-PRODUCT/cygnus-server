package com.umc.product.recruiting.application.port.in.query.dto;

import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.recruiting.domain.enums.RecruitingRoundSort;

import lombok.Builder;

@Builder
public record RecruitingRoundGroupSearchQuery(
    Long gisuId,
    Long chapterId,
    Long schoolId,
    Long seasonId,
    ChallengerTrack track,
    RecruitingRoundSort sort,
    Long requesterMemberId
) {

    public RecruitingRoundSort effectiveSort() {
        return sort == null ? RecruitingRoundSort.NEWEST : sort;
    }
}
