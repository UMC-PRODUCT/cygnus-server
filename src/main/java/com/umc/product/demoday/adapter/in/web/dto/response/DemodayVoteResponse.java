package com.umc.product.demoday.adapter.in.web.dto.response;

import java.time.Instant;

import com.umc.product.demoday.application.port.in.command.dto.DemodayVoteInfo;

import io.swagger.v3.oas.annotations.media.Schema;

public record DemodayVoteResponse(
    @Schema(description = "생성된 표 ID", example = "9001")
    Long voteId,
    @Schema(description = "투표 Poll ID", example = "101")
    Long pollId,
    @Schema(description = "최종 투표한 부스")
    DemodayBoothResponse selectedBooth,
    @Schema(description = "서버에 표가 저장된 시각")
    Instant votedAt
) {

    public static DemodayVoteResponse from(DemodayVoteInfo info) {
        return new DemodayVoteResponse(
            info.voteId(),
            info.pollId(),
            DemodayBoothResponse.from(info.selectedBooth()),
            info.votedAt()
        );
    }
}
