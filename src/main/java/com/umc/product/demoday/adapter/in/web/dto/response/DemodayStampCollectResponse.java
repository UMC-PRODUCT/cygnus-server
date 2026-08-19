package com.umc.product.demoday.adapter.in.web.dto.response;

import java.time.Instant;

import com.umc.product.demoday.application.port.in.command.dto.DemodayStampCollectInfo;

import io.swagger.v3.oas.annotations.media.Schema;

public record DemodayStampCollectResponse(
        @Schema(description = "적립된(또는 재스캔된) 스탬프")
        DemodayStampResponse stamp,
        @Schema(description = "적립한 스탬프 수", example = "4")
        int stampCount,
        @Schema(description = "투표 페이지 진입에 필요한 스탬프 수", example = "6")
        int requiredStampCount,
        @Schema(description = "다음 스탬프 적립 가능 시각. 여섯 번째 적립 후에는 null", nullable = true)
        Instant nextStampAvailableAt,
        @Schema(description = "투표 페이지 진입 가능 여부", example = "false")
        boolean canEnterVotePage
) {

    public static DemodayStampCollectResponse from(DemodayStampCollectInfo info) {
        return new DemodayStampCollectResponse(
                DemodayStampResponse.from(info.stamp()),
                info.stampCount(),
                info.requiredStampCount(),
                info.nextStampAvailableAt(),
                info.canEnterVotePage()
        );
    }
}
