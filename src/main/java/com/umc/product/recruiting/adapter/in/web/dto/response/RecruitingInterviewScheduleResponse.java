package com.umc.product.recruiting.adapter.in.web.dto.response;

import java.time.Instant;

import com.umc.product.recruiting.application.port.in.query.dto.RecruitingInterviewScheduleInfo;
import com.umc.product.recruiting.domain.enums.RecruitingInterviewScheduleStatus;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "면접 일정 응답")
public record RecruitingInterviewScheduleResponse(
    @Schema(description = "면접 일정 ID", example = "1") Long id,
    @Schema(description = "지원서 ID", example = "40") Long applicationId,
    @Schema(description = "가능 일정 Survey 응답 ID", example = "700") Long availabilityFormResponseId,
    @Schema(description = "면접 일정 상태", example = "CONFIRMED") RecruitingInterviewScheduleStatus status,
    @Schema(description = "면접 시작 시각") Instant startsAt,
    @Schema(description = "면접 종료 시각") Instant endsAt,
    @Schema(description = "면접 장소 또는 접속 링크", example = "온라인 회의실 A") String location
) {

    public static RecruitingInterviewScheduleResponse from(RecruitingInterviewScheduleInfo info) {
        return new RecruitingInterviewScheduleResponse(
            info.id(),
            info.applicationId(),
            info.availabilityFormResponseId(),
            info.status(),
            info.startsAt(),
            info.endsAt(),
            info.location()
        );
    }
}
