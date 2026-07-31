package com.umc.product.recruiting.adapter.in.web.dto.response;

import java.time.Instant;

import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingApplicationSummaryInfo;
import com.umc.product.recruiting.domain.enums.RecruitingApplicationRegistrationStatus;
import com.umc.product.recruiting.domain.enums.RecruitingApplicationStatus;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "평가용 지원서 요약")
public record RecruitingApplicationSummaryResponse(
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

    public static RecruitingApplicationSummaryResponse from(RecruitingApplicationSummaryInfo info) {
        return new RecruitingApplicationSummaryResponse(
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
