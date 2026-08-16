package com.umc.product.demoday.adapter.in.web.dto.request;

import java.time.Instant;

import com.umc.product.demoday.application.port.in.command.dto.CreateDemodayPollCommand;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "데모데이 투표 생성 요청")
public record CreateDemodayPollRequest(
    @Schema(description = "투표를 생성할 기수 ID", example = "8")
    @NotNull Long gisuId,

    @Schema(description = "투표 이름", example = "8기 데모데이 인기상 투표")
    @NotBlank String name,

    @Schema(
        description = "실제 투표가 가능한 시작 시각. 상태를 자동으로 OPEN으로 변경하지는 않습니다.",
        example = "2026-08-20T09:00:00Z"
    )
    @NotNull Instant opensAt,

    @Schema(
        description = "실제 투표가 가능한 종료 시각. opensAt보다 뒤여야 하며 상태를 자동으로 CLOSED로 변경하지는 않습니다.",
        example = "2026-08-20T12:00:00Z"
    )
    @NotNull Instant closesAt
) {
    public CreateDemodayPollCommand toCommand(Long memberId) {
        return new CreateDemodayPollCommand(memberId, gisuId, name, opensAt, closesAt);
    }
}
