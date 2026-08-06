package com.umc.product.recruiting.adapter.in.web.dto.request;

import java.time.Instant;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

@Schema(description = "지원자의 면접 가능 시간 제출 요청")
public record SubmitRecruitingInterviewAvailabilityRequest(
    @Schema(
        description = "ISO-8601 UTC 면접 가능 시간 목록",
        format = "date-time",
        example = "[\"2026-08-12T10:00:00Z\", \"2026-08-12T10:15:00Z\"]"
    )
    @NotEmpty(message = "면접 가능 시간은 1개 이상이어야 합니다.") List<@NotNull(message = "면접 가능 시간은 null일 수 없습니다.") Instant> times
) {

    public SubmitRecruitingInterviewAvailabilityRequest {
        times = times == null ? null : List.copyOf(times);
    }
}
