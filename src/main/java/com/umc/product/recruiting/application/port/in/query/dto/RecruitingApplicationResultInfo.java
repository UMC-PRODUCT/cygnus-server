package com.umc.product.recruiting.application.port.in.query.dto;

import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.recruiting.domain.RecruitingApplication;
import com.umc.product.recruiting.domain.enums.RecruitingApplicationRegistrationStatus;
import com.umc.product.recruiting.domain.enums.RecruitingApplicationStatus;

import lombok.Builder;

@Builder
public record RecruitingApplicationResultInfo(
    Long applicationId,
    String applicationNo,
    String maskedEmail,
    ChallengerTrack track,
    RecruitingApplicationStatus status,
    RecruitingApplicationRegistrationStatus registrationStatus
) {

    public static RecruitingApplicationResultInfo from(RecruitingApplication application) {
        return RecruitingApplicationResultInfo.builder()
            .applicationId(application.getId())
            .applicationNo(application.getApplicationNo())
            .maskedEmail(application.getMaskedEmail())
            .track(application.getApplicationForm().getTrack())
            .status(application.getStatus())
            .registrationStatus(application.getRegistrationStatus())
            .build();
    }
}
