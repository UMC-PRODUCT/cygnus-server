package com.umc.product.demoday.adapter.in.web.dto.response;

import java.time.Instant;

import com.umc.product.demoday.application.port.in.command.dto.DemodayVoteStatusInfo;
import com.umc.product.demoday.domain.enums.DemodayVoteStatus;

import io.swagger.v3.oas.annotations.media.Schema;

public record DemodayVoteStatusResponse(
    @Schema(description = "표 ID", example = "9000")
    Long voteId,
    @Schema(description = "변경 후 표 상태", example = "REVOKED")
    DemodayVoteStatus status,
    @Schema(description = "무효 처리 시각. 무효 해제 후에는 null입니다.", nullable = true)
    Instant revokedAt
) {

    public static DemodayVoteStatusResponse from(DemodayVoteStatusInfo info) {
        return new DemodayVoteStatusResponse(info.voteId(), info.status(), info.revokedAt());
    }
}
