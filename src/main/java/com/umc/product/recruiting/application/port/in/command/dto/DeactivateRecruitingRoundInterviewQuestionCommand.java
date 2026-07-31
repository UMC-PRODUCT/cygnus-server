package com.umc.product.recruiting.application.port.in.command.dto;

public record DeactivateRecruitingRoundInterviewQuestionCommand(
    Long questionId,
    Long roundId,
    Long requesterMemberId
) {

    public static DeactivateRecruitingRoundInterviewQuestionCommand of(
        Long questionId,
        Long roundId,
        Long requesterMemberId
    ) {
        return new DeactivateRecruitingRoundInterviewQuestionCommand(questionId, roundId, requesterMemberId);
    }
}
