package com.umc.product.recruiting.adapter.in.web.dto.request;

import com.umc.product.recruiting.application.port.in.command.dto.SaveRecruitingApplicationEvaluationCommand;
import com.umc.product.recruiting.application.port.in.command.dto.SubmitRecruitingApplicationEvaluationCommand;
import com.umc.product.recruiting.domain.enums.RecruitingApplicationEvaluationDecision;
import com.umc.product.recruiting.domain.enums.RecruitingEvaluatorStage;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "단계별 지원서 평가 저장 또는 제출 요청")
public record SaveRecruitingEvaluationRequest(
    @Schema(description = "평가 결정", example = "APPROVED")
    @NotNull RecruitingApplicationEvaluationDecision decision,
    @Schema(description = "평가 의견", example = "지원 동기와 경험이 평가 기준을 충족합니다.", maxLength = 2000)
    @Size(max = 2000) String comment
) {

    public SaveRecruitingApplicationEvaluationCommand toSaveCommand(
        Long applicationId,
        Long requesterMemberId,
        RecruitingEvaluatorStage stage
    ) {
        return SaveRecruitingApplicationEvaluationCommand.of(
            applicationId,
            requesterMemberId,
            stage,
            decision,
            comment
        );
    }

    public SubmitRecruitingApplicationEvaluationCommand toSubmitCommand(
        Long applicationId,
        Long requesterMemberId,
        RecruitingEvaluatorStage stage
    ) {
        return SubmitRecruitingApplicationEvaluationCommand.of(
            applicationId,
            requesterMemberId,
            stage,
            decision,
            comment
        );
    }
}
