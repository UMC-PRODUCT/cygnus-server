package com.umc.product.demoday.adapter.in.web.dto.request;

import com.umc.product.demoday.application.port.in.command.dto.CreateDemodayVoteAuthorizationCommand;
import com.umc.product.demoday.application.port.in.query.participant.DemodayParticipant;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateDemodayVoteAuthorizationRequest(
    @Schema(description = "사용자가 미리 선택한 프로젝트 부스 ID. 외부 부스는 투표할 수 없습니다.", example = "12")
    @NotNull Long boothId,
    @Schema(description = "INFO 부스 QR에서 추출한 서명 token", example = "signed-info-qr-token")
    @NotBlank String qrToken
) {

    public CreateDemodayVoteAuthorizationCommand toCommand(Long pollId, DemodayParticipant participant) {
        return new CreateDemodayVoteAuthorizationCommand(pollId, boothId, qrToken, participant);
    }
}
