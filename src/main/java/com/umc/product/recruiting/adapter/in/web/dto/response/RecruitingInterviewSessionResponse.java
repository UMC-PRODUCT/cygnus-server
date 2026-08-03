package com.umc.product.recruiting.adapter.in.web.dto.response;

import java.time.Instant;

import com.umc.product.recruiting.application.port.in.query.dto.RecruitingInterviewSessionInfo;
import com.umc.product.recruiting.domain.enums.RecruitingInterviewMode;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "운영진 면접 세션 응답")
public record RecruitingInterviewSessionResponse(
    @Schema(description = "세션 ID", example = "10") Long id,
    @Schema(description = "Round ID", example = "3") Long roundId,
    @Schema(description = "세션 이름") String name,
    @Schema(description = "세션 시작 시각") Instant startsAt,
    @Schema(description = "세션 종료 시각") Instant endsAt,
    @Schema(description = "슬롯 시간(분)", example = "15") Integer slotDurationMinutes,
    @Schema(description = "면접 방식") RecruitingInterviewMode mode,
    @Schema(description = "면접 장소 또는 접속 링크") String location
) {
    public static RecruitingInterviewSessionResponse from(RecruitingInterviewSessionInfo info) {
        return new RecruitingInterviewSessionResponse(
            info.id(), info.roundId(), info.name(), info.startsAt(), info.endsAt(),
            info.slotDurationMinutes(), info.mode(), info.location()
        );
    }
}
