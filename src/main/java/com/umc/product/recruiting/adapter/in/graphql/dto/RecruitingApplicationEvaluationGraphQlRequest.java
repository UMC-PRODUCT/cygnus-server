package com.umc.product.recruiting.adapter.in.graphql.dto;

import com.umc.product.global.graphql.relay.GlobalId;
import com.umc.product.global.graphql.relay.GlobalIdTypes;
import com.umc.product.recruiting.application.port.in.command.dto.SubmitRecruitingApplicationEvaluationCommand;
import com.umc.product.recruiting.domain.enums.RecruitingApplicationEvaluationDecision;
import com.umc.product.recruiting.domain.enums.RecruitingEvaluatorStage;

public record RecruitingApplicationEvaluationGraphQlRequest(
    String applicationId,
    RecruitingEvaluatorStage stage,
    RecruitingApplicationEvaluationDecision decision,
    String comment
) {

    public SubmitRecruitingApplicationEvaluationCommand toSubmitCommand(Long requesterMemberId) {
        return SubmitRecruitingApplicationEvaluationCommand.of(
            GlobalId.decodeLong(applicationId, GlobalIdTypes.RECRUITING_APPLICATION),
            requesterMemberId,
            stage,
            decision,
            comment
        );
    }
}
