package com.umc.product.demoday.adapter.in.web.dto.response;

import java.time.Instant;

import com.umc.product.demoday.application.port.in.query.dto.DemodayStampInfo;

import io.swagger.v3.oas.annotations.media.Schema;

public record DemodayStampResponse(
        @Schema(description = "부스 ID", example = "1")
        Long boothId,
        @Schema(description = "스탬프 적립 시각", example = "2026-08-16T10:30:00Z")
        Instant collectedAt
) {

    public static DemodayStampResponse from(DemodayStampInfo info) {
        return new DemodayStampResponse(info.boothId(), info.collectedAt());
    }
}
