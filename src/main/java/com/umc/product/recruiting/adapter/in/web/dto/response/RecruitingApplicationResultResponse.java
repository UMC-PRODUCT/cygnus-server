package com.umc.product.recruiting.adapter.in.web.dto.response;

import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingApplicationResultInfo;
import com.umc.product.recruiting.domain.enums.RecruitingApplicationRegistrationStatus;
import com.umc.product.recruiting.domain.enums.RecruitingApplicationStatus;

public record RecruitingApplicationResultResponse(
    Long applicationId,
    String applicationNo,
    String maskedEmail,
    ChallengerTrack track,
    RecruitingApplicationStatus status,
    RecruitingApplicationRegistrationStatus registrationStatus
) {

    public static RecruitingApplicationResultResponse from(RecruitingApplicationResultInfo info) {
        return new RecruitingApplicationResultResponse(
            info.applicationId(),
            info.applicationNo(),
            info.maskedEmail(),
            info.track(),
            info.status(),
            info.registrationStatus()
        );
    }
}
