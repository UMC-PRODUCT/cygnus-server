package com.umc.product.recruiting.application.port.in.command.dto;

import com.umc.product.recruiting.domain.enums.RecruitingApplicationEvaluationDecision;
import com.umc.product.recruiting.domain.enums.RecruitingEvaluatorStage;

public record SaveRecruitingApplicationEvaluationCommand(
    Long applicationId,
    Long requesterMemberId,
    RecruitingEvaluatorStage stage,
    RecruitingApplicationEvaluationDecision decision,
    String comment
) {

    public static SaveRecruitingApplicationEvaluationCommand of(
        Long applicationId,
        Long requesterMemberId,
        RecruitingEvaluatorStage stage,
        RecruitingApplicationEvaluationDecision decision,
        String comment
    ) {
        return new SaveRecruitingApplicationEvaluationCommand(
            applicationId,
            requesterMemberId,
            stage,
            decision,
            comment
        );
    }
}
