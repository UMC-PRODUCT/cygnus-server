package com.umc.product.demoday.adapter.in.web.dto.request;

import com.umc.product.demoday.application.port.in.command.dto.ChangeDemodayVoteStatusCommand;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangeDemodayVoteStatusRequest(
    @Schema(
        description = "표를 무효화하거나 무효 해제하는 사유는 감사 로그에 기록됩니다.",
        example = "동일 인물의 중복 투표로 확인됨",
        minLength = 1,
        maxLength = 255
    )
    @NotBlank @Size(max = 255) String reason
) {

    public ChangeDemodayVoteStatusCommand toCommand(Long pollId, Long voteId, Long requesterMemberId) {
        return new ChangeDemodayVoteStatusCommand(pollId, voteId, requesterMemberId, reason);
    }
}
