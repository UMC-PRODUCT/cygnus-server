package com.umc.product.recruiting.adapter.in.graphql.dto;

import com.umc.product.recruiting.application.port.in.command.dto.SubmitRecruitingApplicationEvaluationCommand;
import com.umc.product.recruiting.domain.enums.RecruitingApplicationEvaluationDecision;
import com.umc.product.recruiting.domain.enums.RecruitingEvaluatorStage;

public record RecruitingApplicationEvaluationGraphQlRequest(
    RecruitingEvaluatorStage stage,
    RecruitingApplicationEvaluationDecision decision,
    String comment
) {

    public SubmitRecruitingApplicationEvaluationCommand toSubmitCommand(
        Long applicationId,
        Long requesterMemberId
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
