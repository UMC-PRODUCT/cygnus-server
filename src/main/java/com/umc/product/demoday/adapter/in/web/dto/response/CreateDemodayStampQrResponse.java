package com.umc.product.demoday.adapter.in.web.dto.response;

import java.time.Instant;

import com.umc.product.demoday.application.port.in.command.dto.StampCredentialInfo;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "데모데이 부스 스탬프 QR 생성 또는 재발급 결과")
public record CreateDemodayStampQrResponse(
    @Schema(
            description = "스탬프 적립 화면으로 이동하는 QR URL",
            example = "https://vote.example.com/demoday/polls/1/stamp#credential=AbC123_-"
    )
    String qrValue,
    @Schema(description = "스탬프 QR 생성 또는 재발급 시각", example = "2026-08-17T10:00:00Z")
    Instant generatedAt
) {

    public static CreateDemodayStampQrResponse from(StampCredentialInfo stampCredentialInfo) {
        return new CreateDemodayStampQrResponse(
            stampCredentialInfo.qrValue(),
            stampCredentialInfo.generatedAt()
        );
    }
}
