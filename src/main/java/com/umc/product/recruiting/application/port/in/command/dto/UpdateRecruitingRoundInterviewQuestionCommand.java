package com.umc.product.recruiting.application.port.in.command.dto;

public record UpdateRecruitingRoundInterviewQuestionCommand(
    Long questionId,
    Long roundId,
    Long requesterMemberId,
    String content,
    Integer orderNo
) {

    public static UpdateRecruitingRoundInterviewQuestionCommand of(
        Long questionId,
        Long roundId,
        Long requesterMemberId,
        String content,
        Integer orderNo
    ) {
        return new UpdateRecruitingRoundInterviewQuestionCommand(
            questionId,
            roundId,
            requesterMemberId,
            content,
            orderNo
        );
    }
}
