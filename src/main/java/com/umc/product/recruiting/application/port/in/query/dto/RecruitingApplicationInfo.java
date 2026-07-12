package com.umc.product.recruiting.application.port.in.query.dto;

import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.recruiting.domain.RecruitingApplication;
import com.umc.product.recruiting.domain.enums.RecruitingApplicationRegistrationStatus;
import com.umc.product.recruiting.domain.enums.RecruitingApplicationStatus;

public record RecruitingApplicationInfo(
    Long applicationId,
    RecruitingApplicationStatus status,
    RecruitingApplicationRegistrationStatus registrationStatus,
    ChallengerTrack firstChoice,
    ChallengerTrack secondChoice,
    ChallengerTrack acceptedTrack
) {

    public static RecruitingApplicationInfo of(Long applicationId, RecruitingApplicationStatus status) {
        return new RecruitingApplicationInfo(
            applicationId,
            status,
            RecruitingApplicationRegistrationStatus.NOT_READY,
            null,
            null,
            null
        );
    }

    public static RecruitingApplicationInfo from(RecruitingApplication application) {
        return new RecruitingApplicationInfo(
            application.getId(),
            application.getStatus(),
            application.getRegistrationStatus(),
            application.getFirstChoice(),
            application.getSecondChoice(),
            application.getAcceptedTrack()
        );
    }
}
