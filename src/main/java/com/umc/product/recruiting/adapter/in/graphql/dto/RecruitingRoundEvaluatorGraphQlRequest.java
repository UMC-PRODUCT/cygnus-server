package com.umc.product.recruiting.adapter.in.graphql.dto;

import com.umc.product.recruiting.application.port.in.command.dto.RecruitingRoundEvaluatorCommand;
public record RecruitingRoundEvaluatorGraphQlRequest(
    Long evaluatorMemberId
) {

    public RecruitingRoundEvaluatorCommand toCommand(Long roundId, Long requesterMemberId) {
        return RecruitingRoundEvaluatorCommand.of(roundId, requesterMemberId, evaluatorMemberId);
    }
}
