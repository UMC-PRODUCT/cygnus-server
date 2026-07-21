package com.umc.product.recruiting.adapter.in.graphql.dto;

import com.umc.product.recruiting.application.port.in.command.dto.UpdateRecruitingRoundStatusCommand;
import com.umc.product.recruiting.domain.enums.RecruitingRoundStatus;

public record UpdateRecruitingRoundStatusGraphQlRequest(
    RecruitingRoundStatus status
) {

    public UpdateRecruitingRoundStatusCommand toCommand(Long seasonId, Long roundId, Long requesterMemberId) {
        return UpdateRecruitingRoundStatusCommand.builder()
            .seasonId(seasonId)
            .roundId(roundId)
            .status(status)
            .requesterMemberId(requesterMemberId)
            .build();
    }
}
