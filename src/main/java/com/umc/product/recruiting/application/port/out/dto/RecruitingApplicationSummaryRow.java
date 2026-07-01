package com.umc.product.recruiting.application.port.out.dto;

import java.time.Instant;

import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.recruiting.domain.enums.RecruitingApplicationRegistrationStatus;
import com.umc.product.recruiting.domain.enums.RecruitingApplicationStatus;
import com.umc.product.recruiting.domain.enums.RecruitingRoundType;

public record RecruitingApplicationSummaryRow(
    Long seasonId,
    Long gisuId,
    Long schoolId,
    Long roundId,
    RecruitingRoundType roundType,
    Integer roundNo,
    Long applicationFormId,
    Long formId,
    ChallengerTrack track,
    Long applicationId,
    String applicationNo,
    String maskedEmail,
    RecruitingApplicationStatus applicationStatus,
    RecruitingApplicationRegistrationStatus registrationStatus,
    Instant submittedAt
) {
}
