package com.umc.product.recruiting.application.port.in.query.dto;

import java.time.Instant;

import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.recruiting.domain.RecruitingApplication;
import com.umc.product.recruiting.domain.enums.RecruitingApplicationRegistrationStatus;
import com.umc.product.recruiting.domain.enums.RecruitingApplicationStatus;

import lombok.Builder;

@Builder
public record RecruitingApplicationSummaryInfo(
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

    public static RecruitingApplicationSummaryInfo from(
        RecruitingApplication application,
        boolean documentEvaluatedByMe,
        boolean interviewEvaluatedByMe
    ) {
        return RecruitingApplicationSummaryInfo.builder()
            .applicationId(application.getId())
            .applicantName(application.getApplicantName())
            .email(application.getApplicantEmail())
            .applicantMemberId(application.getApplicantMemberId())
            .firstChoice(application.getFirstChoice())
            .secondChoice(application.getSecondChoice())
            .acceptedTrack(application.getAcceptedTrack())
            .status(application.getStatus())
            .registrationStatus(application.getRegistrationStatus())
            .submittedAt(application.getSubmittedAt())
            .documentEvaluatedByMe(documentEvaluatedByMe)
            .interviewEvaluatedByMe(interviewEvaluatedByMe)
            .build();
    }
}
