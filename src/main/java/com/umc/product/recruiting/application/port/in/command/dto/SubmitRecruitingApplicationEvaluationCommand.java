package com.umc.product.recruiting.application.port.in.command.dto;

import com.umc.product.recruiting.domain.enums.RecruitingApplicationEvaluationDecision;
import com.umc.product.recruiting.domain.enums.RecruitingEvaluatorStage;

public record SubmitRecruitingApplicationEvaluationCommand(
    Long applicationId,
    Long requesterMemberId,
    RecruitingEvaluatorStage stage,
    RecruitingApplicationEvaluationDecision decision,
    String comment
) {

    public static SubmitRecruitingApplicationEvaluationCommand of(
        Long applicationId,
        Long requesterMemberId,
        RecruitingEvaluatorStage stage,
        RecruitingApplicationEvaluationDecision decision,
        String comment
    ) {
        return new SubmitRecruitingApplicationEvaluationCommand(
            applicationId,
            requesterMemberId,
            stage,
            decision,
            comment
        );
    }
}
