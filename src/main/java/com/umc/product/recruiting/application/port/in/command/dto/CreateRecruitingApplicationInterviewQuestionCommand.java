package com.umc.product.recruiting.application.port.in.command.dto;

public record CreateRecruitingApplicationInterviewQuestionCommand(
    Long applicationId,
    Long requesterMemberId,
    String content,
    Integer orderNo
) {

    public static CreateRecruitingApplicationInterviewQuestionCommand of(
        Long applicationId,
        Long requesterMemberId,
        String content,
        Integer orderNo
    ) {
        return new CreateRecruitingApplicationInterviewQuestionCommand(
            applicationId,
            requesterMemberId,
            content,
            orderNo
        );
    }
}
