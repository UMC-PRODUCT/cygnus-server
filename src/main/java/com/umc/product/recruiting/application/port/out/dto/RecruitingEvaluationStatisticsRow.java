package com.umc.product.recruiting.application.port.out.dto;

import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.recruiting.domain.enums.RecruitingApplicationStatus;

public record RecruitingEvaluationStatisticsRow(
    Long schoolId,
    ChallengerTrack track,
    RecruitingApplicationStatus status,
    Long count
) {
}
