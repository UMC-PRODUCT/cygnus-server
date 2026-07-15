package com.umc.product.recruiting.application.port.in.command.dto;

public record RecruitingRoundEvaluatorCommand(
    Long roundId,
    Long requesterMemberId,
    Long memberId
) {

    public static RecruitingRoundEvaluatorCommand of(
        Long roundId,
        Long requesterMemberId,
        Long memberId
    ) {
        return new RecruitingRoundEvaluatorCommand(roundId, requesterMemberId, memberId);
    }
}
