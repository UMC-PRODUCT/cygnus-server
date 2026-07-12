package com.umc.product.recruiting.adapter.in.graphql.dto;

import com.umc.product.recruiting.application.port.in.command.dto.RecruitingRoundEvaluatorCommand;
import com.umc.product.recruiting.domain.enums.RecruitingEvaluatorStage;

public record RecruitingRoundEvaluatorGraphQlRequest(
    Long evaluatorMemberId,
    RecruitingEvaluatorStage stage
) {

    public RecruitingRoundEvaluatorCommand toCommand(Long roundId, Long requesterMemberId) {
        return RecruitingRoundEvaluatorCommand.of(roundId, requesterMemberId, evaluatorMemberId, stage);
    }
}
