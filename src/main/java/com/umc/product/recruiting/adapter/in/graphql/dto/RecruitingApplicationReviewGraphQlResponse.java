package com.umc.product.recruiting.adapter.in.graphql.dto;

import java.time.Instant;

import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingApplicationSummaryInfo;
import com.umc.product.recruiting.domain.enums.RecruitingApplicationRegistrationStatus;
import com.umc.product.recruiting.domain.enums.RecruitingApplicationStatus;

public record RecruitingApplicationReviewGraphQlResponse(
    Long applicationId,
    String applicantName,
    String email,
    Long applicantMemberId,
    ChallengerTrack firstChoice,
    ChallengerTrack secondChoice,
    ChallengerTrack acceptedTrack,
    RecruitingApplicationStatus status,
    RecruitingApplicationRegistrationStatus registrationStatus,
    Instant submittedAt,
    boolean documentEvaluatedByMe,
    boolean interviewEvaluatedByMe
) {

    public static RecruitingApplicationReviewGraphQlResponse from(RecruitingApplicationSummaryInfo info) {
        return new RecruitingApplicationReviewGraphQlResponse(
            info.applicationId(),
            info.applicantName(),
            info.email(),
            info.applicantMemberId(),
            info.firstChoice(),
            info.secondChoice(),
            info.acceptedTrack(),
            info.status(),
            info.registrationStatus(),
            info.submittedAt(),
            info.documentEvaluatedByMe(),
            info.interviewEvaluatedByMe()
        );
    }
}
