package com.umc.product.recruiting.application.port.in.command.dto;

public record CreateRecruitingRoundInterviewQuestionCommand(
    Long roundId,
    Long requesterMemberId,
    String content,
    Integer orderNo
) {

    public static CreateRecruitingRoundInterviewQuestionCommand of(
        Long roundId,
        Long requesterMemberId,
        String content,
        Integer orderNo
    ) {
        return new CreateRecruitingRoundInterviewQuestionCommand(roundId, requesterMemberId, content, orderNo);
    }
}
