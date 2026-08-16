package com.umc.product.demoday.adapter.in.web.dto.response;

import java.time.Instant;

import com.umc.product.demoday.application.port.in.query.dto.DemodayPollInfo;
import com.umc.product.demoday.domain.enums.DemodayPollStatus;

import io.swagger.v3.oas.annotations.media.Schema;

public record DemodayPollResponse(
        @Schema(description = "투표 ID", example = "1")
        Long pollId,
        @Schema(description = "투표 이름", example = "2026 2학기 데모데이 투표")
        String name,
        @Schema(description = "투표 시작 시각", example = "2026-08-16T10:00:00Z")
        Instant opensAt,
        @Schema(description = "투표 종료 시각", example = "2026-08-16T12:00:00Z")
        Instant closesAt,
        @Schema(description = "투표 상태", example = "OPEN")
        DemodayPollStatus status
) {

    public static DemodayPollResponse from(DemodayPollInfo info) {
        return new DemodayPollResponse(
                info.pollId(),
                info.name(),
                info.opensAt(),
                info.closesAt(),
                info.status()
        );
    }
}
