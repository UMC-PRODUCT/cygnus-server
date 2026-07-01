package com.umc.product.recruiting.adapter.in.web.dto.response;

import java.time.Instant;

import com.umc.product.recruiting.application.port.out.dto.RecruitingInterviewScheduleCandidate;

public record RecruitingInterviewScheduleCandidateResponse(
    Instant startsAt,
    Instant endsAt,
    Integer availableApplicantCount
) {

    public static RecruitingInterviewScheduleCandidateResponse from(RecruitingInterviewScheduleCandidate candidate) {
        return new RecruitingInterviewScheduleCandidateResponse(
            candidate.startsAt(),
            candidate.endsAt(),
            candidate.availableApplicantCount()
        );
    }
}
