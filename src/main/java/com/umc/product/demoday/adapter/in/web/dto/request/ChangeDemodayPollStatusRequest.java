package com.umc.product.demoday.adapter.in.web.dto.request;

import com.umc.product.demoday.application.port.in.command.dto.ChangeDemodayPollStatusCommand;
import com.umc.product.demoday.domain.enums.DemodayPollStatus;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "데모데이 투표 운영 상태 변경 요청")
public record ChangeDemodayPollStatusRequest(
    @Schema(
        description = "변경할 운영 상태. OPEN은 투표 시작, CLOSED는 투표 종료를 의미합니다.",
        example = "OPEN",
        allowableValues = {"OPEN", "CLOSED"}
    )
    @NotNull DemodayPollStatus status
) {
    public ChangeDemodayPollStatusCommand toCommand(
        Long pollId,
        Long memberId
    ) {
        return new ChangeDemodayPollStatusCommand(memberId, pollId, status);
    }
}
