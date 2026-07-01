package com.umc.product.recruiting.adapter.in.web.dto.request;

import com.umc.product.recruiting.application.port.in.command.dto.SaveRecruitingInterviewEvaluationCommand;
import com.umc.product.recruiting.application.port.in.command.dto.SubmitRecruitingInterviewEvaluationCommand;

import jakarta.validation.constraints.NotNull;

public record SaveRecruitingInterviewEvaluationRequest(
    @NotNull Integer score,
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
