package com.umc.product.recruiting.application.port.in.query.dto;


import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.recruiting.domain.RecruitingApplication;
import com.umc.product.recruiting.domain.enums.RecruitingApplicationRegistrationStatus;
import com.umc.product.recruiting.domain.enums.RecruitingApplicationStatus;

public record RecruitingApplicationResourceInfo(
    Long applicationId,
    Long roundId,
    Long seasonId,
    RecruitingApplicationStatus status,
    RecruitingApplicationRegistrationStatus registrationStatus,
    ChallengerTrack firstChoice,
    ChallengerTrack secondChoice,
    ChallengerTrack acceptedTrack,
    boolean applicantAccess,
    boolean reviewAccess
) {

    public static RecruitingApplicationResourceInfo of(
        RecruitingApplication application,
        boolean applicantAccess,
        boolean reviewAccess
    ) {
        return new RecruitingApplicationResourceInfo(
            application.getId(),
            application.getRound().getId(),
            application.getRound().getSeason().getId(),
            application.getStatus(),
            application.getRegistrationStatus(),
            application.getFirstChoice(),
            application.getSecondChoice(),
            application.getAcceptedTrack(),
            applicantAccess,
            reviewAccess
        );
    }
}
