package com.umc.product.demoday.adapter.in.web.dto.response;

import java.time.Instant;

import com.umc.product.demoday.application.port.in.query.dto.DemodayVoteReceiptInfo;

import io.swagger.v3.oas.annotations.media.Schema;

public record DemodayVoteReceiptResponse(
    @Schema(description = "유효한 표 ID", example = "9001")
    Long voteId,
    @Schema(description = "최종 투표한 부스")
    DemodayBoothResponse selectedBooth,
    @Schema(description = "서버에 표가 저장된 시각")
    Instant votedAt
) {

    public static DemodayVoteReceiptResponse from(DemodayVoteReceiptInfo info) {
        return new DemodayVoteReceiptResponse(
            info.voteId(),
            DemodayBoothResponse.from(info.selectedBooth()),
            info.votedAt()
        );
    }
}
