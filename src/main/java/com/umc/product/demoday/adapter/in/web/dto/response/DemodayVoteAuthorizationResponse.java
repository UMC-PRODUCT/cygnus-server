package com.umc.product.demoday.adapter.in.web.dto.response;

import java.time.Instant;

import com.umc.product.demoday.application.port.in.command.dto.DemodayVoteAuthorizationInfo;

import io.swagger.v3.oas.annotations.media.Schema;

public record DemodayVoteAuthorizationResponse(
    @Schema(description = "최종 투표에 사용할 opaque token. 성공·만료·화면 이탈 시 폐기합니다.")
    String voteAuthorizationToken,
    @Schema(description = "투표 권한에 결합된 선택 부스")
    DemodayBoothResponse selectedBooth,
    @Schema(description = "투표 권한 만료 시각. 발급 시점부터 5분 뒤입니다.")
    Instant expiresAt
) {

    public static DemodayVoteAuthorizationResponse from(DemodayVoteAuthorizationInfo info) {
        return new DemodayVoteAuthorizationResponse(
            info.voteAuthorizationToken(),
            DemodayBoothResponse.from(info.selectedBooth()),
            info.expiresAt()
        );
    }
}
