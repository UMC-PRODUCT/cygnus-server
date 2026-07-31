package com.umc.product.recruiting.application.port.in.command.dto;

public record UpdateRecruitingApplicationInterviewQuestionCommand(
    Long questionId,
    Long applicationId,
    Long requesterMemberId,
    String content,
    Integer orderNo
) {

    public static UpdateRecruitingApplicationInterviewQuestionCommand of(
        Long questionId,
        Long applicationId,
        Long requesterMemberId,
        String content,
        Integer orderNo
    ) {
        return new UpdateRecruitingApplicationInterviewQuestionCommand(
            questionId,
            applicationId,
            requesterMemberId,
            content,
            orderNo
        );
    }
}
