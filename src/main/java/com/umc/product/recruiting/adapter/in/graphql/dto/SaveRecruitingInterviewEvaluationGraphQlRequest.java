package com.umc.product.recruiting.adapter.in.graphql.dto;

import com.umc.product.recruiting.application.port.in.command.dto.SaveRecruitingInterviewEvaluationCommand;
import com.umc.product.recruiting.application.port.in.command.dto.SubmitRecruitingInterviewEvaluationCommand;

public record SaveRecruitingInterviewEvaluationGraphQlRequest(
    Integer score,
    String comment
) {

    public SaveRecruitingInterviewEvaluationCommand toSaveCommand(Long assignmentId, Long evaluatorMemberId) {
        return SaveRecruitingInterviewEvaluationCommand.builder()
            .assignmentId(assignmentId)
            .evaluatorMemberId(evaluatorMemberId)
            .score(score)
            .comment(comment)
            .build();
    }

    public SubmitRecruitingInterviewEvaluationCommand toSubmitCommand(Long assignmentId, Long evaluatorMemberId) {
        return SubmitRecruitingInterviewEvaluationCommand.builder()
            .assignmentId(assignmentId)
            .evaluatorMemberId(evaluatorMemberId)
            .score(score)
            .comment(comment)
            .build();
    }
}
