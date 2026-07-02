package com.umc.product.recruiting.adapter.in.web.dto.request;

import com.umc.product.recruiting.application.port.in.command.dto.SaveRecruitingInterviewEvaluationCommand;
import com.umc.product.recruiting.application.port.in.command.dto.SubmitRecruitingInterviewEvaluationCommand;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "면접 평가 저장 또는 제출 요청")
public record SaveRecruitingInterviewEvaluationRequest(
    @Schema(description = "지원자 평가 점수", example = "5")
    @NotNull Integer score,
    @Schema(description = "면접관 평가 코멘트", example = "문제 해결 과정이 명확했습니다.")
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
