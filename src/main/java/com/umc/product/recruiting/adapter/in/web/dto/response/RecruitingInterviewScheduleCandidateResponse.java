package com.umc.product.recruiting.adapter.in.web.dto.response;

import java.time.Instant;

import com.umc.product.recruiting.application.port.out.dto.RecruitingInterviewScheduleCandidate;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "면접 일정 후보 응답")
public record RecruitingInterviewScheduleCandidateResponse(
    @Schema(description = "면접 후보 시작 일시", example = "2026-07-10T10:00:00Z")
    Instant startsAt,
    @Schema(description = "면접 후보 종료 일시", example = "2026-07-10T10:30:00Z")
    Instant endsAt,
    @Schema(description = "해당 시간에 가능한 지원자 수", example = "8")
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
