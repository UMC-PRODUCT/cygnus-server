package com.umc.product.recruiting.adapter.in.graphql.dto;

import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingApplicationInfo;
import com.umc.product.recruiting.domain.enums.RecruitingApplicationRegistrationStatus;
import com.umc.product.recruiting.domain.enums.RecruitingApplicationStatus;

public record RecruitingApplicationGraphQlResponse(
    Long applicationId,
    RecruitingApplicationStatus status,
    RecruitingApplicationRegistrationStatus registrationStatus,
    ChallengerTrack firstChoice,
    ChallengerTrack secondChoice,
    ChallengerTrack acceptedTrack
) {

    public static RecruitingApplicationGraphQlResponse from(RecruitingApplicationInfo info) {
        return new RecruitingApplicationGraphQlResponse(
            info.applicationId(),
            info.status(),
            info.registrationStatus(),
            info.firstChoice(),
            info.secondChoice(),
            info.acceptedTrack()
        );
    }
}
