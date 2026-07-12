package com.umc.product.recruiting.application.port.in.command.dto;

import com.umc.product.recruiting.domain.enums.RecruitingEvaluatorStage;

public record RecruitingRoundEvaluatorCommand(
    Long roundId,
    Long requesterMemberId,
    Long memberId,
    RecruitingEvaluatorStage stage
) {

    public static RecruitingRoundEvaluatorCommand of(
        Long roundId,
        Long requesterMemberId,
        Long memberId,
        RecruitingEvaluatorStage stage
    ) {
        return new RecruitingRoundEvaluatorCommand(roundId, requesterMemberId, memberId, stage);
    }
}
