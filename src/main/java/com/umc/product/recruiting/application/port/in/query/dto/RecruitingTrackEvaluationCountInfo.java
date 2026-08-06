package com.umc.product.recruiting.application.port.in.query.dto;

import com.umc.product.common.domain.enums.ChallengerTrack;

public record RecruitingTrackEvaluationCountInfo(
    ChallengerTrack track,
    Long applicantCount,
    Long evaluatedCount
) {
}
