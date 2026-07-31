package com.umc.product.recruiting.application.port.in.command.dto;

public record DeactivateRecruitingApplicationInterviewQuestionCommand(
    Long questionId,
    Long applicationId,
    Long requesterMemberId
) {

    public static DeactivateRecruitingApplicationInterviewQuestionCommand of(
        Long questionId,
        Long applicationId,
        Long requesterMemberId
    ) {
        return new DeactivateRecruitingApplicationInterviewQuestionCommand(
            questionId,
            applicationId,
            requesterMemberId
        );
    }
}
