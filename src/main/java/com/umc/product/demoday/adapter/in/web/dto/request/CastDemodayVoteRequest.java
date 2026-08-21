package com.umc.product.demoday.adapter.in.web.dto.request;

import com.umc.product.demoday.application.port.in.command.dto.CastDemodayVoteCommand;
import com.umc.product.demoday.application.port.in.query.participant.DemodayParticipant;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record CastDemodayVoteRequest(
    @Schema(
        description = "투표 권한 발급 API가 반환한 opaque token. 투표 대상 부스는 token에 이미 결합되어 있습니다.",
        example = "signed-vote-authorization-token"
    )
    @NotBlank String voteAuthorizationToken
) {

    public CastDemodayVoteCommand toCommand(Long pollId, DemodayParticipant participant) {
        return new CastDemodayVoteCommand(pollId, voteAuthorizationToken, participant);
    }
}
