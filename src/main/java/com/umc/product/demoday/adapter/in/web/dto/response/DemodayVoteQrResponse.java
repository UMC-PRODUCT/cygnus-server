package com.umc.product.demoday.adapter.in.web.dto.response;

import java.time.Instant;

import com.umc.product.demoday.application.port.in.query.dto.DemodayVoteQrInfo;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "현재 시간 구간의 INFO 투표 인증 QR 조회 결과")
public record DemodayVoteQrResponse(
    @Schema(description = "투표 행사 ID", example = "1")
    Long pollId,
    @Schema(
        description = "FE가 그대로 QR 이미지로 렌더링할 완성된 전체 문자열. 조립하지 마세요.",
        example = "https://vote.umc.it.kr/demoday/polls/1/vote-authorization#token=eyJ..."
    )
    String qrValue,
    @Schema(description = "현재 QR 유효 구간의 시작 시각", example = "2026-08-17T15:00:00Z")
    Instant generatedAt,
    @Schema(description = "INFO QR credential 만료 시각. 1시간 주기로 교체됩니다.", example = "2026-08-17T16:00:00Z")
    Instant expiresAt
) {

    public static DemodayVoteQrResponse from(DemodayVoteQrInfo info) {
        return new DemodayVoteQrResponse(info.pollId(), info.qrValue(), info.generatedAt(), info.expiresAt());
    }
}
